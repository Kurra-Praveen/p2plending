package com.llms.service;

import com.llms.entity.Loan;
import com.llms.entity.Penalty;
import com.llms.entity.RepaymentSchedule;
import com.llms.enums.EmiStatus;
import com.llms.enums.LoanStatus;
import com.llms.enums.PenaltyStatus;
import com.llms.repository.LoanRepository;
import com.llms.repository.PenaltyRepository;
import com.llms.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Delinquency Service handles:
 * 1. Daily overdue detection
 * 2. Penalty calculation and accrual
 * 3. Default classification based on DPD (Days Past Due)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DelinquencyService {

    private final RepaymentScheduleRepository scheduleRepository;
    private final PenaltyRepository penaltyRepository;
    private final LoanRepository loanRepository;
    private final LoanService loanService;

    @Value("${penalty.rate-per-day:0.05}")
    private BigDecimal penaltyRatePerDay;

    @Value("${penalty.grace-period-days:0}")
    private int gracePeriodDays;

    @Value("${penalty.max-dpd-for-default:90}")
    private int maxDpdForDefault;

    /**
     * Process overdue loans - called by scheduled job
     */
    @Transactional
    public void processOverdueLoans() {
        LocalDate today = LocalDate.now();
        log.info("Starting daily overdue processing for date: {}", today);

        // Find all overdue schedules
        List<RepaymentSchedule> overdueSchedules = scheduleRepository.findOverdueSchedules(today);

        int processedCount = 0;
        int penaltiesApplied = 0;
        int defaultsMarked = 0;

        for (RepaymentSchedule schedule : overdueSchedules) {
            try {
                // Mark as overdue if still pending
                if (schedule.getStatus() == EmiStatus.PENDING) {
                    schedule.setStatus(EmiStatus.OVERDUE);
                    scheduleRepository.save(schedule);
                }

                // Calculate days past due
                long daysOverdue = ChronoUnit.DAYS.between(schedule.getDueDate(), today);

                // Apply penalty if beyond grace period
                if (daysOverdue > gracePeriodDays) {
                    boolean penaltyApplied = applyPenalty(schedule, (int) daysOverdue);
                    if (penaltyApplied) penaltiesApplied++;
                }

                // Check for default classification
                if (daysOverdue >= maxDpdForDefault) {
                    Loan loan = schedule.getLoan();
                    if (loan.getStatus() == LoanStatus.ACTIVE) {
                        loanService.markAsDefaulted(loan.getId(),
                                "Auto-defaulted: " + daysOverdue + " days past due");
                        defaultsMarked++;
                    }
                }

                processedCount++;
            } catch (Exception e) {
                log.error("Error processing overdue schedule {}: {}", schedule.getId(), e.getMessage());
            }
        }

        log.info("Daily overdue processing completed. Processed: {}, Penalties: {}, Defaults: {}",
                processedCount, penaltiesApplied, defaultsMarked);
    }

    /**
     * Apply penalty to overdue EMI
     * Penalty = Outstanding EMI Amount * Penalty Rate * Days Overdue
     */
    private boolean applyPenalty(RepaymentSchedule schedule, int daysOverdue) {
        // Check if penalty already exists for today's processing
        List<Penalty> existingPenalties = penaltyRepository.findUnpaidByScheduleId(schedule.getId());

        // Calculate expected penalty
        long outstandingAmount = schedule.getTotalRemaining();
        if (outstandingAmount <= 0) {
            return false;
        }

        BigDecimal penaltyAmount = BigDecimal.valueOf(outstandingAmount)
                .multiply(penaltyRatePerDay)
                .multiply(BigDecimal.valueOf(daysOverdue))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        long penaltyInPaise = penaltyAmount.longValue();

        if (penaltyInPaise <= 0) {
            return false;
        }

        // Check if we need to update existing penalty or create new
        if (!existingPenalties.isEmpty()) {
            // Update existing penalty with new amount based on current DPD
            Penalty existing = existingPenalties.get(0);
            if (existing.getDaysOverdue() < daysOverdue) {
                existing.setAmount(penaltyInPaise);
                existing.setDaysOverdue(daysOverdue);
                penaltyRepository.save(existing);
                updateLoanPenalty(schedule.getLoan());
                log.debug("Updated penalty for schedule {}: {} paise ({} days)",
                        schedule.getId(), penaltyInPaise, daysOverdue);
            }
            return false; // Not a new penalty
        }

        // Create new penalty
        Penalty penalty = Penalty.builder()
                .loan(schedule.getLoan())
                .schedule(schedule)
                .emiNo(schedule.getEmiNo())
                .amount(penaltyInPaise)
                .daysOverdue(daysOverdue)
                .status(PenaltyStatus.UNPAID)
                .appliedAt(LocalDateTime.now())
                .build();

        penaltyRepository.save(penalty);
        updateLoanPenalty(schedule.getLoan());

        log.info("Applied penalty to loan {} EMI {}: {} paise ({} days overdue)",
                schedule.getLoan().getId(), schedule.getEmiNo(), penaltyInPaise, daysOverdue);

        return true;
    }

    private void updateLoanPenalty(Loan loan) {
        Long totalPenalty = penaltyRepository.sumUnpaidPenaltiesByLoanId(loan.getId());
        loan.setOutstandingPenalty(totalPenalty != null ? totalPenalty : 0L);
        loanRepository.save(loan);
    }

    /**
     * Get DPD (Days Past Due) for a loan
     */
    @Transactional(readOnly = true)
    public int getDaysOverdue(Loan loan) {
        List<RepaymentSchedule> overdueSchedules = scheduleRepository
                .findByLoanIdAndStatusIn(loan.getId(), List.of(EmiStatus.OVERDUE, EmiStatus.PARTIAL));

        if (overdueSchedules.isEmpty()) {
            return 0;
        }

        // Find oldest overdue EMI
        LocalDate oldestDueDate = overdueSchedules.stream()
                .map(RepaymentSchedule::getDueDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        return (int) ChronoUnit.DAYS.between(oldestDueDate, LocalDate.now());
    }
}
