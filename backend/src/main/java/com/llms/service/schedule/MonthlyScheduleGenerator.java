package com.llms.service.schedule;

import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;
import com.llms.enums.LoanFrequency;
import com.llms.service.interest.InterestCalculator;
import com.llms.service.interest.InterestCalculatorFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates monthly repayment schedules.
 * Uses the appropriate interest calculator based on the interest rate mode.
 */
@Component
@RequiredArgsConstructor
public class MonthlyScheduleGenerator implements ScheduleGenerator {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final InterestCalculatorFactory calculatorFactory;

    @Override
    public LoanFrequency getFrequency() {
        return LoanFrequency.MONTHLY;
    }

    @Override
    public ScheduleResult generateSchedule(ScheduleRequest request) {
        if (request.getInterestType() == InterestType.FLAT) {
            return generateFlatSchedule(request);
        } else {
            return generateReducingSchedule(request);
        }
    }

    private ScheduleResult generateFlatSchedule(ScheduleRequest request) {
        InterestCalculator calculator = calculatorFactory.getCalculator(request.getInterestRateMode());

        long principal = request.getPrincipal();
        int tenure = request.getTenure();
        LocalDate startDate = request.getStartDate();

        // Calculate total interest for the entire loan period
        LocalDate endDate = startDate.plusMonths(tenure);
        long totalInterest = calculator.calculateInterest(
                principal,
                request.getInterestRate(),
                request.getInterestType(),
                startDate,
                endDate,
                request.getTimezone()
        );

        long totalPayable = principal + totalInterest;
        long emiAmount = totalPayable / tenure;
        long lastEmiAdjustment = totalPayable - (emiAmount * tenure);

        long monthlyInterest = totalInterest / tenure;
        long monthlyPrincipal = principal / tenure;

        List<ScheduleItem> items = new ArrayList<>();
        long openingBalance = principal;

        for (int i = 1; i <= tenure; i++) {
            LocalDate periodStart = startDate.plusMonths(i - 1);
            LocalDate periodEnd = startDate.plusMonths(i);
            LocalDate dueDate = periodEnd;

            long principalDue = monthlyPrincipal;
            long interestDue = monthlyInterest;
            long totalDue = emiAmount;

            if (i == tenure) {
                principalDue = openingBalance;
                totalDue = emiAmount + lastEmiAdjustment;
                interestDue = totalDue - principalDue;
            }

            long closingBalance = openingBalance - principalDue;

            items.add(ScheduleItem.builder()
                    .installmentNo(i)
                    .dueDate(dueDate)
                    .periodStartDate(periodStart)
                    .periodEndDate(periodEnd)
                    .principalDue(principalDue)
                    .interestDue(interestDue)
                    .totalDue(totalDue)
                    .openingBalance(openingBalance)
                    .closingBalance(closingBalance)
                    .build());

            openingBalance = closingBalance;
        }

        return ScheduleResult.builder()
                .emiAmount(emiAmount)
                .totalInterest(totalInterest)
                .totalPayable(totalPayable)
                .items(items)
                .build();
    }

    private ScheduleResult generateReducingSchedule(ScheduleRequest request) {
        InterestCalculator calculator = calculatorFactory.getCalculator(request.getInterestRateMode());

        long principal = request.getPrincipal();
        int tenure = request.getTenure();
        LocalDate startDate = request.getStartDate();

        // For reducing balance, we need to calculate EMI first
        // This uses the standard PMT formula adapted for the interest rate mode
        long emiAmount = calculateReducingEmi(request, calculator);

        List<ScheduleItem> items = new ArrayList<>();
        long openingBalance = principal;
        long totalInterest = 0;

        for (int i = 1; i <= tenure; i++) {
            LocalDate periodStart = startDate.plusMonths(i - 1);
            LocalDate periodEnd = startDate.plusMonths(i);
            LocalDate dueDate = periodEnd;

            // Interest for this period based on opening balance
            long interestDue = calculator.calculateInterest(
                    openingBalance,
                    request.getInterestRate(),
                    request.getInterestType(),
                    periodStart,
                    periodEnd,
                    request.getTimezone()
            );

            long principalDue;
            long totalDue;

            if (i == tenure) {
                principalDue = openingBalance;
                totalDue = principalDue + interestDue;
            } else {
                principalDue = emiAmount - interestDue;
                if (principalDue < 0) {
                    principalDue = 0;
                }
                totalDue = emiAmount;
            }

            long closingBalance = openingBalance - principalDue;
            if (closingBalance < 0) {
                closingBalance = 0;
            }
            totalInterest += interestDue;

            items.add(ScheduleItem.builder()
                    .installmentNo(i)
                    .dueDate(dueDate)
                    .periodStartDate(periodStart)
                    .periodEndDate(periodEnd)
                    .principalDue(principalDue)
                    .interestDue(interestDue)
                    .totalDue(totalDue)
                    .openingBalance(openingBalance)
                    .closingBalance(closingBalance)
                    .build());

            openingBalance = closingBalance;
        }

        long totalPayable = principal + totalInterest;

        return ScheduleResult.builder()
                .emiAmount(emiAmount)
                .totalInterest(totalInterest)
                .totalPayable(totalPayable)
                .items(items)
                .build();
    }

    private long calculateReducingEmi(ScheduleRequest request, InterestCalculator calculator) {
        // Convert rate to equivalent monthly rate for PMT calculation
        BigDecimal annualRate = calculator.normalizeToAnnualRate(
                request.getInterestRate(),
                request.getStartDate()
        );

        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(1200), SCALE, ROUNDING);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return request.getPrincipal() / request.getTenure();
        }

        BigDecimal principalBD = BigDecimal.valueOf(request.getPrincipal());
        int tenure = request.getTenure();

        // PMT = P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRPowerN = onePlusR.pow(tenure);

        BigDecimal numerator = principalBD.multiply(monthlyRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);

        BigDecimal emiBD = numerator.divide(denominator, SCALE, ROUNDING);
        return emiBD.setScale(0, ROUNDING).longValue();
    }
}
