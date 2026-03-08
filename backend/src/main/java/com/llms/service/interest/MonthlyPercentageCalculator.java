package com.llms.service.interest;

import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Calculates interest based on monthly percentage rate.
 * The monthly rate is provided explicitly - NOT derived from annual.
 */
@Component
public class MonthlyPercentageCalculator implements InterestCalculator {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    @Override
    public InterestRateMode getMode() {
        return InterestRateMode.MONTHLY_PERCENTAGE;
    }

    @Override
    public long calculateInterest(
            long principal,
            BigDecimal monthlyRate,
            InterestType interestType,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String timezone
    ) {
        if (principal <= 0 || monthlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        long days = ChronoUnit.DAYS.between(periodStartDate, periodEndDate);
        if (days <= 0) {
            return 0L;
        }

        BigDecimal principalDec = BigDecimal.valueOf(principal);
        BigDecimal totalInterest = BigDecimal.ZERO;

        // Calculate interest for each day in the period
        LocalDate currentDate = periodStartDate;
        while (currentDate.isBefore(periodEndDate)) {
            int daysInMonth = YearMonth.from(currentDate).lengthOfMonth();
            BigDecimal dailyRate = monthlyRate.divide(BigDecimal.valueOf(daysInMonth), SCALE, ROUNDING)
                    .divide(HUNDRED, SCALE, ROUNDING);
            totalInterest = totalInterest.add(principalDec.multiply(dailyRate));
            currentDate = currentDate.plusDays(1);
        }

        return totalInterest.setScale(0, ROUNDING).longValue();
    }

    @Override
    public long calculateDailyInterest(
            long principal,
            BigDecimal monthlyRate,
            LocalDate date,
            String timezone
    ) {
        if (principal <= 0 || monthlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        int daysInMonth = YearMonth.from(date).lengthOfMonth();

        BigDecimal principalDec = BigDecimal.valueOf(principal);
        BigDecimal daysInMonthDec = BigDecimal.valueOf(daysInMonth);

        // Daily rate = Monthly rate / days in month / 100
        BigDecimal dailyRate = monthlyRate.divide(daysInMonthDec, SCALE, ROUNDING)
                .divide(HUNDRED, SCALE, ROUNDING);

        // Daily interest = Principal × Daily rate
        BigDecimal interest = principalDec.multiply(dailyRate);

        return interest.setScale(0, ROUNDING).longValue();
    }

    @Override
    public BigDecimal normalizeToAnnualRate(BigDecimal monthlyRate, LocalDate referenceDate) {
        // Annual = Monthly × 12
        return monthlyRate.multiply(TWELVE);
    }

    @Override
    public BigDecimal fromAnnualRate(BigDecimal annualRate, LocalDate referenceDate) {
        // Monthly = Annual / 12
        return annualRate.divide(TWELVE, SCALE, ROUNDING);
    }
}
