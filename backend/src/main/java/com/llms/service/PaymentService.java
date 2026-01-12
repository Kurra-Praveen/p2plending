package com.llms.service;

import com.llms.dto.request.RecordPaymentRequest;
import com.llms.dto.response.PaymentResponse;
import com.llms.entity.*;
import com.llms.enums.*;
import com.llms.exception.InvalidStateException;
import com.llms.repository.*;
import com.llms.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payment Service implementing the allocation logic:
 * 1. Penalty (oldest first)
 * 2. Interest (oldest EMI first)
 * 3. Principal (oldest EMI first)
 *
 * This ensures proper financial handling and supports partial payments.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final PenaltyRepository penaltyRepository;
    private final SecurityUtils securityUtils;
    private final LoanService loanService;
    private final AuditService auditService;

    @Transactional
    public PaymentResponse recordPayment(UUID loanId, RecordPaymentRequest request) {
        Loan loan = loanService.findLoanOrThrow(loanId);
        User currentUser = securityUtils.getCurrentUser();

        // Validate loan state
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new InvalidStateException("INVALID_STATE",
                    "Payments can only be recorded for ACTIVE loans. Current state: " + loan.getStatus());
        }

        // Check for duplicate reference
        if (request.getReference() != null && paymentRepository.existsByReference(request.getReference())) {
            throw new InvalidStateException("DUPLICATE_REFERENCE",
                    "Payment with reference " + request.getReference() + " already exists");
        }

        // Create payment record
        Payment payment = Payment.builder()
                .loan(loan)
                .amountPaid(request.getAmount())
                .paymentDate(request.getPaymentDate())
                .mode(request.getMode())
                .reference(request.getReference())
                .createdBy(currentUser)
                .build();
        payment = paymentRepository.save(payment);

        // Allocate payment
        List<PaymentAllocation> allocations = allocatePayment(payment, loan, request.getAmount());
        payment.getAllocations().addAll(allocations);

        // Update loan outstanding balances
        updateLoanOutstandings(loan);

        // Check if loan can be closed
        if (loan.getTotalOutstanding() == 0) {
            loanService.closeLoan(loanId);
        }

        log.info("Payment recorded: {} for loan {} amount {} by user {}",
                payment.getId(), loanId, request.getAmount(), currentUser.getEmail());

        auditService.logCreate("Payment", payment.getId(), payment);

        return PaymentResponse.from(payment);
    }

    /**
     * Allocates payment amount in order:
     * 1. Penalties (oldest first)
     * 2. Interest (oldest EMI first)
     * 3. Principal (oldest EMI first)
     */
    private List<PaymentAllocation> allocatePayment(Payment payment, Loan loan, long amount) {
        List<PaymentAllocation> allocations = new ArrayList<>();
        long remaining = amount;

        // Step 1: Allocate to penalties
        remaining = allocateToPenalties(payment, loan.getId(), remaining, allocations);

        if (remaining <= 0) {
            return allocations;
        }

        // Step 2 & 3: Allocate to EMI schedules (interest first, then principal)
        List<RepaymentSchedule> pendingSchedules = scheduleRepository
                .findByLoanIdAndStatusIn(loan.getId(), List.of(EmiStatus.PENDING, EmiStatus.PARTIAL, EmiStatus.OVERDUE));

        for (RepaymentSchedule schedule : pendingSchedules) {
            if (remaining <= 0) break;

            // First allocate to interest
            long interestRemaining = schedule.getInterestRemaining();
            if (interestRemaining > 0 && remaining > 0) {
                long interestAllocation = Math.min(remaining, interestRemaining);
                allocations.add(createAllocation(payment, schedule, AllocationType.INTEREST, interestAllocation));
                schedule.setInterestPaid(schedule.getInterestPaid() + interestAllocation);
                remaining -= interestAllocation;
            }

            // Then allocate to principal
            long principalRemaining = schedule.getPrincipalRemaining();
            if (principalRemaining > 0 && remaining > 0) {
                long principalAllocation = Math.min(remaining, principalRemaining);
                allocations.add(createAllocation(payment, schedule, AllocationType.PRINCIPAL, principalAllocation));
                schedule.setPrincipalPaid(schedule.getPrincipalPaid() + principalAllocation);
                remaining -= principalAllocation;
            }

            // Update schedule status
            updateScheduleStatus(schedule);
            scheduleRepository.save(schedule);
        }

        // If there's remaining amount (overpayment), it goes to principal reduction
        if (remaining > 0) {
            log.warn("Overpayment of {} paise for loan {}. Applying to principal.", remaining, loan.getId());
            // Apply to outstanding principal directly
            loan.setOutstandingPrincipal(Math.max(0, loan.getOutstandingPrincipal() - remaining));
            allocations.add(createAllocation(payment, null, AllocationType.PRINCIPAL, remaining));
        }

        return allocations;
    }

    private long allocateToPenalties(Payment payment, UUID loanId, long amount, List<PaymentAllocation> allocations) {
        List<Penalty> unpaidPenalties = penaltyRepository.findUnpaidByLoanId(loanId);
        long remaining = amount;

        for (Penalty penalty : unpaidPenalties) {
            if (remaining <= 0) break;

            long penaltyAmount = penalty.getUnpaidAmount();
            long allocation = Math.min(remaining, penaltyAmount);

            allocations.add(createAllocation(payment, penalty.getSchedule(), AllocationType.PENALTY, allocation));

            if (allocation >= penaltyAmount) {
                penalty.setStatus(PenaltyStatus.PAID);
                penalty.setPaidAt(LocalDateTime.now());
            } else {
                penalty.setStatus(PenaltyStatus.PARTIAL);
            }

            // Update schedule's penalty paid
            RepaymentSchedule schedule = penalty.getSchedule();
            schedule.setPenaltyPaid(schedule.getPenaltyPaid() + allocation);
            scheduleRepository.save(schedule);

            penaltyRepository.save(penalty);
            remaining -= allocation;
        }

        return remaining;
    }

    private PaymentAllocation createAllocation(Payment payment, RepaymentSchedule schedule,
                                                AllocationType type, long amount) {
        PaymentAllocation allocation = PaymentAllocation.builder()
                .payment(payment)
                .schedule(schedule)
                .type(type)
                .amount(amount)
                .build();
        return allocationRepository.save(allocation);
    }

    private void updateScheduleStatus(RepaymentSchedule schedule) {
        if (schedule.isFullyPaid()) {
            schedule.setStatus(EmiStatus.PAID);
            schedule.setPaidAt(LocalDateTime.now());
        } else if (schedule.getPrincipalPaid() > 0 || schedule.getInterestPaid() > 0) {
            schedule.setStatus(EmiStatus.PARTIAL);
        }
    }

    private void updateLoanOutstandings(Loan loan) {
        // Recalculate from schedules
        List<RepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByEmiNoAsc(loan.getId());

        long outstandingPrincipal = 0;
        long outstandingInterest = 0;

        for (RepaymentSchedule schedule : schedules) {
            outstandingPrincipal += schedule.getPrincipalRemaining();
            outstandingInterest += schedule.getInterestRemaining();
        }

        // Get outstanding penalties
        Long outstandingPenalty = penaltyRepository.sumUnpaidPenaltiesByLoanId(loan.getId());

        loan.setOutstandingPrincipal(outstandingPrincipal);
        loan.setOutstandingInterest(outstandingInterest);
        loan.setOutstandingPenalty(outstandingPenalty != null ? outstandingPenalty : 0L);

        loanRepository.save(loan);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByLoan(UUID loanId, Pageable pageable) {
        loanService.findLoanOrThrow(loanId);
        return paymentRepository.findByLoanId(loanId, pageable)
                .map(PaymentResponse::from);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPaymentsByLoan(UUID loanId) {
        loanService.findLoanOrThrow(loanId);
        return paymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId).stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        if (securityUtils.isAdminOrAuditor()) {
            return paymentRepository.findAll(pageable)
                    .map(PaymentResponse::from);
        } else {
            UUID userId = securityUtils.getCurrentUserId();
            return paymentRepository.findByLoanCreatedById(userId, pageable)
                    .map(PaymentResponse::from);
        }
    }
}
