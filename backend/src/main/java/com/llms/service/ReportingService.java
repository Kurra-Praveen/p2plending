package com.llms.service;

import com.llms.dto.response.*;
import com.llms.entity.Loan;
import com.llms.entity.Payment;
import com.llms.entity.RepaymentSchedule;
import com.llms.enums.AllocationType;
import com.llms.enums.LoanStatus;
import com.llms.repository.*;
import com.llms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingService {

    private final LoanRepository loanRepository;
    private final BorrowerRepository borrowerRepository;
    private final PaymentRepository paymentRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final LoanService loanService;
    private final SecurityUtils securityUtils;

    /**
     * Get portfolio summary for the current lender
     */
    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getPortfolioSummary() {
        UUID currentUserId = securityUtils.getCurrentUserId();

        Long totalDisbursed = loanRepository.sumTotalDisbursedByCreatedBy(currentUserId);
        Long outstanding = loanRepository.sumTotalOutstandingByCreatedBy(currentUserId);
        long activeLoans = loanRepository.countByStatusAndCreatedBy(LoanStatus.ACTIVE, currentUserId);
        long closedLoans = loanRepository.countByStatusAndCreatedBy(LoanStatus.CLOSED, currentUserId);
        long defaultedLoans = loanRepository.countByStatusAndCreatedBy(LoanStatus.DEFAULTED, currentUserId);
        long totalBorrowers = borrowerRepository.countActiveByCreatedBy(currentUserId);

        // Calculate total collected (sum of all payments for this lender's loans)
        Long totalCollected = 0L;
        List<Loan> lenderLoans = loanRepository.findByStatusInAndCreatedBy(
                List.of(LoanStatus.ACTIVE, LoanStatus.CLOSED, LoanStatus.DEFAULTED), currentUserId);
        for (Loan loan : lenderLoans) {
            totalCollected += paymentRepository.sumPaymentsByLoanId(loan.getId());
        }

        return PortfolioSummaryResponse.builder()
                .totalDisbursed(totalDisbursed != null ? totalDisbursed : 0L)
                .outstanding(outstanding != null ? outstanding : 0L)
                .totalCollected(totalCollected)
                .activeLoans(activeLoans)
                .closedLoans(closedLoans)
                .defaultedLoans(defaultedLoans)
                .totalBorrowers(totalBorrowers)
                .build();
    }

    /**
     * Get loan statement with full details
     */
    @Transactional(readOnly = true)
    public LoanStatementResponse getLoanStatement(UUID loanId) {
        Loan loan = loanService.findLoanOrThrow(loanId);

        List<RepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByEmiNoAsc(loanId);
        List<Payment> payments = paymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId);

        Long totalPaid = paymentRepository.sumPaymentsByLoanId(loanId);

        return LoanStatementResponse.builder()
                .loanId(loan.getId())
                .borrowerName(loan.getBorrower().getFullName())
                .principal(loan.getPrincipalAmount())
                .totalInterest(loan.getTotalInterest())
                .totalPayable(loan.getTotalPayable())
                .totalPaid(totalPaid != null ? totalPaid : 0L)
                .totalOutstanding(loan.getTotalOutstanding())
                .status(loan.getStatus().name())
                .schedule(schedules.stream()
                        .map(RepaymentScheduleResponse::from)
                        .collect(Collectors.toList()))
                .payments(payments.stream()
                        .map(PaymentResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Get collections summary for a date range for the current lender
     */
    @Transactional(readOnly = true)
    public CollectionsSummaryResponse getCollectionsSummary(LocalDate startDate, LocalDate endDate) {
        UUID currentUserId = securityUtils.getCurrentUserId();

        Long totalCollected = paymentRepository.sumPaymentsBetweenDatesByCreatedBy(startDate, endDate, currentUserId);

        // Get breakdown by allocation type for this lender
        Long principalCollected = allocationRepository.sumAllocationsByTypeAndCreatedBy(AllocationType.PRINCIPAL, currentUserId);
        Long interestCollected = allocationRepository.sumAllocationsByTypeAndCreatedBy(AllocationType.INTEREST, currentUserId);
        Long penaltyCollected = allocationRepository.sumAllocationsByTypeAndCreatedBy(AllocationType.PENALTY, currentUserId);

        return CollectionsSummaryResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalCollected(totalCollected != null ? totalCollected : 0L)
                .principalCollected(principalCollected)
                .interestCollected(interestCollected)
                .penaltyCollected(penaltyCollected)
                .build();
    }

    /**
     * Get overdue loans summary for the current lender
     */
    @Transactional(readOnly = true)
    public List<OverdueLoanResponse> getOverdueLoans() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        List<Loan> activeLoans = loanRepository.findByStatusAndCreatedBy(LoanStatus.ACTIVE, currentUserId);

        return activeLoans.stream()
                .filter(loan -> loan.getTotalOutstanding() > 0)
                .map(loan -> {
                    List<RepaymentSchedule> overdueSchedules = scheduleRepository
                            .findByLoanIdAndStatusIn(loan.getId(),
                                    List.of(com.llms.enums.EmiStatus.OVERDUE, com.llms.enums.EmiStatus.PARTIAL));

                    if (overdueSchedules.isEmpty()) {
                        return null;
                    }

                    LocalDate oldestDueDate = overdueSchedules.stream()
                            .map(RepaymentSchedule::getDueDate)
                            .min(LocalDate::compareTo)
                            .orElse(LocalDate.now());

                    int dpd = (int) java.time.temporal.ChronoUnit.DAYS.between(oldestDueDate, LocalDate.now());

                    return OverdueLoanResponse.builder()
                            .loanId(loan.getId())
                            .borrowerName(loan.getBorrower().getFullName())
                            .borrowerPhone(loan.getBorrower().getPhone())
                            .principalOutstanding(loan.getOutstandingPrincipal())
                            .interestOutstanding(loan.getOutstandingInterest())
                            .penaltyOutstanding(loan.getOutstandingPenalty())
                            .totalOutstanding(loan.getTotalOutstanding())
                            .daysOverdue(dpd)
                            .overdueEmiCount(overdueSchedules.size())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .sorted((a, b) -> Integer.compare(b.getDaysOverdue(), a.getDaysOverdue()))
                .collect(Collectors.toList());
    }
}
