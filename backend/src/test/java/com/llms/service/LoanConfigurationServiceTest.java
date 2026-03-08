package com.llms.service;

import com.llms.entity.*;
import com.llms.enums.*;
import com.llms.exception.InvalidStateException;
import com.llms.exception.ResourceNotFoundException;
import com.llms.repository.*;
import com.llms.security.SecurityUtils;
import com.llms.service.interest.*;
import com.llms.service.schedule.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Loan Configuration Service Unit Tests")
class LoanConfigurationServiceTest {

    @Mock
    private LoanConfigurationRepository configurationRepository;

    @Mock
    private LoanInterestConfigHistoryRepository historyRepository;

    @Mock
    private LoanService loanService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private AuditService auditService;

    private LoanConfigurationService configurationService;
    private ScheduleGeneratorFactory scheduleGeneratorFactory;

    private User testUser;
    private Loan testLoan;
    private LoanConfiguration testConfig;

    @BeforeEach
    void setUp() {
        // Create real calculator and schedule generator instances
        InterestCalculatorFactory calculatorFactory = new InterestCalculatorFactory(List.of(
                new AnnualPercentageCalculator(),
                new MonthlyPercentageCalculator(),
                new DailyPercentageCalculator(),
                new FixedRupeePerDayCalculator(),
                new FixedRupeePerMonthCalculator()
        ));

        scheduleGeneratorFactory = new ScheduleGeneratorFactory(List.of(
                new MonthlyScheduleGenerator(calculatorFactory),
                new WeeklyScheduleGenerator(calculatorFactory)
        ));

        configurationService = new LoanConfigurationService(
                configurationRepository,
                historyRepository,
                scheduleGeneratorFactory,
                loanService,
                securityUtils,
                auditService
        );

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("lender@test.com")
                .build();

        testLoan = Loan.builder()
                .id(UUID.randomUUID())
                .principalAmount(10_000_000L)
                .interestRate(BigDecimal.valueOf(12.0))
                .interestType(InterestType.REDUCING)
                .tenureMonths(12)
                .status(LoanStatus.ACTIVE)
                .disbursedAt(LocalDateTime.now().minusDays(30))
                .build();

        testConfig = LoanConfiguration.builder()
                .id(UUID.randomUUID())
                .loan(testLoan)
                .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                .interestRate(BigDecimal.valueOf(12.0))
                .interestType(InterestType.REDUCING)
                .frequency(LoanFrequency.MONTHLY)
                .tenureMonths(12)
                .timezone("Asia/Kolkata")
                .build();
    }

    @Nested
    @DisplayName("Create Initial Configuration Tests")
    class CreateInitialConfigurationTests {

        @Test
        @DisplayName("Should create initial configuration successfully")
        void shouldCreateInitialConfiguration() {
            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(historyRepository.save(any(LoanInterestConfigHistory.class)))
                    .thenAnswer(invocation -> {
                        LoanInterestConfigHistory history = invocation.getArgument(0);
                        history.setId(UUID.randomUUID());
                        return history;
                    });
            when(configurationRepository.save(any(LoanConfiguration.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            LoanConfiguration result = configurationService.createInitialConfiguration(
                    testLoan,
                    InterestRateMode.ANNUAL_PERCENTAGE,
                    BigDecimal.valueOf(12.0),
                    InterestType.REDUCING,
                    LoanFrequency.MONTHLY,
                    12,
                    null,
                    "Asia/Kolkata"
            );

            assertThat(result).isNotNull();
            assertThat(result.getInterestRateMode()).isEqualTo(InterestRateMode.ANNUAL_PERCENTAGE);
            assertThat(result.getInterestRate()).isEqualByComparingTo(BigDecimal.valueOf(12.0));
            assertThat(result.getFrequency()).isEqualTo(LoanFrequency.MONTHLY);

            verify(historyRepository).save(any(LoanInterestConfigHistory.class));
            verify(configurationRepository).save(any(LoanConfiguration.class));
        }

        @Test
        @DisplayName("Should create weekly configuration")
        void shouldCreateWeeklyConfiguration() {
            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(historyRepository.save(any(LoanInterestConfigHistory.class)))
                    .thenAnswer(invocation -> {
                        LoanInterestConfigHistory history = invocation.getArgument(0);
                        history.setId(UUID.randomUUID());
                        return history;
                    });
            when(configurationRepository.save(any(LoanConfiguration.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            LoanConfiguration result = configurationService.createInitialConfiguration(
                    testLoan,
                    InterestRateMode.MONTHLY_PERCENTAGE,
                    BigDecimal.valueOf(1.5),
                    InterestType.FLAT,
                    LoanFrequency.WEEKLY,
                    null,
                    52,
                    "Asia/Kolkata"
            );

            assertThat(result.getFrequency()).isEqualTo(LoanFrequency.WEEKLY);
            assertThat(result.getTenureUnits()).isEqualTo(52);
        }
    }

    @Nested
    @DisplayName("Get Configuration Tests")
    class GetConfigurationTests {

        @Test
        @DisplayName("Should return configuration for loan")
        void shouldReturnConfiguration() {
            when(configurationRepository.findByLoanId(testLoan.getId()))
                    .thenReturn(Optional.of(testConfig));

            LoanConfiguration result = configurationService.getConfiguration(testLoan.getId());

            assertThat(result).isEqualTo(testConfig);
        }

        @Test
        @DisplayName("Should throw when configuration not found")
        void shouldThrowWhenNotFound() {
            when(configurationRepository.findByLoanId(testLoan.getId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> configurationService.getConfiguration(testLoan.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Interest Config History Tests")
    class GetInterestConfigHistoryTests {

        @Test
        @DisplayName("Should return interest config history")
        void shouldReturnHistory() {
            LoanInterestConfigHistory history1 = LoanInterestConfigHistory.builder()
                    .id(UUID.randomUUID())
                    .loan(testLoan)
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .effectiveFrom(LocalDate.of(2026, 1, 1))
                    .effectiveTo(LocalDate.of(2026, 2, 28))
                    .build();

            LoanInterestConfigHistory history2 = LoanInterestConfigHistory.builder()
                    .id(UUID.randomUUID())
                    .loan(testLoan)
                    .interestRateMode(InterestRateMode.MONTHLY_PERCENTAGE)
                    .interestRate(BigDecimal.valueOf(1.0))
                    .effectiveFrom(LocalDate.of(2026, 3, 1))
                    .effectiveTo(null)
                    .build();

            when(historyRepository.findByLoanIdOrderByEffectiveFromDesc(testLoan.getId()))
                    .thenReturn(List.of(history2, history1));

            List<LoanInterestConfigHistory> result =
                    configurationService.getInterestConfigHistory(testLoan.getId());

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getEffectiveTo()).isNull(); // Active one first
        }

        @Test
        @DisplayName("Should return active configuration")
        void shouldReturnActiveConfig() {
            LoanInterestConfigHistory activeHistory = LoanInterestConfigHistory.builder()
                    .id(UUID.randomUUID())
                    .loan(testLoan)
                    .effectiveTo(null)
                    .build();

            when(historyRepository.findActiveByLoanId(testLoan.getId()))
                    .thenReturn(Optional.of(activeHistory));

            LoanInterestConfigHistory result = configurationService.getActiveInterestConfig(testLoan.getId());

            assertThat(result).isNotNull();
            assertThat(result.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Switch Interest Mode Tests")
    class SwitchInterestModeTests {

        @Test
        @DisplayName("Should switch interest mode for active loan")
        void shouldSwitchInterestMode() {
            LocalDate futureDate = LocalDate.now().plusDays(10);

            when(loanService.findLoanOrThrow(testLoan.getId())).thenReturn(testLoan);
            when(securityUtils.getCurrentUser()).thenReturn(testUser);
            when(configurationRepository.findByLoanId(testLoan.getId()))
                    .thenReturn(Optional.of(testConfig));
            when(historyRepository.findActiveByLoanId(testLoan.getId()))
                    .thenReturn(Optional.of(LoanInterestConfigHistory.builder()
                            .id(UUID.randomUUID())
                            .loan(testLoan)
                            .effectiveTo(null)
                            .build()));
            when(historyRepository.save(any(LoanInterestConfigHistory.class)))
                    .thenAnswer(invocation -> {
                        LoanInterestConfigHistory h = invocation.getArgument(0);
                        if (h.getId() == null) h.setId(UUID.randomUUID());
                        return h;
                    });
            when(configurationRepository.save(any(LoanConfiguration.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            LoanConfiguration result = configurationService.switchInterestMode(
                    testLoan.getId(),
                    InterestRateMode.MONTHLY_PERCENTAGE,
                    BigDecimal.valueOf(1.0),
                    futureDate,
                    "Customer requested lower rate"
            );

            assertThat(result.getInterestRateMode()).isEqualTo(InterestRateMode.MONTHLY_PERCENTAGE);
            assertThat(result.getInterestRate()).isEqualByComparingTo(BigDecimal.valueOf(1.0));

            verify(historyRepository, times(2)).save(any(LoanInterestConfigHistory.class));
        }

        @Test
        @DisplayName("Should reject switch for non-active loan")
        void shouldRejectSwitchForNonActiveLoan() {
            testLoan.setStatus(LoanStatus.CREATED);
            when(loanService.findLoanOrThrow(testLoan.getId())).thenReturn(testLoan);

            assertThatThrownBy(() -> configurationService.switchInterestMode(
                    testLoan.getId(),
                    InterestRateMode.MONTHLY_PERCENTAGE,
                    BigDecimal.valueOf(1.0),
                    LocalDate.now().plusDays(10),
                    "Test reason"
            )).isInstanceOf(InvalidStateException.class)
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("Should reject switch with past effective date")
        void shouldRejectSwitchWithPastDate() {
            when(loanService.findLoanOrThrow(testLoan.getId())).thenReturn(testLoan);

            assertThatThrownBy(() -> configurationService.switchInterestMode(
                    testLoan.getId(),
                    InterestRateMode.MONTHLY_PERCENTAGE,
                    BigDecimal.valueOf(1.0),
                    LocalDate.now().minusDays(1),
                    "Test reason"
            )).isInstanceOf(InvalidStateException.class)
                    .hasMessageContaining("future");
        }

        @Test
        @DisplayName("Should reject switch with today's date")
        void shouldRejectSwitchWithTodaysDate() {
            when(loanService.findLoanOrThrow(testLoan.getId())).thenReturn(testLoan);

            assertThatThrownBy(() -> configurationService.switchInterestMode(
                    testLoan.getId(),
                    InterestRateMode.MONTHLY_PERCENTAGE,
                    BigDecimal.valueOf(1.0),
                    LocalDate.now(),
                    "Test reason"
            )).isInstanceOf(InvalidStateException.class)
                    .hasMessageContaining("future");
        }

        @Test
        @DisplayName("Should reject restricted combination WEEKLY + DAILY + REDUCING")
        void shouldRejectRestrictedCombination() {
            testConfig.setFrequency(LoanFrequency.WEEKLY);
            testConfig.setInterestType(InterestType.REDUCING);

            when(loanService.findLoanOrThrow(testLoan.getId())).thenReturn(testLoan);
            when(configurationRepository.findByLoanId(testLoan.getId()))
                    .thenReturn(Optional.of(testConfig));

            assertThatThrownBy(() -> configurationService.switchInterestMode(
                    testLoan.getId(),
                    InterestRateMode.DAILY_PERCENTAGE,
                    BigDecimal.valueOf(0.05),
                    LocalDate.now().plusDays(10),
                    "Test reason"
            )).isInstanceOf(InvalidStateException.class)
                    .hasMessageContaining("restricted");
        }
    }

    @Nested
    @DisplayName("Generate Loan Preview Tests")
    class GenerateLoanPreviewTests {

        @Test
        @DisplayName("Should generate monthly loan preview")
        void shouldGenerateMonthlyPreview() {
            ScheduleGenerator.ScheduleResult result = configurationService.generateLoanPreview(
                    10_000_000L,
                    BigDecimal.valueOf(12.0),
                    InterestRateMode.ANNUAL_PERCENTAGE,
                    InterestType.REDUCING,
                    LoanFrequency.MONTHLY,
                    12,
                    LocalDate.of(2026, 1, 1),
                    "Asia/Kolkata"
            );

            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(12);
            assertThat(result.getEmiAmount()).isPositive();
            assertThat(result.getTotalInterest()).isPositive();
        }

        @Test
        @DisplayName("Should generate weekly loan preview")
        void shouldGenerateWeeklyPreview() {
            ScheduleGenerator.ScheduleResult result = configurationService.generateLoanPreview(
                    10_000_000L,
                    BigDecimal.valueOf(12.0),
                    InterestRateMode.ANNUAL_PERCENTAGE,
                    InterestType.FLAT,
                    LoanFrequency.WEEKLY,
                    52,
                    LocalDate.of(2026, 1, 1),
                    "Asia/Kolkata"
            );

            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(52);
        }

        @Test
        @DisplayName("Should work with different interest rate modes")
        void shouldWorkWithDifferentModes() {
            // Test with monthly percentage
            ScheduleGenerator.ScheduleResult result1 = configurationService.generateLoanPreview(
                    10_000_000L,
                    BigDecimal.valueOf(1.0),
                    InterestRateMode.MONTHLY_PERCENTAGE,
                    InterestType.REDUCING,
                    LoanFrequency.MONTHLY,
                    12,
                    LocalDate.of(2026, 1, 1),
                    null
            );

            // Test with daily percentage
            ScheduleGenerator.ScheduleResult result2 = configurationService.generateLoanPreview(
                    10_000_000L,
                    BigDecimal.valueOf(0.033),
                    InterestRateMode.DAILY_PERCENTAGE,
                    InterestType.REDUCING,
                    LoanFrequency.MONTHLY,
                    12,
                    LocalDate.of(2026, 1, 1),
                    null
            );

            assertThat(result1.getItems()).hasSize(12);
            assertThat(result2.getItems()).hasSize(12);
        }
    }

    @Nested
    @DisplayName("Interest Config History Entity Tests")
    class InterestConfigHistoryEntityTests {

        @Test
        @DisplayName("Should correctly identify active configuration")
        void shouldIdentifyActiveConfig() {
            LoanInterestConfigHistory active = LoanInterestConfigHistory.builder()
                    .effectiveFrom(LocalDate.of(2026, 1, 1))
                    .effectiveTo(null)
                    .build();

            LoanInterestConfigHistory inactive = LoanInterestConfigHistory.builder()
                    .effectiveFrom(LocalDate.of(2025, 1, 1))
                    .effectiveTo(LocalDate.of(2025, 12, 31))
                    .build();

            assertThat(active.isActive()).isTrue();
            assertThat(inactive.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should correctly check if active on date")
        void shouldCheckActiveOnDate() {
            LoanInterestConfigHistory history = LoanInterestConfigHistory.builder()
                    .effectiveFrom(LocalDate.of(2026, 3, 1))
                    .effectiveTo(LocalDate.of(2026, 6, 30))
                    .build();

            assertThat(history.isActiveOn(LocalDate.of(2026, 2, 28))).isFalse(); // Before
            assertThat(history.isActiveOn(LocalDate.of(2026, 3, 1))).isTrue();   // First day
            assertThat(history.isActiveOn(LocalDate.of(2026, 4, 15))).isTrue();  // Middle
            assertThat(history.isActiveOn(LocalDate.of(2026, 6, 30))).isTrue();  // Last day
            assertThat(history.isActiveOn(LocalDate.of(2026, 7, 1))).isFalse();  // After
        }

        @Test
        @DisplayName("Should handle open-ended effective period")
        void shouldHandleOpenEndedPeriod() {
            LoanInterestConfigHistory history = LoanInterestConfigHistory.builder()
                    .effectiveFrom(LocalDate.of(2026, 1, 1))
                    .effectiveTo(null)
                    .build();

            assertThat(history.isActiveOn(LocalDate.of(2026, 1, 1))).isTrue();
            assertThat(history.isActiveOn(LocalDate.of(2050, 12, 31))).isTrue(); // Far future
        }
    }
}
