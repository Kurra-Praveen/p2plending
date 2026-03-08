package com.llms.service.interest;

import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Interest Calculators Unit Tests")
class InterestCalculatorTest {

    private static final String TIMEZONE = "Asia/Kolkata";
    private static final long ONE_LAKH_PAISE = 10_000_000L; // ₹1,00,000

    @Nested
    @DisplayName("Annual Percentage Calculator Tests")
    class AnnualPercentageCalculatorTests {

        private AnnualPercentageCalculator calculator;

        @BeforeEach
        void setUp() {
            calculator = new AnnualPercentageCalculator();
        }

        @Test
        @DisplayName("Should return correct mode")
        void shouldReturnCorrectMode() {
            assertThat(calculator.getMode()).isEqualTo(InterestRateMode.ANNUAL_PERCENTAGE);
        }

        @Test
        @DisplayName("Should calculate monthly interest correctly for 12% annual rate")
        void shouldCalculateMonthlyInterest() {
            // 12% annual on 1 lakh for 30 days
            long principal = ONE_LAKH_PAISE;
            BigDecimal annualRate = BigDecimal.valueOf(12.0);
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 2, 1); // 31 days

            long interest = calculator.calculateInterest(
                    principal, annualRate, InterestType.REDUCING,
                    startDate, endDate, TIMEZONE);

            // Daily rate = 12 / 365 / 100 = 0.000328767
            // Interest = 10,000,000 * 0.000328767 * 31 = 101,918 paise (approx)
            assertThat(interest).isCloseTo(101918L, within(100L));
        }

        @Test
        @DisplayName("Should handle leap year correctly")
        void shouldHandleLeapYear() {
            long principal = ONE_LAKH_PAISE;
            BigDecimal annualRate = BigDecimal.valueOf(12.0);
            LocalDate startDate = LocalDate.of(2024, 2, 1); // Leap year
            LocalDate endDate = LocalDate.of(2024, 3, 1); // 29 days

            long interest = calculator.calculateInterest(
                    principal, annualRate, InterestType.REDUCING,
                    startDate, endDate, TIMEZONE);

            // Daily rate = 12 / 366 / 100 = 0.000327869
            // Interest = 10,000,000 * 0.000327869 * 29 = 95,082 paise (approx)
            assertThat(interest).isCloseTo(95082L, within(100L));
        }

        @Test
        @DisplayName("Should calculate daily interest correctly")
        void shouldCalculateDailyInterest() {
            long principal = ONE_LAKH_PAISE;
            BigDecimal annualRate = BigDecimal.valueOf(12.0);
            LocalDate date = LocalDate.of(2026, 6, 15);

            long dailyInterest = calculator.calculateDailyInterest(
                    principal, annualRate, date, TIMEZONE);

            // Daily = 10,000,000 * (12 / 365 / 100) = 3,288 paise (approx)
            assertThat(dailyInterest).isCloseTo(3288L, within(10L));
        }

        @Test
        @DisplayName("Should return zero for zero principal")
        void shouldReturnZeroForZeroPrincipal() {
            long interest = calculator.calculateInterest(
                    0L, BigDecimal.valueOf(12.0), InterestType.REDUCING,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), TIMEZONE);

            assertThat(interest).isZero();
        }

        @Test
        @DisplayName("Should return zero for zero rate")
        void shouldReturnZeroForZeroRate() {
            long interest = calculator.calculateInterest(
                    ONE_LAKH_PAISE, BigDecimal.ZERO, InterestType.REDUCING,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), TIMEZONE);

            assertThat(interest).isZero();
        }

        @Test
        @DisplayName("Should normalize to same annual rate")
        void shouldNormalizeToSameRate() {
            BigDecimal rate = BigDecimal.valueOf(12.0);
            BigDecimal normalized = calculator.normalizeToAnnualRate(rate, LocalDate.now());

            assertThat(normalized).isEqualByComparingTo(rate);
        }
    }

    @Nested
    @DisplayName("Monthly Percentage Calculator Tests")
    class MonthlyPercentageCalculatorTests {

        private MonthlyPercentageCalculator calculator;

        @BeforeEach
        void setUp() {
            calculator = new MonthlyPercentageCalculator();
        }

        @Test
        @DisplayName("Should return correct mode")
        void shouldReturnCorrectMode() {
            assertThat(calculator.getMode()).isEqualTo(InterestRateMode.MONTHLY_PERCENTAGE);
        }

        @Test
        @DisplayName("Should calculate interest for 1.5% monthly rate")
        void shouldCalculateMonthlyInterest() {
            long principal = ONE_LAKH_PAISE;
            BigDecimal monthlyRate = BigDecimal.valueOf(1.5);
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 2, 1); // 31 days

            long interest = calculator.calculateInterest(
                    principal, monthlyRate, InterestType.REDUCING,
                    startDate, endDate, TIMEZONE);

            // Monthly interest = 1,00,000 * 1.5% = 1,500 = 150,000 paise
            // But calculated per day and summed
            assertThat(interest).isCloseTo(150000L, within(1000L));
        }

        @Test
        @DisplayName("Should calculate daily interest from monthly rate")
        void shouldCalculateDailyInterest() {
            long principal = ONE_LAKH_PAISE;
            BigDecimal monthlyRate = BigDecimal.valueOf(1.5);
            LocalDate date = LocalDate.of(2026, 1, 15); // January has 31 days

            long dailyInterest = calculator.calculateDailyInterest(
                    principal, monthlyRate, date, TIMEZONE);

            // Daily = 10,000,000 * (1.5 / 31 / 100) = 4,839 paise (approx)
            assertThat(dailyInterest).isCloseTo(4839L, within(10L));
        }

        @Test
        @DisplayName("Should normalize monthly to annual rate")
        void shouldNormalizeToAnnualRate() {
            BigDecimal monthlyRate = BigDecimal.valueOf(1.0);
            BigDecimal annualRate = calculator.normalizeToAnnualRate(monthlyRate, LocalDate.now());

            assertThat(annualRate).isEqualByComparingTo(BigDecimal.valueOf(12.0));
        }

        @Test
        @DisplayName("Should convert from annual to monthly rate")
        void shouldConvertFromAnnualRate() {
            BigDecimal annualRate = BigDecimal.valueOf(12.0);
            BigDecimal monthlyRate = calculator.fromAnnualRate(annualRate, LocalDate.now());

            assertThat(monthlyRate).isEqualByComparingTo(BigDecimal.valueOf(1.0));
        }
    }

    @Nested
    @DisplayName("Daily Percentage Calculator Tests")
    class DailyPercentageCalculatorTests {

        private DailyPercentageCalculator calculator;

        @BeforeEach
        void setUp() {
            calculator = new DailyPercentageCalculator();
        }

        @Test
        @DisplayName("Should return correct mode")
        void shouldReturnCorrectMode() {
            assertThat(calculator.getMode()).isEqualTo(InterestRateMode.DAILY_PERCENTAGE);
        }

        @Test
        @DisplayName("Should calculate interest for 0.05% daily rate")
        void shouldCalculateDailyRateInterest() {
            long principal = ONE_LAKH_PAISE;
            BigDecimal dailyRate = BigDecimal.valueOf(0.05);
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 1, 31); // 30 days

            long interest = calculator.calculateInterest(
                    principal, dailyRate, InterestType.REDUCING,
                    startDate, endDate, TIMEZONE);

            // Interest = 10,000,000 * 0.05 / 100 * 30 = 150,000 paise
            assertThat(interest).isEqualTo(150000L);
        }

        @Test
        @DisplayName("Should calculate single day interest")
        void shouldCalculateSingleDayInterest() {
            long principal = ONE_LAKH_PAISE;
            BigDecimal dailyRate = BigDecimal.valueOf(0.05);
            LocalDate date = LocalDate.of(2026, 1, 15);

            long dailyInterest = calculator.calculateDailyInterest(
                    principal, dailyRate, date, TIMEZONE);

            // Daily = 10,000,000 * 0.05 / 100 = 5,000 paise
            assertThat(dailyInterest).isEqualTo(5000L);
        }

        @Test
        @DisplayName("Should normalize daily to annual rate")
        void shouldNormalizeToAnnualRate() {
            BigDecimal dailyRate = BigDecimal.valueOf(0.05);
            LocalDate date = LocalDate.of(2026, 1, 1); // Non-leap year

            BigDecimal annualRate = calculator.normalizeToAnnualRate(dailyRate, date);

            // Annual = 0.05 * 365 = 18.25
            assertThat(annualRate).isEqualByComparingTo(BigDecimal.valueOf(18.25));
        }
    }

    @Nested
    @DisplayName("Fixed Rupee Per Day Calculator Tests")
    class FixedRupeePerDayCalculatorTests {

        private FixedRupeePerDayCalculator calculator;

        @BeforeEach
        void setUp() {
            calculator = new FixedRupeePerDayCalculator();
        }

        @Test
        @DisplayName("Should return correct mode")
        void shouldReturnCorrectMode() {
            assertThat(calculator.getMode()).isEqualTo(InterestRateMode.FIXED_RUPEE_PER_DAY);
        }

        @Test
        @DisplayName("Should calculate interest for ₹50 per day")
        void shouldCalculateFixedDailyInterest() {
            long principal = ONE_LAKH_PAISE; // Not used in calculation
            BigDecimal fixedPerDay = BigDecimal.valueOf(5000); // ₹50 = 5000 paise
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 1, 31); // 30 days

            long interest = calculator.calculateInterest(
                    principal, fixedPerDay, InterestType.FLAT,
                    startDate, endDate, TIMEZONE);

            // Interest = 5000 * 30 = 150,000 paise
            assertThat(interest).isEqualTo(150000L);
        }

        @Test
        @DisplayName("Should return fixed daily amount")
        void shouldReturnFixedDailyAmount() {
            long principal = ONE_LAKH_PAISE;
            BigDecimal fixedPerDay = BigDecimal.valueOf(5000);
            LocalDate date = LocalDate.of(2026, 1, 15);

            long dailyInterest = calculator.calculateDailyInterest(
                    principal, fixedPerDay, date, TIMEZONE);

            assertThat(dailyInterest).isEqualTo(5000L);
        }

        @Test
        @DisplayName("Should be independent of principal")
        void shouldBeIndependentOfPrincipal() {
            BigDecimal fixedPerDay = BigDecimal.valueOf(5000);
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 1, 11); // 10 days

            long interest1 = calculator.calculateInterest(
                    ONE_LAKH_PAISE, fixedPerDay, InterestType.FLAT,
                    startDate, endDate, TIMEZONE);

            long interest2 = calculator.calculateInterest(
                    ONE_LAKH_PAISE * 10, fixedPerDay, InterestType.FLAT,
                    startDate, endDate, TIMEZONE);

            assertThat(interest1).isEqualTo(interest2);
        }
    }

    @Nested
    @DisplayName("Fixed Rupee Per Month Calculator Tests")
    class FixedRupeePerMonthCalculatorTests {

        private FixedRupeePerMonthCalculator calculator;

        @BeforeEach
        void setUp() {
            calculator = new FixedRupeePerMonthCalculator();
        }

        @Test
        @DisplayName("Should return correct mode")
        void shouldReturnCorrectMode() {
            assertThat(calculator.getMode()).isEqualTo(InterestRateMode.FIXED_RUPEE_PER_MONTH);
        }

        @Test
        @DisplayName("Should calculate interest for ₹500 per month")
        void shouldCalculateFixedMonthlyInterest() {
            long principal = ONE_LAKH_PAISE;
            BigDecimal fixedPerMonth = BigDecimal.valueOf(50000); // ₹500 = 50000 paise
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 2, 1); // Full month (31 days)

            long interest = calculator.calculateInterest(
                    principal, fixedPerMonth, InterestType.FLAT,
                    startDate, endDate, TIMEZONE);

            // For a full month, should be approximately the fixed amount
            assertThat(interest).isCloseTo(50000L, within(100L));
        }

        @Test
        @DisplayName("Should pro-rate for partial month")
        void shouldProRateForPartialMonth() {
            long principal = ONE_LAKH_PAISE;
            BigDecimal fixedPerMonth = BigDecimal.valueOf(31000); // ₹310 = 31000 paise (₹10/day in Jan)
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 1, 16); // 15 days

            long interest = calculator.calculateInterest(
                    principal, fixedPerMonth, InterestType.FLAT,
                    startDate, endDate, TIMEZONE);

            // Pro-rated: 31000 / 31 * 15 = 15,000 paise
            assertThat(interest).isCloseTo(15000L, within(100L));
        }

        @Test
        @DisplayName("Should handle different month lengths")
        void shouldHandleDifferentMonthLengths() {
            long principal = ONE_LAKH_PAISE;
            BigDecimal fixedPerMonth = BigDecimal.valueOf(30000);

            // February (28 days)
            LocalDate febStart = LocalDate.of(2026, 2, 1);
            LocalDate febEnd = LocalDate.of(2026, 3, 1);
            long febInterest = calculator.calculateInterest(
                    principal, fixedPerMonth, InterestType.FLAT,
                    febStart, febEnd, TIMEZONE);

            // March (31 days)
            LocalDate marStart = LocalDate.of(2026, 3, 1);
            LocalDate marEnd = LocalDate.of(2026, 4, 1);
            long marInterest = calculator.calculateInterest(
                    principal, fixedPerMonth, InterestType.FLAT,
                    marStart, marEnd, TIMEZONE);

            // Both should equal the fixed monthly amount
            assertThat(febInterest).isCloseTo(30000L, within(100L));
            assertThat(marInterest).isCloseTo(30000L, within(100L));
        }
    }

    @Nested
    @DisplayName("Interest Calculator Factory Tests")
    class InterestCalculatorFactoryTests {

        private InterestCalculatorFactory factory;

        @BeforeEach
        void setUp() {
            factory = new InterestCalculatorFactory(java.util.List.of(
                    new AnnualPercentageCalculator(),
                    new MonthlyPercentageCalculator(),
                    new DailyPercentageCalculator(),
                    new FixedRupeePerDayCalculator(),
                    new FixedRupeePerMonthCalculator()
            ));
        }

        @ParameterizedTest
        @CsvSource({
                "ANNUAL_PERCENTAGE",
                "MONTHLY_PERCENTAGE",
                "DAILY_PERCENTAGE",
                "FIXED_RUPEE_PER_DAY",
                "FIXED_RUPEE_PER_MONTH"
        })
        @DisplayName("Should return correct calculator for each mode")
        void shouldReturnCorrectCalculator(String modeStr) {
            InterestRateMode mode = InterestRateMode.valueOf(modeStr);
            InterestCalculator calculator = factory.getCalculator(mode);

            assertThat(calculator).isNotNull();
            assertThat(calculator.getMode()).isEqualTo(mode);
        }

        @Test
        @DisplayName("Should return default calculator")
        void shouldReturnDefaultCalculator() {
            InterestCalculator calculator = factory.getDefaultCalculator();

            assertThat(calculator).isNotNull();
            assertThat(calculator.getMode()).isEqualTo(InterestRateMode.ANNUAL_PERCENTAGE);
        }
    }

    @Nested
    @DisplayName("Financial Accuracy Tests")
    class FinancialAccuracyTests {

        private AnnualPercentageCalculator calculator;

        @BeforeEach
        void setUp() {
            calculator = new AnnualPercentageCalculator();
        }

        @Test
        @DisplayName("Two independent implementations should produce identical results")
        void twoImplementationsShouldProduceSameResults() {
            // This tests the spec requirement: "Two engineers implementing this
            // independently must arrive at identical results."

            long principal = 12345678L; // ₹1,23,456.78
            BigDecimal rate = BigDecimal.valueOf(15.75);
            LocalDate start = LocalDate.of(2026, 3, 15);
            LocalDate end = LocalDate.of(2026, 4, 15); // 31 days

            // Calculate using the calculator
            long interest = calculator.calculateInterest(
                    principal, rate, InterestType.REDUCING, start, end, TIMEZONE);

            // Manual calculation
            // Daily rate = 15.75 / 365 / 100 = 0.000431507
            // Interest = 12345678 * 0.000431507 * 31 = 165,075 paise (approx)
            double manualDailyRate = 15.75 / 365.0 / 100.0;
            double manualInterest = principal * manualDailyRate * 31;

            assertThat(interest).isCloseTo((long) manualInterest, within(10L));
        }

        @Test
        @DisplayName("Should maintain precision for large amounts")
        void shouldMaintainPrecisionForLargeAmounts() {
            long principal = 1_000_000_000_00L; // ₹1,00,00,00,000 (100 crore)
            BigDecimal rate = BigDecimal.valueOf(12.0);
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2027, 1, 1); // Full year

            long interest = calculator.calculateInterest(
                    principal, rate, InterestType.REDUCING, start, end, TIMEZONE);

            // Interest = 100 crore * 12% = 12 crore = 12,00,00,00,000 paise
            assertThat(interest).isCloseTo(12_000_000_000L, within(10000L));
        }

        @Test
        @DisplayName("Should have zero paise tolerance for exact calculations")
        void shouldHaveZeroPaiseTolerance() {
            // For fixed amounts, there should be no rounding error
            FixedRupeePerDayCalculator fixedCalc = new FixedRupeePerDayCalculator();
            long principal = ONE_LAKH_PAISE;
            BigDecimal fixedPerDay = BigDecimal.valueOf(1000); // Exactly ₹10
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 1, 11); // Exactly 10 days

            long interest = fixedCalc.calculateInterest(
                    principal, fixedPerDay, InterestType.FLAT, start, end, TIMEZONE);

            // Should be exactly 10,000 paise (₹100)
            assertThat(interest).isEqualTo(10000L);
        }
    }
}
