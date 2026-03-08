package com.llms.service;

import com.llms.dto.request.RecordPaymentRequest;
import com.llms.dto.response.PaymentResponse;
import com.llms.entity.*;
import com.llms.enums.*;
import com.llms.repository.*;
import com.llms.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentAllocationRepository allocationRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private RepaymentScheduleRepository scheduleRepository;
    @Mock
    private PenaltyRepository penaltyRepository;
    @Mock
    private LoanChargeRepository loanChargeRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private LoanService loanService;
    @Mock
    private ChargeService chargeService;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private PaymentService paymentService;

    private Loan testLoan;
    private User testUser;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .build();

        testLoan = Loan.builder()
                .id(UUID.randomUUID())
                .status(LoanStatus.ACTIVE)
                .principalAmount(100000L)
                .outstandingPrincipal(100000L)
                .build();

        testPayment = Payment.builder()
                .id(UUID.randomUUID())
                .loan(testLoan)
                .amountPaid(1000L)
                .allocations(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Should allocate to charges first")
    void shouldAllocateToChargesFirst() {
        // Given
        RecordPaymentRequest request = RecordPaymentRequest.builder()
                .amount(2000L)
                .paymentDate(LocalDate.now())
                .mode(PaymentMode.CASH)
                .build();

        LoanCharge charge = LoanCharge.builder()
                .id(UUID.randomUUID())
                .amount(1000L)
                .amountPaid(0L)
                .amountWaived(0L)
                .status(ChargeStatus.PENDING)
                .build();

        when(loanService.findLoanOrThrow(testLoan.getId())).thenReturn(testLoan);
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        // Removed unnecessary stubbing for existsByReference
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);
        when(loanChargeRepository.findOutstandingChargesByLoanId(testLoan.getId())).thenReturn(List.of(charge));
        when(allocationRepository.save(any(PaymentAllocation.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        PaymentResponse response = paymentService.recordPayment(testLoan.getId(), request);

        // Then
        // Should allocate 1000 to charge
        verify(loanChargeRepository).save(charge);
        verify(allocationRepository, atLeastOnce()).save(argThat(allocation ->
            allocation.getType() == AllocationType.CHARGE && allocation.getAmount() == 1000L
        ));
    }

    @Test
    @DisplayName("Should allocate to penalties second")
    void shouldAllocateToPenaltiesSecond() {
        // Given
        RecordPaymentRequest request = RecordPaymentRequest.builder()
                .amount(2000L) // 1000 charge + 1000 penalty
                .paymentDate(LocalDate.now())
                .mode(PaymentMode.CASH)
                .build();

        // 1. Charge (1000)
        LoanCharge charge = LoanCharge.builder()
                .id(UUID.randomUUID())
                .amount(1000L)
                .amountPaid(0L)
                .amountWaived(0L)
                .status(ChargeStatus.PENDING)
                .build();

        // 2. Penalty (1000)
        RepaymentSchedule schedule = RepaymentSchedule.builder().id(UUID.randomUUID()).build();
        Penalty penalty = Penalty.builder()
                .id(UUID.randomUUID())
                .amount(1000L)
                .status(PenaltyStatus.UNPAID)
                .schedule(schedule)
                .build();

        when(loanService.findLoanOrThrow(testLoan.getId())).thenReturn(testLoan);
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        when(loanChargeRepository.findOutstandingChargesByLoanId(testLoan.getId())).thenReturn(List.of(charge));
        when(penaltyRepository.findUnpaidByLoanId(testLoan.getId())).thenReturn(List.of(penalty));

        when(allocationRepository.save(any(PaymentAllocation.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        paymentService.recordPayment(testLoan.getId(), request);

        // Then
        verify(loanChargeRepository).save(charge);
        verify(penaltyRepository).save(penalty);

        // Check order implied by logic: verify calls happened
        verify(allocationRepository, times(2)).save(any(PaymentAllocation.class));
    }

    @Test
    @DisplayName("Should allocate to interest third and principal fourth")
    void shouldAllocateToInterestThenPrincipal() {
        // Given
        RecordPaymentRequest request = RecordPaymentRequest.builder()
                .amount(3000L) // 1000 interest + 2000 principal
                .paymentDate(LocalDate.now())
                .mode(PaymentMode.CASH)
                .build();

        RepaymentSchedule schedule = RepaymentSchedule.builder()
                .id(UUID.randomUUID())
                .interestDue(1000L)
                .interestPaid(0L)
                .principalDue(5000L)
                .principalPaid(0L)
                .status(EmiStatus.PENDING)
                .build();

        when(loanService.findLoanOrThrow(testLoan.getId())).thenReturn(testLoan);
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        // No charges or penalties
        when(loanChargeRepository.findOutstandingChargesByLoanId(testLoan.getId())).thenReturn(List.of());
        when(penaltyRepository.findUnpaidByLoanId(testLoan.getId())).thenReturn(List.of());

        when(scheduleRepository.findByLoanIdAndStatusIn(any(), any())).thenReturn(List.of(schedule));

        when(allocationRepository.save(any(PaymentAllocation.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        paymentService.recordPayment(testLoan.getId(), request);

        // Then
        // Interest (1000) -> Principal (2000)
        verify(allocationRepository).save(argThat(a -> a.getType() == AllocationType.INTEREST && a.getAmount() == 1000L));
        verify(allocationRepository).save(argThat(a -> a.getType() == AllocationType.PRINCIPAL && a.getAmount() == 2000L));

        verify(scheduleRepository).save(schedule);
        assertThat(schedule.getInterestPaid()).isEqualTo(1000L);
        assertThat(schedule.getPrincipalPaid()).isEqualTo(2000L);
    }

    @Test
    @DisplayName("Should handle overpayment as principal reduction")
    void shouldHandleOverpayment() {
        // Given
        RecordPaymentRequest request = RecordPaymentRequest.builder()
                .amount(1000L)
                .paymentDate(LocalDate.now())
                .mode(PaymentMode.CASH)
                .build();

        // Mock schedules to support the remaining principal after calculation
        RepaymentSchedule futureSchedule = RepaymentSchedule.builder()
                .id(UUID.randomUUID())
                .principalDue(99000L)
                .principalPaid(0L)
                .interestDue(0L)
                .interestPaid(0L)
                .status(EmiStatus.PENDING)
                .build();

        when(loanService.findLoanOrThrow(testLoan.getId())).thenReturn(testLoan);
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);
        when(loanChargeRepository.findOutstandingChargesByLoanId(testLoan.getId())).thenReturn(List.of());
        when(penaltyRepository.findUnpaidByLoanId(testLoan.getId())).thenReturn(List.of());

        // Return empty for pending schedules to force "Overpayment" logic in allocatePayment
        when(scheduleRepository.findByLoanIdAndStatusIn(any(), any())).thenReturn(List.of());

        // Return futureSchedule for updateLoanOutstandings to maintain the principal balance
        when(scheduleRepository.findByLoanIdOrderByEmiNoAsc(testLoan.getId())).thenReturn(List.of(futureSchedule));

        when(allocationRepository.save(any(PaymentAllocation.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        paymentService.recordPayment(testLoan.getId(), request);

        // Then
        verify(allocationRepository).save(argThat(a -> a.getType() == AllocationType.PRINCIPAL && a.getAmount() == 1000L));
        // Verify loan principal is consistent with schedules
        assertThat(testLoan.getOutstandingPrincipal()).isEqualTo(99000L);
    }
}
