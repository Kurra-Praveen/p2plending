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
 * Generates weekly repayment schedules.
 * Uses the appropriate interest calculator based on the interest rate mode.
 */
@Component
@RequiredArgsConstructor
public class WeeklyScheduleGenerator implements ScheduleGenerator {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final int DAYS_PER_WEEK = 7;

    private final InterestCalculatorFactory calculatorFactory;

    @Override
    public LoanFrequency getFrequency() {
        return LoanFrequency.WEEKLY;
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
        int tenure = request.getTenure(); // tenure in weeks
        LocalDate startDate = request.getStartDate();

        // Calculate total interest for the entire loan period
        LocalDate endDate = startDate.plusWeeks(tenure);
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

        long weeklyInterest = totalInterest / tenure;
        long weeklyPrincipal = principal / tenure;

        List<ScheduleItem> items = new ArrayList<>();
        long openingBalance = principal;

        for (int i = 1; i <= tenure; i++) {
            LocalDate periodStart = startDate.plusWeeks(i - 1);
            LocalDate periodEnd = startDate.plusWeeks(i);
            LocalDate dueDate = periodEnd;

            long principalDue = weeklyPrincipal;
            long interestDue = weeklyInterest;
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
        int tenure = request.getTenure(); // tenure in weeks
        LocalDate startDate = request.getStartDate();

        // For reducing balance, calculate weekly EMI using adapted PMT formula
        long emiAmount = calculateReducingEmi(request, calculator);

        List<ScheduleItem> items = new ArrayList<>();
        long openingBalance = principal;
        long totalInterest = 0;

        for (int i = 1; i <= tenure; i++) {
            LocalDate periodStart = startDate.plusWeeks(i - 1);
            LocalDate periodEnd = startDate.plusWeeks(i);
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
        // Convert rate to equivalent weekly rate for PMT calculation
        BigDecimal annualRate = calculator.normalizeToAnnualRate(
                request.getInterestRate(),
                request.getStartDate()
        );

        // Weekly rate = Annual rate / 52 / 100
        BigDecimal weeklyRate = annualRate
                .divide(BigDecimal.valueOf(5200), SCALE, ROUNDING);

        if (weeklyRate.compareTo(BigDecimal.ZERO) == 0) {
            return request.getPrincipal() / request.getTenure();
        }

        BigDecimal principalBD = BigDecimal.valueOf(request.getPrincipal());
        int tenure = request.getTenure();

        // PMT = P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal onePlusR = BigDecimal.ONE.add(weeklyRate);
        BigDecimal onePlusRPowerN = onePlusR.pow(tenure);

        BigDecimal numerator = principalBD.multiply(weeklyRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);

        BigDecimal emiBD = numerator.divide(denominator, SCALE, ROUNDING);
        return emiBD.setScale(0, ROUNDING).longValue();
    }
}
