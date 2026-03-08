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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Charge Service Unit Tests")
class ChargeServiceTest {

    @Mock
    private ChargeDefinitionRepository chargeDefinitionRepository;

    @Mock
    private LoanChargeRepository loanChargeRepository;

    @Mock
    private ChargeScheduleRepository chargeScheduleRepository;

    @InjectMocks
    private ChargeService chargeService;

    private Loan testLoan;
    private ChargeDefinition processingFeeDefinition;
    private ChargeDefinition insuranceDefinition;

    @BeforeEach
    void setUp() {
        testLoan = Loan.builder()
                .id(UUID.randomUUID())
                .principalAmount(10_000_000L) // 1 lakh
                .outstandingCharges(0L)
                .build();

        processingFeeDefinition = ChargeDefinition.builder()
                .id(UUID.randomUUID())
                .name("Processing Fee")
                .code("PROCESSING_FEE")
                .chargeType(ChargeType.PROCESSING_FEE)
                .calculationType(ChargeCalculationType.PERCENTAGE_OF_PRINCIPAL)
                .percentage(BigDecimal.valueOf(1.5))
                .applicationTiming(ChargeApplicationTiming.DISBURSEMENT)
                .isActive(true)
                .isMandatory(true)
                .build();

        insuranceDefinition = ChargeDefinition.builder()
                .id(UUID.randomUUID())
                .name("Insurance Premium")
                .code("INSURANCE")
                .chargeType(ChargeType.INSURANCE)
                .calculationType(ChargeCalculationType.FIXED)
                .amount(50000L) // ₹500
                .applicationTiming(ChargeApplicationTiming.FIRST_EMI)
                .isActive(true)
                .isMandatory(false)
                .build();
    }

    @Nested
    @DisplayName("Get Charge Definitions Tests")
    class GetChargeDefinitionsTests {

        @Test
        @DisplayName("Should return active charge definitions")
        void shouldReturnActiveChargeDefinitions() {
            when(chargeDefinitionRepository.findByIsActiveTrue())
                    .thenReturn(List.of(processingFeeDefinition, insuranceDefinition));

            List<ChargeDefinition> result = chargeService.getActiveChargeDefinitions();

            assertThat(result).hasSize(2);
            verify(chargeDefinitionRepository).findByIsActiveTrue();
        }

        @Test
        @DisplayName("Should return mandatory charges")
        void shouldReturnMandatoryCharges() {
            when(chargeDefinitionRepository.findByIsMandatoryTrueAndIsActiveTrue())
                    .thenReturn(List.of(processingFeeDefinition));

            List<ChargeDefinition> result = chargeService.getMandatoryCharges();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("PROCESSING_FEE");
        }
    }

    @Nested
    @DisplayName("Apply Charge Tests")
    class ApplyChargeTests {

        @Test
        @DisplayName("Should calculate percentage-based charge correctly")
        void shouldCalculatePercentageCharge() {
            when(loanChargeRepository.save(any(LoanCharge.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            LoanCharge charge = chargeService.applyCharge(
                    testLoan,
                    processingFeeDefinition,
                    LocalDate.of(2026, 1, 15),
                    List.of()
            );

            // 1 lakh * 1.5% = 1,500 = 150,000 paise
            assertThat(charge.getAmount()).isEqualTo(150000L);
            assertThat(charge.getChargeType()).isEqualTo(ChargeType.PROCESSING_FEE);
            assertThat(charge.getStatus()).isEqualTo(ChargeStatus.PENDING);
        }

        @Test
        @DisplayName("Should calculate fixed charge correctly")
        void shouldCalculateFixedCharge() {
            when(loanChargeRepository.save(any(LoanCharge.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            LoanCharge charge = chargeService.applyCharge(
                    testLoan,
                    insuranceDefinition,
                    LocalDate.of(2026, 1, 15),
                    List.of()
            );

            assertThat(charge.getAmount()).isEqualTo(50000L);
            assertThat(charge.getChargeType()).isEqualTo(ChargeType.INSURANCE);
        }

        @Test
        @DisplayName("Should set due date based on application timing - DISBURSEMENT")
        void shouldSetDueDateForDisbursement() {
            when(loanChargeRepository.save(any(LoanCharge.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            LocalDate disbursementDate = LocalDate.of(2026, 1, 15);
            LoanCharge charge = chargeService.applyCharge(
                    testLoan,
                    processingFeeDefinition,
                    disbursementDate,
                    List.of()
            );

            assertThat(charge.getDueDate()).isEqualTo(disbursementDate);
        }

        @Test
        @DisplayName("Should set due date based on application timing - FIRST_EMI")
        void shouldSetDueDateForFirstEmi() {
            when(loanChargeRepository.save(any(LoanCharge.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RepaymentSchedule firstSchedule = RepaymentSchedule.builder()
                    .dueDate(LocalDate.of(2026, 2, 15))
                    .build();

            LoanCharge charge = chargeService.applyCharge(
                    testLoan,
                    insuranceDefinition,
                    LocalDate.of(2026, 1, 15),
                    List.of(firstSchedule)
            );

            assertThat(charge.getDueDate()).isEqualTo(LocalDate.of(2026, 2, 15));
        }
    }

    @Nested
    @DisplayName("Outstanding Charges Tests")
    class OutstandingChargesTests {

        @Test
        @DisplayName("Should return outstanding charges")
        void shouldReturnOutstandingCharges() {
            LoanCharge pendingCharge = LoanCharge.builder()
                    .id(UUID.randomUUID())
                    .loan(testLoan)
                    .amount(100000L)
                    .amountPaid(0L)
                    .amountWaived(0L)
                    .status(ChargeStatus.PENDING)
                    .build();

            when(loanChargeRepository.findOutstandingChargesByLoanId(testLoan.getId()))
                    .thenReturn(List.of(pendingCharge));

            List<LoanCharge> result = chargeService.getOutstandingCharges(testLoan.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAmount()).isEqualTo(100000L);
        }

        @Test
        @DisplayName("Should calculate total outstanding charges")
        void shouldCalculateTotalOutstandingCharges() {
            when(loanChargeRepository.sumOutstandingChargesByLoanId(testLoan.getId()))
                    .thenReturn(250000L);

            long total = chargeService.getTotalOutstandingCharges(testLoan.getId());

            assertThat(total).isEqualTo(250000L);
        }

        @Test
        @DisplayName("Should return zero when no outstanding charges")
        void shouldReturnZeroWhenNoOutstandingCharges() {
            when(loanChargeRepository.sumOutstandingChargesByLoanId(testLoan.getId()))
                    .thenReturn(null);

            long total = chargeService.getTotalOutstandingCharges(testLoan.getId());

            assertThat(total).isZero();
        }
    }

    @Nested
    @DisplayName("Waive Charge Tests")
    class WaiveChargeTests {

        @Test
        @DisplayName("Should waive charge successfully")
        void shouldWaiveChargeSuccessfully() {
            LoanCharge charge = LoanCharge.builder()
                    .id(UUID.randomUUID())
                    .loan(testLoan)
                    .name("Test Charge")
                    .chargeType(ChargeType.PROCESSING_FEE)
                    .amount(100000L)
                    .amountPaid(0L)
                    .amountWaived(0L)
                    .status(ChargeStatus.PENDING)
                    .build();

            User admin = User.builder()
                    .id(UUID.randomUUID())
                    .email("admin@test.com")
                    .build();

            when(loanChargeRepository.findById(charge.getId()))
                    .thenReturn(Optional.of(charge));
            when(loanChargeRepository.save(any(LoanCharge.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(loanChargeRepository.sumOutstandingChargesByLoanId(testLoan.getId()))
                    .thenReturn(0L);

            LoanCharge result = chargeService.waiveCharge(charge.getId(), admin, "Customer goodwill");

            assertThat(result.getAmountWaived()).isEqualTo(100000L);
            assertThat(result.getStatus()).isEqualTo(ChargeStatus.WAIVED);
            assertThat(result.getWaivedBy()).isEqualTo(admin);
            assertThat(result.getWaiverReason()).isEqualTo("Customer goodwill");
        }

        @Test
        @DisplayName("Should partially waive charge")
        void shouldPartiallyWaiveCharge() {
            LoanCharge charge = LoanCharge.builder()
                    .id(UUID.randomUUID())
                    .loan(testLoan)
                    .name("Test Charge")
                    .chargeType(ChargeType.PROCESSING_FEE)
                    .amount(100000L)
                    .amountPaid(50000L) // Already partially paid
                    .amountWaived(0L)
                    .status(ChargeStatus.PARTIAL)
                    .build();

            User admin = User.builder()
                    .id(UUID.randomUUID())
                    .build();

            when(loanChargeRepository.findById(charge.getId()))
                    .thenReturn(Optional.of(charge));
            when(loanChargeRepository.save(any(LoanCharge.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            when(loanChargeRepository.sumOutstandingChargesByLoanId(testLoan.getId()))
                    .thenReturn(0L);

            LoanCharge result = chargeService.waiveCharge(charge.getId(), admin, "Partial waiver");

            // Only the outstanding amount should be waived
            assertThat(result.getAmountWaived()).isEqualTo(50000L);
            assertThat(result.getStatus()).isEqualTo(ChargeStatus.PAID);
        }
    }

    @Nested
    @DisplayName("Charge Definition Calculation Tests")
    class ChargeDefinitionCalculationTests {

        @Test
        @DisplayName("Should calculate fixed amount correctly")
        void shouldCalculateFixedAmount() {
            ChargeDefinition fixed = ChargeDefinition.builder()
                    .calculationType(ChargeCalculationType.FIXED)
                    .amount(100000L)
                    .build();

            long result = fixed.calculateAmount(50_000_000L); // 5 lakh

            assertThat(result).isEqualTo(100000L); // Fixed regardless of principal
        }

        @Test
        @DisplayName("Should calculate percentage of principal correctly")
        void shouldCalculatePercentageOfPrincipal() {
            ChargeDefinition percentage = ChargeDefinition.builder()
                    .calculationType(ChargeCalculationType.PERCENTAGE_OF_PRINCIPAL)
                    .percentage(BigDecimal.valueOf(2.0))
                    .build();

            long result = percentage.calculateAmount(10_000_000L); // 1 lakh

            // 1 lakh * 2% = 2,000 = 200,000 paise
            assertThat(result).isEqualTo(200000L);
        }

        @Test
        @DisplayName("Should return zero for null values")
        void shouldReturnZeroForNullValues() {
            ChargeDefinition nullAmount = ChargeDefinition.builder()
                    .calculationType(ChargeCalculationType.FIXED)
                    .amount(null)
                    .build();

            long result = nullAmount.calculateAmount(10_000_000L);

            assertThat(result).isZero();
        }
    }
}
