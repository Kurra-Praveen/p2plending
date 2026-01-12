package com.llms.util;

import com.llms.enums.InterestType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * EMI Calculator using deterministic integer arithmetic.
 * All monetary values are in paise (smallest currency unit).
 *
 * Formulas:
 * - FLAT: EMI = (Principal + Total Interest) / Tenure
 *         Total Interest = Principal * Rate * Tenure / 12 / 100
 *
 * - REDUCING (PMT formula):
 *         EMI = P * r * (1+r)^n / ((1+r)^n - 1)
 *         where r = monthly rate = annual rate / 12 / 100
 *               n = number of months
 */
public class EmiCalculator {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Data
    @Builder
    public static class EmiScheduleItem {
        private int emiNo;
        private LocalDate dueDate;
        private long principalDue;
        private long interestDue;
        private long totalDue;
        private long openingBalance;
        private long closingBalance;
    }

    @Data
    @Builder
    public static class LoanCalculationResult {
        private long emiAmount;
        private long totalInterest;
        private long totalPayable;
        private List<EmiScheduleItem> schedule;
    }

    /**
     * Calculate loan EMI and amortization schedule.
     *
     * @param principal      Principal amount in paise
     * @param annualRate     Annual interest rate as percentage (e.g., 12.5 for 12.5%)
     * @param tenureMonths   Loan tenure in months
     * @param interestType   FLAT or REDUCING
     * @param firstEmiDate   Date of first EMI
     * @return LoanCalculationResult containing EMI, total interest, and schedule
     */
    public static LoanCalculationResult calculate(
            long principal,
            BigDecimal annualRate,
            int tenureMonths,
            InterestType interestType,
            LocalDate firstEmiDate) {

        if (interestType == InterestType.FLAT) {
            return calculateFlat(principal, annualRate, tenureMonths, firstEmiDate);
        } else {
            return calculateReducing(principal, annualRate, tenureMonths, firstEmiDate);
        }
    }

    /**
     * FLAT Interest Calculation:
     * Total Interest = Principal * Annual Rate * Tenure / 12 / 100
     * EMI = (Principal + Total Interest) / Tenure
     */
    private static LoanCalculationResult calculateFlat(
            long principal,
            BigDecimal annualRate,
            int tenureMonths,
            LocalDate firstEmiDate) {

        BigDecimal principalBD = BigDecimal.valueOf(principal);
        BigDecimal tenureBD = BigDecimal.valueOf(tenureMonths);

        // Total Interest = Principal * Rate * Tenure / 12 / 100
        BigDecimal totalInterestBD = principalBD
                .multiply(annualRate)
                .multiply(tenureBD)
                .divide(BigDecimal.valueOf(1200), SCALE, ROUNDING);

        long totalInterest = totalInterestBD.setScale(0, ROUNDING).longValue();
        long totalPayable = principal + totalInterest;

        // EMI = Total Payable / Tenure
        long emiAmount = totalPayable / tenureMonths;

        // Distribute remaining paise to last EMI
        long lastEmiAdjustment = totalPayable - (emiAmount * tenureMonths);

        // For flat rate, interest is constant per EMI
        long monthlyInterest = totalInterest / tenureMonths;
        long monthlyPrincipal = principal / tenureMonths;

        List<EmiScheduleItem> schedule = new ArrayList<>();
        long openingBalance = principal;

        for (int i = 1; i <= tenureMonths; i++) {
            long principalDue = monthlyPrincipal;
            long interestDue = monthlyInterest;
            long totalDue = emiAmount;

            // Adjust last EMI for rounding
            if (i == tenureMonths) {
                principalDue = openingBalance;
                totalDue = emiAmount + lastEmiAdjustment;
                interestDue = totalDue - principalDue;
            }

            long closingBalance = openingBalance - principalDue;

            schedule.add(EmiScheduleItem.builder()
                    .emiNo(i)
                    .dueDate(firstEmiDate.plusMonths(i - 1))
                    .principalDue(principalDue)
                    .interestDue(interestDue)
                    .totalDue(totalDue)
                    .openingBalance(openingBalance)
                    .closingBalance(closingBalance)
                    .build());

            openingBalance = closingBalance;
        }

        return LoanCalculationResult.builder()
                .emiAmount(emiAmount)
                .totalInterest(totalInterest)
                .totalPayable(totalPayable)
                .schedule(schedule)
                .build();
    }

    /**
     * REDUCING Interest Calculation (Standard PMT formula):
     * EMI = P * r * (1+r)^n / ((1+r)^n - 1)
     * where r = monthly rate, n = tenure months
     */
    private static LoanCalculationResult calculateReducing(
            long principal,
            BigDecimal annualRate,
            int tenureMonths,
            LocalDate firstEmiDate) {

        BigDecimal principalBD = BigDecimal.valueOf(principal);

        // Monthly rate = Annual Rate / 12 / 100
        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(1200), SCALE, ROUNDING);

        // (1 + r)^n
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowerN = onePlusR.pow(tenureMonths);

        // EMI = P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal numerator = principalBD.multiply(monthlyRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);

        BigDecimal emiBD = numerator.divide(denominator, SCALE, ROUNDING);
        long emiAmount = emiBD.setScale(0, ROUNDING).longValue();

        // Generate amortization schedule
        List<EmiScheduleItem> schedule = new ArrayList<>();
        long openingBalance = principal;
        long totalInterest = 0;

        for (int i = 1; i <= tenureMonths; i++) {
            // Interest for this month = Opening Balance * Monthly Rate
            BigDecimal interestBD = BigDecimal.valueOf(openingBalance)
                    .multiply(monthlyRate)
                    .setScale(0, ROUNDING);
            long interestDue = interestBD.longValue();

            long principalDue;
            long totalDue;

            if (i == tenureMonths) {
                // Last EMI: pay off remaining balance
                principalDue = openingBalance;
                totalDue = principalDue + interestDue;
            } else {
                principalDue = emiAmount - interestDue;
                totalDue = emiAmount;
            }

            long closingBalance = openingBalance - principalDue;
            totalInterest += interestDue;

            schedule.add(EmiScheduleItem.builder()
                    .emiNo(i)
                    .dueDate(firstEmiDate.plusMonths(i - 1))
                    .principalDue(principalDue)
                    .interestDue(interestDue)
                    .totalDue(totalDue)
                    .openingBalance(openingBalance)
                    .closingBalance(closingBalance)
                    .build());

            openingBalance = closingBalance;
        }

        long totalPayable = principal + totalInterest;

        return LoanCalculationResult.builder()
                .emiAmount(emiAmount)
                .totalInterest(totalInterest)
                .totalPayable(totalPayable)
                .schedule(schedule)
                .build();
    }
}
