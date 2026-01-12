package com.llms.service;

import com.llms.entity.*;
import com.llms.enums.*;
import com.llms.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DelinquencyService Unit Tests")
class DelinquencyServiceTest {

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @Mock
    private PenaltyRepository penaltyRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanService loanService;

    @InjectMocks
    private DelinquencyService delinquencyService;

    private User currentUser;
    private Borrower testBorrower;
    private Loan testLoan;
    private RepaymentSchedule overdueSchedule;

    @BeforeEach
    void setUp() {
        // Set configuration values
        ReflectionTestUtils.setField(delinquencyService, "penaltyRatePerDay", BigDecimal.valueOf(0.05));
        ReflectionTestUtils.setField(delinquencyService, "gracePeriodDays", 0);
        ReflectionTestUtils.setField(delinquencyService, "maxDpdForDefault", 90);

        currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test Lender")
                .email("lender@test.com")
                .role(UserRole.LENDER)
                .build();

        testBorrower = Borrower.builder()
                .id(UUID.randomUUID())
                .fullName("Test Borrower")
                .phone("9876543210")
                .status(BorrowerStatus.ACTIVE)
                .createdBy(currentUser)
                .build();

        testLoan = Loan.builder()
                .id(UUID.randomUUID())
                .borrower(testBorrower)
                .principalAmount(10000000L)
                .interestRate(BigDecimal.valueOf(12.0))
                .interestType(InterestType.REDUCING)
                .tenureMonths(12)
                .emiAmount(888488L)
                .totalInterest(661856L)
                .totalPayable(10661856L)
                .outstandingPrincipal(10000000L)
                .outstandingInterest(661856L)
                .outstandingPenalty(0L)
                .status(LoanStatus.ACTIVE)
                .disbursedAt(LocalDateTime.now().minusMonths(1))
                .createdBy(currentUser)
                .build();

        overdueSchedule = RepaymentSchedule.builder()
                .id(UUID.randomUUID())
                .loan(testLoan)
                .emiNo(1)
                .dueDate(LocalDate.now().minusDays(10)) // 10 days overdue
                .principalDue(833333L)
                .interestDue(55155L)
                .totalDue(888488L)
                .principalPaid(0L)
                .interestPaid(0L)
                .penaltyPaid(0L)
                .status(EmiStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("processOverdueLoans")
    class ProcessOverdueLoansTests {

        @Test
        @DisplayName("Should process overdue loans and mark as OVERDUE")
        void shouldProcessOverdueLoans() {
            List<RepaymentSchedule> overdueSchedules = List.of(overdueSchedule);

            when(scheduleRepository.findOverdueSchedules(any(LocalDate.class))).thenReturn(overdueSchedules);
            when(penaltyRepository.findUnpaidByScheduleId(any())).thenReturn(new ArrayList<>());
            when(penaltyRepository.save(any(Penalty.class))).thenAnswer(i -> i.getArgument(0));
            when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

            delinquencyService.processOverdueLoans();

            assertThat(overdueSchedule.getStatus()).isEqualTo(EmiStatus.OVERDUE);
            verify(scheduleRepository).save(overdueSchedule);
        }

        @Test
        @DisplayName("Should apply penalty for overdue EMI")
        void shouldApplyPenaltyForOverdueEmi() {
            List<RepaymentSchedule> overdueSchedules = List.of(overdueSchedule);

            when(scheduleRepository.findOverdueSchedules(any(LocalDate.class))).thenReturn(overdueSchedules);
            when(penaltyRepository.findUnpaidByScheduleId(any())).thenReturn(new ArrayList<>());
            when(penaltyRepository.save(any(Penalty.class))).thenAnswer(i -> i.getArgument(0));
            when(penaltyRepository.sumUnpaidPenaltiesByLoanId(any())).thenReturn(4424L);
            when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

            delinquencyService.processOverdueLoans();

            verify(penaltyRepository).save(any(Penalty.class));
            verify(loanRepository).save(testLoan);
        }

        @Test
        @DisplayName("Should mark loan as defaulted when DPD exceeds threshold")
        void shouldMarkLoanAsDefaultedWhenDpdExceedsThreshold() {
            // Set to 100 days overdue (exceeds 90 DPD threshold)
            overdueSchedule.setDueDate(LocalDate.now().minusDays(100));
            List<RepaymentSchedule> overdueSchedules = List.of(overdueSchedule);

            when(scheduleRepository.findOverdueSchedules(any(LocalDate.class))).thenReturn(overdueSchedules);
            when(penaltyRepository.findUnpaidByScheduleId(any())).thenReturn(new ArrayList<>());
            when(penaltyRepository.save(any(Penalty.class))).thenAnswer(i -> i.getArgument(0));
            when(penaltyRepository.sumUnpaidPenaltiesByLoanId(any())).thenReturn(0L);
            when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

            delinquencyService.processOverdueLoans();

            verify(loanService).markAsDefaulted(eq(testLoan.getId()), anyString());
        }

        @Test
        @DisplayName("Should not mark already defaulted loan")
        void shouldNotMarkAlreadyDefaultedLoan() {
            testLoan.setStatus(LoanStatus.DEFAULTED);
            overdueSchedule.setDueDate(LocalDate.now().minusDays(100));
            List<RepaymentSchedule> overdueSchedules = List.of(overdueSchedule);

            when(scheduleRepository.findOverdueSchedules(any(LocalDate.class))).thenReturn(overdueSchedules);
            when(penaltyRepository.findUnpaidByScheduleId(any())).thenReturn(new ArrayList<>());
            when(penaltyRepository.save(any(Penalty.class))).thenAnswer(i -> i.getArgument(0));
            when(penaltyRepository.sumUnpaidPenaltiesByLoanId(any())).thenReturn(0L);
            when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

            delinquencyService.processOverdueLoans();

            verify(loanService, never()).markAsDefaulted(any(), any());
        }

        @Test
        @DisplayName("Should not apply penalty when within grace period")
        void shouldNotApplyPenaltyWithinGracePeriod() {
            // Set grace period to 15 days
            ReflectionTestUtils.setField(delinquencyService, "gracePeriodDays", 15);

            overdueSchedule.setDueDate(LocalDate.now().minusDays(10)); // Within grace
            List<RepaymentSchedule> overdueSchedules = List.of(overdueSchedule);

            when(scheduleRepository.findOverdueSchedules(any(LocalDate.class))).thenReturn(overdueSchedules);

            delinquencyService.processOverdueLoans();

            verify(penaltyRepository, never()).save(any(Penalty.class));
        }

        @Test
        @DisplayName("Should update existing penalty instead of creating new")
        void shouldUpdateExistingPenalty() {
            Penalty existingPenalty = Penalty.builder()
                    .id(UUID.randomUUID())
                    .loan(testLoan)
                    .schedule(overdueSchedule)
                    .emiNo(1)
                    .amount(2000L)
                    .daysOverdue(5)
                    .status(PenaltyStatus.UNPAID)
                    .appliedAt(LocalDateTime.now().minusDays(5))
                    .build();

            List<RepaymentSchedule> overdueSchedules = List.of(overdueSchedule);

            when(scheduleRepository.findOverdueSchedules(any(LocalDate.class))).thenReturn(overdueSchedules);
            when(penaltyRepository.findUnpaidByScheduleId(any())).thenReturn(List.of(existingPenalty));
            when(penaltyRepository.save(any(Penalty.class))).thenAnswer(i -> i.getArgument(0));
            when(penaltyRepository.sumUnpaidPenaltiesByLoanId(any())).thenReturn(4424L);
            when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

            delinquencyService.processOverdueLoans();

            // Should update existing penalty with new days
            assertThat(existingPenalty.getDaysOverdue()).isEqualTo(10);
            verify(penaltyRepository).save(existingPenalty);
        }

        @Test
        @DisplayName("Should handle empty overdue list")
        void shouldHandleEmptyOverdueList() {
            when(scheduleRepository.findOverdueSchedules(any(LocalDate.class))).thenReturn(new ArrayList<>());

            delinquencyService.processOverdueLoans();

            verify(penaltyRepository, never()).save(any());
            verify(loanService, never()).markAsDefaulted(any(), any());
        }
    }

    @Nested
    @DisplayName("getDaysOverdue")
    class GetDaysOverdueTests {

        @Test
        @DisplayName("Should return correct days overdue")
        void shouldReturnCorrectDaysOverdue() {
            overdueSchedule.setStatus(EmiStatus.OVERDUE);
            List<RepaymentSchedule> overdueSchedules = List.of(overdueSchedule);

            when(scheduleRepository.findByLoanIdAndStatusIn(eq(testLoan.getId()), anyList()))
                    .thenReturn(overdueSchedules);

            int dpd = delinquencyService.getDaysOverdue(testLoan);

            assertThat(dpd).isEqualTo(10);
        }

        @Test
        @DisplayName("Should return zero when no overdue EMIs")
        void shouldReturnZeroWhenNoOverdue() {
            when(scheduleRepository.findByLoanIdAndStatusIn(eq(testLoan.getId()), anyList()))
                    .thenReturn(new ArrayList<>());

            int dpd = delinquencyService.getDaysOverdue(testLoan);

            assertThat(dpd).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return days based on oldest overdue EMI")
        void shouldReturnDaysBasedOnOldestOverdue() {
            RepaymentSchedule olderOverdue = RepaymentSchedule.builder()
                    .id(UUID.randomUUID())
                    .loan(testLoan)
                    .emiNo(1)
                    .dueDate(LocalDate.now().minusDays(30))
                    .status(EmiStatus.OVERDUE)
                    .build();

            RepaymentSchedule newerOverdue = RepaymentSchedule.builder()
                    .id(UUID.randomUUID())
                    .loan(testLoan)
                    .emiNo(2)
                    .dueDate(LocalDate.now().minusDays(5))
                    .status(EmiStatus.OVERDUE)
                    .build();

            when(scheduleRepository.findByLoanIdAndStatusIn(eq(testLoan.getId()), anyList()))
                    .thenReturn(List.of(olderOverdue, newerOverdue));

            int dpd = delinquencyService.getDaysOverdue(testLoan);

            assertThat(dpd).isEqualTo(30); // Should be based on oldest
        }
    }
}
