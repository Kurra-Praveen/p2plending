package com.llms.service;

import com.llms.dto.request.CreateLoanRequest;
import com.llms.dto.request.DisburseLoanRequest;
import com.llms.dto.response.LoanResponse;
import com.llms.dto.response.RepaymentScheduleResponse;
import com.llms.entity.*;
import com.llms.enums.EmiStatus;
import com.llms.enums.LoanStatus;
import com.llms.exception.InvalidStateException;
import com.llms.exception.ResourceNotFoundException;
import com.llms.repository.*;
import com.llms.security.SecurityUtils;
import com.llms.util.EmiCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanService {

    private final LoanRepository loanRepository;
    private final BorrowerRepository borrowerRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final DisbursementRepository disbursementRepository;
    private final LoanStatusHistoryRepository statusHistoryRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    @Transactional
    public LoanResponse createLoan(CreateLoanRequest request) {
        Borrower borrower = borrowerRepository.findByIdAndNotDeleted(request.getBorrowerId())
                .orElseThrow(() -> new ResourceNotFoundException("Borrower", "id", request.getBorrowerId()));

        User currentUser = securityUtils.getCurrentUser();

        // Calculate EMI and schedule
        // First EMI due date is one month from today (will be adjusted on disbursement)
        LocalDate tentativeFirstEmiDate = LocalDate.now().plusMonths(1);

        EmiCalculator.LoanCalculationResult calculation = EmiCalculator.calculate(
                request.getPrincipal(),
                request.getInterestRate(),
                request.getTenureMonths(),
                request.getInterestType(),
                tentativeFirstEmiDate
        );

        // Create loan entity
        Loan loan = Loan.builder()
                .borrower(borrower)
                .principalAmount(request.getPrincipal())
                .interestRate(request.getInterestRate())
                .interestType(request.getInterestType())
                .tenureMonths(request.getTenureMonths())
                .emiAmount(calculation.getEmiAmount())
                .totalInterest(calculation.getTotalInterest())
                .totalPayable(calculation.getTotalPayable())
                .outstandingPrincipal(request.getPrincipal())
                .outstandingInterest(calculation.getTotalInterest())
                .outstandingPenalty(0L)
                .status(LoanStatus.CREATED)
                .createdBy(currentUser)
                .build();

        loan = loanRepository.save(loan);

        // Create repayment schedule
        for (EmiCalculator.EmiScheduleItem item : calculation.getSchedule()) {
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .loan(loan)
                    .emiNo(item.getEmiNo())
                    .dueDate(item.getDueDate())
                    .principalDue(item.getPrincipalDue())
                    .interestDue(item.getInterestDue())
                    .totalDue(item.getTotalDue())
                    .status(EmiStatus.PENDING)
                    .build();
            scheduleRepository.save(schedule);
        }

        // Record status history
        recordStatusChange(loan, null, LoanStatus.CREATED, "Loan created", currentUser);

        log.info("Loan created: {} for borrower {} by user {}",
                loan.getId(), borrower.getId(), currentUser.getEmail());

        auditService.logCreate("Loan", loan.getId(), loan);

        return LoanResponse.from(loan);
    }

    @Transactional
    public LoanResponse disburseLoan(UUID loanId, DisburseLoanRequest request) {
        Loan loan = findLoanOrThrow(loanId);
        User currentUser = securityUtils.getCurrentUser();

        // Validate state
        if (loan.getStatus() != LoanStatus.CREATED) {
            throw new InvalidStateException("INVALID_STATE",
                    "Loan can only be disbursed from CREATED state. Current state: " + loan.getStatus());
        }

        // Validate amount
        if (!request.getAmount().equals(loan.getPrincipalAmount())) {
            throw new InvalidStateException("AMOUNT_MISMATCH",
                    "Disbursement amount must equal principal amount");
        }

        // Create disbursement record
        Disbursement disbursement = Disbursement.builder()
                .loan(loan)
                .amount(request.getAmount())
                .mode(request.getMode())
                .reference(request.getReference())
                .disbursedAt(LocalDateTime.now())
                .createdBy(currentUser)
                .build();
        disbursementRepository.save(disbursement);

        // Update loan status
        LoanStatus previousStatus = loan.getStatus();
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setDisbursedAt(LocalDateTime.now());

        // Recalculate schedule dates based on disbursement date
        LocalDate firstEmiDate = LocalDate.now().plusMonths(1);
        List<RepaymentSchedule> schedules = scheduleRepository.findByLoanIdOrderByEmiNoAsc(loanId);
        for (RepaymentSchedule schedule : schedules) {
            schedule.setDueDate(firstEmiDate.plusMonths(schedule.getEmiNo() - 1));
            scheduleRepository.save(schedule);
        }

        loan = loanRepository.save(loan);

        // Record status history
        recordStatusChange(loan, previousStatus, LoanStatus.ACTIVE, "Loan disbursed", currentUser);

        log.info("Loan disbursed: {} amount {} by user {}",
                loan.getId(), request.getAmount(), currentUser.getEmail());

        auditService.logStatusChange("Loan", loan.getId(), previousStatus, loan.getStatus());

        return LoanResponse.from(loan);
    }

    @Transactional(readOnly = true)
    public LoanResponse getLoan(UUID id) {
        Loan loan = findLoanOrThrow(id);
        return LoanResponse.from(loan);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> getAllLoans(Pageable pageable) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return loanRepository.findAllByCreatedBy(currentUserId, pageable)
                .map(LoanResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> getLoansByStatus(LoanStatus status, Pageable pageable) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return loanRepository.findByStatusAndCreatedBy(status, currentUserId, pageable)
                .map(LoanResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<LoanResponse> getLoansByBorrower(UUID borrowerId, Pageable pageable) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return loanRepository.findByBorrowerIdAndCreatedBy(borrowerId, currentUserId, pageable)
                .map(LoanResponse::from);
    }

    @Transactional(readOnly = true)
    public List<RepaymentScheduleResponse> getRepaymentSchedule(UUID loanId) {
        findLoanOrThrow(loanId);
        return scheduleRepository.findByLoanIdOrderByEmiNoAsc(loanId).stream()
                .map(RepaymentScheduleResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void closeLoan(UUID loanId) {
        Loan loan = findLoanOrThrow(loanId);
        User currentUser = securityUtils.getCurrentUser();

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new InvalidStateException("INVALID_STATE",
                    "Only ACTIVE loans can be closed");
        }

        if (loan.getTotalOutstanding() > 0) {
            throw new InvalidStateException("OUTSTANDING_BALANCE",
                    "Cannot close loan with outstanding balance: " + loan.getTotalOutstanding());
        }

        LoanStatus previousStatus = loan.getStatus();
        loan.setStatus(LoanStatus.CLOSED);
        loan.setClosedAt(LocalDateTime.now());
        loanRepository.save(loan);

        recordStatusChange(loan, previousStatus, LoanStatus.CLOSED, "Loan closed - fully paid", currentUser);

        log.info("Loan closed: {}", loanId);
        auditService.logStatusChange("Loan", loan.getId(), previousStatus, loan.getStatus());
    }

    @Transactional
    public void markAsDefaulted(UUID loanId, String reason) {
        Loan loan = findLoanOrThrow(loanId);
        User currentUser = securityUtils.getCurrentUser();

        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new InvalidStateException("INVALID_STATE",
                    "Only ACTIVE loans can be marked as defaulted");
        }

        LoanStatus previousStatus = loan.getStatus();
        loan.setStatus(LoanStatus.DEFAULTED);
        loanRepository.save(loan);

        recordStatusChange(loan, previousStatus, LoanStatus.DEFAULTED, reason, currentUser);

        log.info("Loan marked as defaulted: {} reason: {}", loanId, reason);
        auditService.logStatusChange("Loan", loan.getId(), previousStatus, loan.getStatus());
    }

    Loan findLoanOrThrow(UUID id) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return loanRepository.findByIdAndCreatedBy(id, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "id", id));
    }

    private void recordStatusChange(Loan loan, LoanStatus fromStatus, LoanStatus toStatus,
                                    String reason, User changedBy) {
        LoanStatusHistory history = LoanStatusHistory.builder()
                .loan(loan)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(reason)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .build();
        statusHistoryRepository.save(history);
    }
}
