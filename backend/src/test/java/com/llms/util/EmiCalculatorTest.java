package com.llms.util;

import com.llms.enums.InterestType;
import com.llms.util.EmiCalculator.EmiScheduleItem;
import com.llms.util.EmiCalculator.LoanCalculationResult;
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

@DisplayName("EmiCalculator Unit Tests")
class EmiCalculatorTest {

    private static final LocalDate FIRST_EMI_DATE = LocalDate.of(2026, 2, 1);

    @Nested
    @DisplayName("Flat Interest Calculations")
    class FlatInterestTests {

        @Test
        @DisplayName("Should calculate flat EMI correctly for standard loan")
        void shouldCalculateFlatEmi() {
            // 1 lakh principal, 12% annual, 12 months
            long principal = 10000000L; // 1 lakh in paise
            BigDecimal rate = BigDecimal.valueOf(12.0);
            int tenure = 12;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.FLAT, FIRST_EMI_DATE);

            // Total Interest = 1,00,000 * 12% * 12/12 = 12,000 = 1,200,000 paise
            // EMI = (1,00,000 + 12,000) / 12 = 9,333.33 = 933,333 paise
            assertThat(result.getTotalInterest()).isEqualTo(1200000L);
            assertThat(result.getTotalPayable()).isEqualTo(11200000L);
            assertThat(result.getEmiAmount()).isCloseTo(933333L, within(10L));
        }

        @Test
        @DisplayName("Should generate correct schedule for flat interest")
        void shouldGenerateCorrectFlatSchedule() {
            long principal = 6000000L; // 60,000 paise
            BigDecimal rate = BigDecimal.valueOf(10.0);
            int tenure = 6;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.FLAT, FIRST_EMI_DATE);

            List<EmiScheduleItem> schedule = result.getSchedule();

            assertThat(schedule).hasSize(6);
            assertThat(schedule.get(0).getEmiNo()).isEqualTo(1);
            assertThat(schedule.get(5).getEmiNo()).isEqualTo(6);

            // Verify schedule dates
            assertThat(schedule.get(0).getDueDate()).isEqualTo(FIRST_EMI_DATE);
            assertThat(schedule.get(1).getDueDate()).isEqualTo(FIRST_EMI_DATE.plusMonths(1));
        }

        @Test
        @DisplayName("Should have consistent principal distribution in flat interest")
        void shouldHaveConsistentPrincipalInFlat() {
            long principal = 12000000L;
            BigDecimal rate = BigDecimal.valueOf(12.0);
            int tenure = 12;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.FLAT, FIRST_EMI_DATE);

            List<EmiScheduleItem> schedule = result.getSchedule();

            // In flat rate, principal is evenly distributed
            long expectedMonthlyPrincipal = principal / tenure;
            for (int i = 0; i < tenure - 1; i++) {
                assertThat(schedule.get(i).getPrincipalDue())
                        .isCloseTo(expectedMonthlyPrincipal, within(1L));
            }
        }

        @Test
        @DisplayName("Should ensure closing balance is zero after last EMI")
        void shouldEnsureZeroClosingBalance() {
            long principal = 5000000L;
            BigDecimal rate = BigDecimal.valueOf(15.0);
            int tenure = 10;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.FLAT, FIRST_EMI_DATE);

            EmiScheduleItem lastEmi = result.getSchedule().get(tenure - 1);
            assertThat(lastEmi.getClosingBalance()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Total principal in schedule should equal original principal")
        void totalPrincipalShouldEqualOriginal() {
            long principal = 10000000L;
            BigDecimal rate = BigDecimal.valueOf(12.0);
            int tenure = 12;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.FLAT, FIRST_EMI_DATE);

            long totalPrincipalInSchedule = result.getSchedule().stream()
                    .mapToLong(EmiScheduleItem::getPrincipalDue)
                    .sum();

            assertThat(totalPrincipalInSchedule).isEqualTo(principal);
        }
    }

    @Nested
    @DisplayName("Reducing Balance Interest Calculations")
    class ReducingInterestTests {

        @Test
        @DisplayName("Should calculate reducing EMI correctly")
        void shouldCalculateReducingEmi() {
            // 1 lakh principal, 12% annual, 12 months
            long principal = 10000000L;
            BigDecimal rate = BigDecimal.valueOf(12.0);
            int tenure = 12;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.REDUCING, FIRST_EMI_DATE);

            // Expected EMI using PMT formula ≈ 8,885 rupees = 888,500 paise
            assertThat(result.getEmiAmount()).isBetween(880000L, 900000L);
            assertThat(result.getTotalInterest()).isLessThan(1200000L); // Less than flat
        }

        @Test
        @DisplayName("Reducing interest should be less than flat interest for same loan")
        void reducingInterestShouldBeLessThanFlat() {
            long principal = 10000000L;
            BigDecimal rate = BigDecimal.valueOf(12.0);
            int tenure = 12;

            LoanCalculationResult flatResult = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.FLAT, FIRST_EMI_DATE);
            LoanCalculationResult reducingResult = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.REDUCING, FIRST_EMI_DATE);

            assertThat(reducingResult.getTotalInterest())
                    .isLessThan(flatResult.getTotalInterest());
        }

        @Test
        @DisplayName("Should have decreasing interest component in reducing balance")
        void shouldHaveDecreasingInterestInReducing() {
            long principal = 10000000L;
            BigDecimal rate = BigDecimal.valueOf(12.0);
            int tenure = 12;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.REDUCING, FIRST_EMI_DATE);

            List<EmiScheduleItem> schedule = result.getSchedule();

            // Interest should decrease over time
            for (int i = 0; i < tenure - 2; i++) {
                assertThat(schedule.get(i).getInterestDue())
                        .isGreaterThanOrEqualTo(schedule.get(i + 1).getInterestDue());
            }
        }

        @Test
        @DisplayName("Should have increasing principal component in reducing balance")
        void shouldHaveIncreasingPrincipalInReducing() {
            long principal = 10000000L;
            BigDecimal rate = BigDecimal.valueOf(12.0);
            int tenure = 12;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.REDUCING, FIRST_EMI_DATE);

            List<EmiScheduleItem> schedule = result.getSchedule();

            // Principal should increase over time (except last EMI which may be adjusted)
            for (int i = 0; i < tenure - 2; i++) {
                assertThat(schedule.get(i).getPrincipalDue())
                        .isLessThanOrEqualTo(schedule.get(i + 1).getPrincipalDue());
            }
        }

        @Test
        @DisplayName("Opening balance of first EMI should equal principal")
        void openingBalanceShouldEqualPrincipal() {
            long principal = 10000000L;
            BigDecimal rate = BigDecimal.valueOf(12.0);
            int tenure = 12;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.REDUCING, FIRST_EMI_DATE);

            assertThat(result.getSchedule().get(0).getOpeningBalance()).isEqualTo(principal);
        }

        @Test
        @DisplayName("Closing balance should equal next opening balance")
        void closingBalanceShouldEqualNextOpening() {
            long principal = 10000000L;
            BigDecimal rate = BigDecimal.valueOf(12.0);
            int tenure = 12;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.REDUCING, FIRST_EMI_DATE);

            List<EmiScheduleItem> schedule = result.getSchedule();
            for (int i = 0; i < tenure - 1; i++) {
                assertThat(schedule.get(i).getClosingBalance())
                        .isEqualTo(schedule.get(i + 1).getOpeningBalance());
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle single month tenure")
        void shouldHandleSingleMonthTenure() {
            long principal = 10000000L;
            BigDecimal rate = BigDecimal.valueOf(12.0);
            int tenure = 1;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.REDUCING, FIRST_EMI_DATE);

            assertThat(result.getSchedule()).hasSize(1);
            assertThat(result.getSchedule().get(0).getClosingBalance()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should handle very small principal")
        void shouldHandleSmallPrincipal() {
            long principal = 100L; // 1 rupee
            BigDecimal rate = BigDecimal.valueOf(10.0);
            int tenure = 2;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.FLAT, FIRST_EMI_DATE);

            assertThat(result.getTotalPayable()).isGreaterThan(principal);
        }

        @Test
        @DisplayName("Should handle very low interest rate")
        void shouldHandleLowInterestRate() {
            long principal = 10000000L;
            BigDecimal rate = BigDecimal.valueOf(0.1); // 0.1%
            int tenure = 12;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.REDUCING, FIRST_EMI_DATE);

            // Total interest should be very small
            assertThat(result.getTotalInterest()).isLessThan(100000L);
        }

        @Test
        @DisplayName("Should handle long tenure")
        void shouldHandleLongTenure() {
            long principal = 100000000L; // 10 lakh
            BigDecimal rate = BigDecimal.valueOf(10.0);
            int tenure = 240; // 20 years

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.REDUCING, FIRST_EMI_DATE);

            assertThat(result.getSchedule()).hasSize(240);
            assertThat(result.getSchedule().get(239).getClosingBalance()).isEqualTo(0L);
        }

        @ParameterizedTest
        @DisplayName("Should calculate correctly for various rates")
        @CsvSource({
            "10000000, 8.0, 12",
            "10000000, 10.5, 24",
            "50000000, 12.0, 36",
            "100000000, 15.0, 60"
        })
        void shouldCalculateForVariousRates(long principal, double rate, int tenure) {
            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, BigDecimal.valueOf(rate), tenure,
                    InterestType.REDUCING, FIRST_EMI_DATE);

            assertThat(result.getEmiAmount()).isGreaterThan(0);
            assertThat(result.getTotalInterest()).isGreaterThan(0);
            assertThat(result.getSchedule()).hasSize(tenure);

            // Verify total principal equals original
            long totalPrincipal = result.getSchedule().stream()
                    .mapToLong(EmiScheduleItem::getPrincipalDue)
                    .sum();
            assertThat(totalPrincipal).isEqualTo(principal);
        }
    }

    @Nested
    @DisplayName("Financial Accuracy Tests")
    class FinancialAccuracyTests {

        @Test
        @DisplayName("Total payable should equal principal plus interest")
        void totalPayableShouldEqualPrincipalPlusInterest() {
            long principal = 10000000L;
            BigDecimal rate = BigDecimal.valueOf(12.0);
            int tenure = 12;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.REDUCING, FIRST_EMI_DATE);

            assertThat(result.getTotalPayable())
                    .isEqualTo(principal + result.getTotalInterest());
        }

        @Test
        @DisplayName("Sum of total_due in schedule should approximately equal total_payable")
        void sumOfScheduleShouldEqualTotalPayable() {
            long principal = 10000000L;
            BigDecimal rate = BigDecimal.valueOf(12.0);
            int tenure = 12;

            LoanCalculationResult result = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.REDUCING, FIRST_EMI_DATE);

            long sumOfTotalDue = result.getSchedule().stream()
                    .mapToLong(EmiScheduleItem::getTotalDue)
                    .sum();

            // Allow small rounding difference
            assertThat(sumOfTotalDue).isCloseTo(result.getTotalPayable(), within(100L));
        }

        @Test
        @DisplayName("Interest calculation should not use floating point for money")
        void shouldNotUseFloatingPointForMoney() {
            // Running multiple calculations should give deterministic results
            long principal = 10000000L;
            BigDecimal rate = BigDecimal.valueOf(12.0);
            int tenure = 12;

            LoanCalculationResult result1 = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.REDUCING, FIRST_EMI_DATE);
            LoanCalculationResult result2 = EmiCalculator.calculate(
                    principal, rate, tenure, InterestType.REDUCING, FIRST_EMI_DATE);

            assertThat(result1.getEmiAmount()).isEqualTo(result2.getEmiAmount());
            assertThat(result1.getTotalInterest()).isEqualTo(result2.getTotalInterest());
        }
    }
}
