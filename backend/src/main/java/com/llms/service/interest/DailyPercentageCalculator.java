package com.llms.service.interest;

import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.ChronoUnit;

/**
 * Calculates interest based on daily percentage rate.
 * Uses actual day count for interest calculation.
 */
@Component
public class DailyPercentageCalculator implements InterestCalculator {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Override
    public InterestRateMode getMode() {
        return InterestRateMode.DAILY_PERCENTAGE;
    }

    @Override
    public long calculateInterest(
            long principal,
            BigDecimal dailyRate,
            InterestType interestType,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String timezone
    ) {
        if (principal <= 0 || dailyRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        long days = ChronoUnit.DAYS.between(periodStartDate, periodEndDate);
        if (days <= 0) {
            return 0L;
        }

        BigDecimal principalDec = BigDecimal.valueOf(principal);
        BigDecimal daysDec = BigDecimal.valueOf(days);

        // Daily rate already provided as percentage
        BigDecimal rate = dailyRate.divide(HUNDRED, SCALE, ROUNDING);

        // Interest = Principal × Daily rate × Days
        BigDecimal interest = principalDec.multiply(rate).multiply(daysDec);

        return interest.setScale(0, ROUNDING).longValue();
    }

    @Override
    public long calculateDailyInterest(
            long principal,
            BigDecimal dailyRate,
            LocalDate date,
            String timezone
    ) {
        if (principal <= 0 || dailyRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        BigDecimal principalDec = BigDecimal.valueOf(principal);

        // Daily rate as percentage
        BigDecimal rate = dailyRate.divide(HUNDRED, SCALE, ROUNDING);

        // Daily interest = Principal × Daily rate
        BigDecimal interest = principalDec.multiply(rate);

        return interest.setScale(0, ROUNDING).longValue();
    }

    @Override
    public BigDecimal normalizeToAnnualRate(BigDecimal dailyRate, LocalDate referenceDate) {
        // Annual = Daily × days in year
        int daysInYear = getDaysInYear(referenceDate);
        return dailyRate.multiply(BigDecimal.valueOf(daysInYear));
    }

    @Override
    public BigDecimal fromAnnualRate(BigDecimal annualRate, LocalDate referenceDate) {
        // Daily = Annual / days in year
        int daysInYear = getDaysInYear(referenceDate);
        return annualRate.divide(BigDecimal.valueOf(daysInYear), SCALE, ROUNDING);
    }

    private int getDaysInYear(LocalDate date) {
        return Year.of(date.getYear()).isLeap() ? 366 : 365;
    }
}
