package com.llms.service.schedule;

import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;
import com.llms.enums.LoanFrequency;
import com.llms.service.interest.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Schedule Generator Unit Tests")
class ScheduleGeneratorTest {

    private static final String TIMEZONE = "Asia/Kolkata";
    private static final long ONE_LAKH_PAISE = 10_000_000L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);

    private InterestCalculatorFactory calculatorFactory;
    private MonthlyScheduleGenerator monthlyGenerator;
    private WeeklyScheduleGenerator weeklyGenerator;

    @BeforeEach
    void setUp() {
        calculatorFactory = new InterestCalculatorFactory(List.of(
                new AnnualPercentageCalculator(),
                new MonthlyPercentageCalculator(),
                new DailyPercentageCalculator(),
                new FixedRupeePerDayCalculator(),
                new FixedRupeePerMonthCalculator()
        ));
        monthlyGenerator = new MonthlyScheduleGenerator(calculatorFactory);
        weeklyGenerator = new WeeklyScheduleGenerator(calculatorFactory);
    }

    @Nested
    @DisplayName("Monthly Schedule Generator Tests")
    class MonthlyScheduleGeneratorTests {

        @Test
        @DisplayName("Should return correct frequency")
        void shouldReturnCorrectFrequency() {
            assertThat(monthlyGenerator.getFrequency()).isEqualTo(LoanFrequency.MONTHLY);
        }

        @Test
        @DisplayName("Should generate correct number of installments")
        void shouldGenerateCorrectInstallments() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .tenure(12)
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = monthlyGenerator.generateSchedule(request);

            assertThat(result.getItems()).hasSize(12);
        }

        @Test
        @DisplayName("Should calculate reducing balance EMI correctly")
        void shouldCalculateReducingBalanceEmi() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .tenure(12)
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = monthlyGenerator.generateSchedule(request);

            // Standard EMI for 1 lakh @ 12% for 12 months ≈ ₹8,885
            assertThat(result.getEmiAmount()).isCloseTo(888488L, within(1000L));
        }

        @Test
        @DisplayName("Should calculate flat interest correctly")
        void shouldCalculateFlatInterest() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.FLAT)
                    .tenure(12)
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = monthlyGenerator.generateSchedule(request);

            // Flat interest = 1,00,000 * 12% = 12,000 = 1,200,000 paise
            assertThat(result.getTotalInterest()).isCloseTo(1200000L, within(10000L));
            // Total = 1,00,000 + 12,000 = 1,12,000 = 11,200,000 paise
            assertThat(result.getTotalPayable()).isCloseTo(11200000L, within(10000L));
        }

        @Test
        @DisplayName("Should have correct due dates for monthly schedule")
        void shouldHaveCorrectDueDates() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .tenure(6)
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = monthlyGenerator.generateSchedule(request);
            List<ScheduleGenerator.ScheduleItem> items = result.getItems();

            assertThat(items.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(items.get(1).getDueDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(items.get(5).getDueDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        }

        @Test
        @DisplayName("Should sum principal to total principal")
        void shouldSumPrincipalCorrectly() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .tenure(12)
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = monthlyGenerator.generateSchedule(request);

            long totalPrincipal = result.getItems().stream()
                    .mapToLong(ScheduleGenerator.ScheduleItem::getPrincipalDue)
                    .sum();

            assertThat(totalPrincipal).isEqualTo(ONE_LAKH_PAISE);
        }

        @Test
        @DisplayName("Should have decreasing closing balance")
        void shouldHaveDecreasingClosingBalance() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .tenure(12)
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = monthlyGenerator.generateSchedule(request);
            List<ScheduleGenerator.ScheduleItem> items = result.getItems();

            for (int i = 1; i < items.size(); i++) {
                assertThat(items.get(i).getOpeningBalance())
                        .isLessThan(items.get(i - 1).getOpeningBalance());
            }

            // Last closing balance should be 0
            assertThat(items.get(items.size() - 1).getClosingBalance()).isZero();
        }

        @Test
        @DisplayName("Should work with monthly percentage rate mode")
        void shouldWorkWithMonthlyPercentageMode() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(1.0)) // 1% per month
                    .interestRateMode(InterestRateMode.MONTHLY_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .tenure(12)
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = monthlyGenerator.generateSchedule(request);

            assertThat(result.getItems()).hasSize(12);
            assertThat(result.getEmiAmount()).isPositive();
            assertThat(result.getTotalInterest()).isPositive();
        }
    }

    @Nested
    @DisplayName("Weekly Schedule Generator Tests")
    class WeeklyScheduleGeneratorTests {

        @Test
        @DisplayName("Should return correct frequency")
        void shouldReturnCorrectFrequency() {
            assertThat(weeklyGenerator.getFrequency()).isEqualTo(LoanFrequency.WEEKLY);
        }

        @Test
        @DisplayName("Should generate correct number of weekly installments")
        void shouldGenerateCorrectWeeklyInstallments() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .tenure(52) // 52 weeks
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = weeklyGenerator.generateSchedule(request);

            assertThat(result.getItems()).hasSize(52);
        }

        @Test
        @DisplayName("Should have correct weekly due dates")
        void shouldHaveCorrectWeeklyDueDates() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .tenure(4) // 4 weeks
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = weeklyGenerator.generateSchedule(request);
            List<ScheduleGenerator.ScheduleItem> items = result.getItems();

            assertThat(items.get(0).getDueDate()).isEqualTo(START_DATE.plusWeeks(1));
            assertThat(items.get(1).getDueDate()).isEqualTo(START_DATE.plusWeeks(2));
            assertThat(items.get(3).getDueDate()).isEqualTo(START_DATE.plusWeeks(4));
        }

        @Test
        @DisplayName("Should calculate flat interest for weekly schedule")
        void shouldCalculateFlatInterestWeekly() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.FLAT)
                    .tenure(52)
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = weeklyGenerator.generateSchedule(request);

            // 52 weeks ≈ 1 year, so interest should be approximately 12,000
            assertThat(result.getTotalInterest()).isCloseTo(1200000L, within(50000L));
        }

        @Test
        @DisplayName("Should have smaller weekly EMI than monthly EMI for same loan")
        void shouldHaveSmallerWeeklyEmi() {
            // Same principal, rate, and approximately same duration
            ScheduleGenerator.ScheduleRequest monthlyRequest = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .tenure(12) // 12 months
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleRequest weeklyRequest = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .tenure(52) // 52 weeks ≈ 12 months
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult monthlyResult = monthlyGenerator.generateSchedule(monthlyRequest);
            ScheduleGenerator.ScheduleResult weeklyResult = weeklyGenerator.generateSchedule(weeklyRequest);

            // Weekly EMI should be smaller than monthly EMI
            assertThat(weeklyResult.getEmiAmount()).isLessThan(monthlyResult.getEmiAmount());

            // But weekly EMI * 52 should be approximately equal to monthly EMI * 12
            long weeklyTotal = weeklyResult.getEmiAmount() * 52;
            long monthlyTotal = monthlyResult.getEmiAmount() * 12;
            assertThat(weeklyTotal).isCloseTo(monthlyTotal, within(100000L));
        }

        @Test
        @DisplayName("Should sum weekly principal to total principal")
        void shouldSumWeeklyPrincipalCorrectly() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .tenure(26) // 26 weeks
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = weeklyGenerator.generateSchedule(request);

            long totalPrincipal = result.getItems().stream()
                    .mapToLong(ScheduleGenerator.ScheduleItem::getPrincipalDue)
                    .sum();

            assertThat(totalPrincipal).isEqualTo(ONE_LAKH_PAISE);
        }
    }

    @Nested
    @DisplayName("Schedule Generator Factory Tests")
    class ScheduleGeneratorFactoryTests {

        private ScheduleGeneratorFactory factory;

        @BeforeEach
        void setUpFactory() {
            factory = new ScheduleGeneratorFactory(List.of(
                    monthlyGenerator,
                    weeklyGenerator
            ));
        }

        @Test
        @DisplayName("Should return monthly generator")
        void shouldReturnMonthlyGenerator() {
            ScheduleGenerator generator = factory.getGenerator(LoanFrequency.MONTHLY);
            assertThat(generator.getFrequency()).isEqualTo(LoanFrequency.MONTHLY);
        }

        @Test
        @DisplayName("Should return weekly generator")
        void shouldReturnWeeklyGenerator() {
            ScheduleGenerator generator = factory.getGenerator(LoanFrequency.WEEKLY);
            assertThat(generator.getFrequency()).isEqualTo(LoanFrequency.WEEKLY);
        }

        @Test
        @DisplayName("Should return default generator as monthly")
        void shouldReturnDefaultGenerator() {
            ScheduleGenerator generator = factory.getDefaultGenerator();
            assertThat(generator.getFrequency()).isEqualTo(LoanFrequency.MONTHLY);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle minimum tenure of 1 month")
        void shouldHandleMinimumTenure() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.FLAT)
                    .tenure(1)
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = monthlyGenerator.generateSchedule(request);

            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).getPrincipalDue()).isEqualTo(ONE_LAKH_PAISE);
        }

        @Test
        @DisplayName("Should handle long tenure of 360 months")
        void shouldHandleLongTenure() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE * 100) // 1 crore
                    .interestRate(BigDecimal.valueOf(10.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .tenure(360) // 30 years
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = monthlyGenerator.generateSchedule(request);

            assertThat(result.getItems()).hasSize(360);
            assertThat(result.getItems().get(359).getClosingBalance()).isZero();
        }

        @Test
        @DisplayName("Should handle zero interest rate")
        void shouldHandleZeroInterestRate() {
            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(BigDecimal.ZERO)
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.FLAT)
                    .tenure(10)
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = monthlyGenerator.generateSchedule(request);

            assertThat(result.getTotalInterest()).isZero();
            assertThat(result.getTotalPayable()).isEqualTo(ONE_LAKH_PAISE);
        }

        @ParameterizedTest
        @CsvSource({
                "ANNUAL_PERCENTAGE, 12.0",
                "MONTHLY_PERCENTAGE, 1.0",
                "DAILY_PERCENTAGE, 0.03"
        })
        @DisplayName("Should work with different interest rate modes")
        void shouldWorkWithDifferentModes(String modeStr, String rateStr) {
            InterestRateMode mode = InterestRateMode.valueOf(modeStr);
            BigDecimal rate = new BigDecimal(rateStr);

            ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                    .principal(ONE_LAKH_PAISE)
                    .interestRate(rate)
                    .interestRateMode(mode)
                    .interestType(InterestType.REDUCING)
                    .tenure(12)
                    .startDate(START_DATE)
                    .timezone(TIMEZONE)
                    .build();

            ScheduleGenerator.ScheduleResult result = monthlyGenerator.generateSchedule(request);

            assertThat(result.getItems()).hasSize(12);
            assertThat(result.getEmiAmount()).isPositive();
        }
    }
}
