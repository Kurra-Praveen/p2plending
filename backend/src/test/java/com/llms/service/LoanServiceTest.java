package com.llms.service;

import com.llms.dto.request.CreateLoanRequest;
import com.llms.dto.request.DisburseLoanRequest;
import com.llms.dto.response.LoanResponse;
import com.llms.entity.*;
import com.llms.enums.*;
import com.llms.exception.InvalidStateException;
import com.llms.exception.ResourceNotFoundException;
import com.llms.repository.*;
import com.llms.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService Unit Tests")
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private RepaymentScheduleRepository scheduleRepository;

    @Mock
    private DisbursementRepository disbursementRepository;

    @Mock
    private LoanStatusHistoryRepository statusHistoryRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private LoanService loanService;

    private User currentUser;
    private Borrower testBorrower;
    private Loan testLoan;

    @BeforeEach
    void setUp() {
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
                .status(LoanStatus.CREATED)
                .createdBy(currentUser)
                .repaymentSchedules(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("createLoan")
    class CreateLoanTests {

        @Test
        @DisplayName("Should create loan successfully")
        void shouldCreateLoanSuccessfully() {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(testBorrower.getId())
                    .principal(10000000L)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestType(InterestType.REDUCING)
                    .tenureMonths(12)
                    .build();

            when(borrowerRepository.findByIdAndNotDeleted(testBorrower.getId()))
                    .thenReturn(Optional.of(testBorrower));
            when(securityUtils.getCurrentUser()).thenReturn(currentUser);
            when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
                Loan loan = invocation.getArgument(0);
                loan.setId(UUID.randomUUID());
                return loan;
            });

            LoanResponse response = loanService.createLoan(request);

            assertThat(response).isNotNull();
            assertThat(response.getPrincipal()).isEqualTo(10000000L);
            assertThat(response.getStatus()).isEqualTo(LoanStatus.CREATED);
            assertThat(response.getEmiAmount()).isGreaterThan(0);

            verify(loanRepository).save(any(Loan.class));
            verify(scheduleRepository, times(12)).save(any(RepaymentSchedule.class));
            verify(statusHistoryRepository).save(any(LoanStatusHistory.class));
        }

        @Test
        @DisplayName("Should throw exception for non-existent borrower")
        void shouldThrowExceptionForNonExistentBorrower() {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(UUID.randomUUID())
                    .principal(10000000L)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestType(InterestType.REDUCING)
                    .tenureMonths(12)
                    .build();

            when(borrowerRepository.findByIdAndNotDeleted(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.createLoan(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should create loan with FLAT interest type")
        void shouldCreateLoanWithFlatInterest() {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(testBorrower.getId())
                    .principal(5000000L)
                    .interestRate(BigDecimal.valueOf(10.0))
                    .interestType(InterestType.FLAT)
                    .tenureMonths(6)
                    .build();

            when(borrowerRepository.findByIdAndNotDeleted(testBorrower.getId()))
                    .thenReturn(Optional.of(testBorrower));
            when(securityUtils.getCurrentUser()).thenReturn(currentUser);
            when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
                Loan loan = invocation.getArgument(0);
                loan.setId(UUID.randomUUID());
                return loan;
            });

            LoanResponse response = loanService.createLoan(request);

            assertThat(response.getInterestType()).isEqualTo(InterestType.FLAT);
            verify(scheduleRepository, times(6)).save(any(RepaymentSchedule.class));
        }
    }

    @Nested
    @DisplayName("disburseLoan")
    class DisburseLoanTests {

        @Test
        @DisplayName("Should disburse loan successfully")
        void shouldDisburseLoanSuccessfully() {
            DisburseLoanRequest request = DisburseLoanRequest.builder()
                    .amount(10000000L)
                    .mode(PaymentMode.BANK)
                    .reference("TXN123")
                    .build();

            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(loanRepository.findByIdAndCreatedBy(testLoan.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testLoan));
            when(securityUtils.getCurrentUser()).thenReturn(currentUser);
            when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);
            when(scheduleRepository.findByLoanIdOrderByEmiNoAsc(testLoan.getId()))
                    .thenReturn(new ArrayList<>());

            LoanResponse response = loanService.disburseLoan(testLoan.getId(), request);

            assertThat(testLoan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
            assertThat(testLoan.getDisbursedAt()).isNotNull();
            verify(disbursementRepository).save(any(Disbursement.class));
            verify(statusHistoryRepository).save(any(LoanStatusHistory.class));
        }

        @Test
        @DisplayName("Should throw exception when disbursing non-CREATED loan")
        void shouldThrowExceptionForNonCreatedLoan() {
            testLoan.setStatus(LoanStatus.ACTIVE);

            DisburseLoanRequest request = DisburseLoanRequest.builder()
                    .amount(10000000L)
                    .mode(PaymentMode.BANK)
                    .build();

            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(loanRepository.findByIdAndCreatedBy(testLoan.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testLoan));

            assertThatThrownBy(() -> loanService.disburseLoan(testLoan.getId(), request))
                    .isInstanceOf(InvalidStateException.class)
                    .hasMessageContaining("CREATED");
        }

        @Test
        @DisplayName("Should throw exception for mismatched amount")
        void shouldThrowExceptionForMismatchedAmount() {
            DisburseLoanRequest request = DisburseLoanRequest.builder()
                    .amount(5000000L) // Different from principal
                    .mode(PaymentMode.BANK)
                    .build();

            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(loanRepository.findByIdAndCreatedBy(testLoan.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testLoan));

            assertThatThrownBy(() -> loanService.disburseLoan(testLoan.getId(), request))
                    .isInstanceOf(InvalidStateException.class)
                    .satisfies(ex -> assertThat(((InvalidStateException) ex).getErrorCode()).isEqualTo("AMOUNT_MISMATCH"));
        }
    }

    @Nested
    @DisplayName("closeLoan")
    class CloseLoanTests {

        @Test
        @DisplayName("Should close loan with zero outstanding")
        void shouldCloseLoanWithZeroOutstanding() {
            testLoan.setStatus(LoanStatus.ACTIVE);
            testLoan.setOutstandingPrincipal(0L);
            testLoan.setOutstandingInterest(0L);
            testLoan.setOutstandingPenalty(0L);

            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(loanRepository.findByIdAndCreatedBy(testLoan.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testLoan));
            when(securityUtils.getCurrentUser()).thenReturn(currentUser);
            when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

            loanService.closeLoan(testLoan.getId());

            assertThat(testLoan.getStatus()).isEqualTo(LoanStatus.CLOSED);
            assertThat(testLoan.getClosedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when closing with outstanding balance")
        void shouldThrowExceptionWithOutstandingBalance() {
            testLoan.setStatus(LoanStatus.ACTIVE);
            testLoan.setOutstandingPrincipal(5000000L);

            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(loanRepository.findByIdAndCreatedBy(testLoan.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testLoan));

            assertThatThrownBy(() -> loanService.closeLoan(testLoan.getId()))
                    .isInstanceOf(InvalidStateException.class)
                    .satisfies(ex -> assertThat(((InvalidStateException) ex).getErrorCode()).isEqualTo("OUTSTANDING_BALANCE"));
        }

        @Test
        @DisplayName("Should throw exception when closing non-ACTIVE loan")
        void shouldThrowExceptionForNonActiveLoan() {
            testLoan.setStatus(LoanStatus.CREATED);

            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(loanRepository.findByIdAndCreatedBy(testLoan.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testLoan));

            assertThatThrownBy(() -> loanService.closeLoan(testLoan.getId()))
                    .isInstanceOf(InvalidStateException.class);
        }
    }

    @Nested
    @DisplayName("markAsDefaulted")
    class MarkAsDefaultedTests {

        @Test
        @DisplayName("Should mark loan as defaulted")
        void shouldMarkLoanAsDefaulted() {
            testLoan.setStatus(LoanStatus.ACTIVE);

            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(loanRepository.findByIdAndCreatedBy(testLoan.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testLoan));
            when(securityUtils.getCurrentUser()).thenReturn(currentUser);
            when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

            loanService.markAsDefaulted(testLoan.getId(), "90+ DPD");

            assertThat(testLoan.getStatus()).isEqualTo(LoanStatus.DEFAULTED);
            verify(statusHistoryRepository).save(any(LoanStatusHistory.class));
        }

        @Test
        @DisplayName("Should throw exception when defaulting non-ACTIVE loan")
        void shouldThrowExceptionForNonActiveLoan() {
            testLoan.setStatus(LoanStatus.CLOSED);

            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(loanRepository.findByIdAndCreatedBy(testLoan.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testLoan));

            assertThatThrownBy(() -> loanService.markAsDefaulted(testLoan.getId(), "Manual"))
                    .isInstanceOf(InvalidStateException.class);
        }
    }

    @Nested
    @DisplayName("getLoan")
    class GetLoanTests {

        @Test
        @DisplayName("Should get loan by ID")
        void shouldGetLoanById() {
            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(loanRepository.findByIdAndCreatedBy(testLoan.getId(), currentUser.getId()))
                    .thenReturn(Optional.of(testLoan));

            LoanResponse response = loanService.getLoan(testLoan.getId());

            assertThat(response).isNotNull();
            assertThat(response.getLoanId()).isEqualTo(testLoan.getId());
        }

        @Test
        @DisplayName("Should throw exception for non-existent loan")
        void shouldThrowExceptionForNonExistent() {
            UUID randomId = UUID.randomUUID();
            when(securityUtils.getCurrentUserId()).thenReturn(currentUser.getId());
            when(loanRepository.findByIdAndCreatedBy(randomId, currentUser.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> loanService.getLoan(randomId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
