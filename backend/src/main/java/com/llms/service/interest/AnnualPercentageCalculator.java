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
 * Calculates interest based on annual percentage rate.
 * This is the canonical rate format - all other modes can convert to/from this.
 */
@Component
public class AnnualPercentageCalculator implements InterestCalculator {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    @Override
    public InterestRateMode getMode() {
        return InterestRateMode.ANNUAL_PERCENTAGE;
    }

    @Override
    public long calculateInterest(
            long principal,
            BigDecimal annualRate,
            InterestType interestType,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String timezone
    ) {
        if (principal <= 0 || annualRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        long days = ChronoUnit.DAYS.between(periodStartDate, periodEndDate);
        if (days <= 0) {
            return 0L;
        }

        // For period-based calculation, use actual days in the year
        int daysInYear = getDaysInYear(periodStartDate);

        BigDecimal principalDec = BigDecimal.valueOf(principal);
        BigDecimal daysDec = BigDecimal.valueOf(days);
        BigDecimal daysInYearDec = BigDecimal.valueOf(daysInYear);

        // Daily rate = Annual rate / days in year / 100
        BigDecimal dailyRate = annualRate.divide(daysInYearDec, SCALE, ROUNDING)
                .divide(HUNDRED, SCALE, ROUNDING);

        // Interest = Principal × Daily rate × Days
        BigDecimal interest = principalDec.multiply(dailyRate).multiply(daysDec);

        return interest.setScale(0, ROUNDING).longValue();
    }

    @Override
    public long calculateDailyInterest(
            long principal,
            BigDecimal annualRate,
            LocalDate date,
            String timezone
    ) {
        if (principal <= 0 || annualRate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        int daysInYear = getDaysInYear(date);

        BigDecimal principalDec = BigDecimal.valueOf(principal);
        BigDecimal daysInYearDec = BigDecimal.valueOf(daysInYear);

        // Daily rate = Annual rate / days in year / 100
        BigDecimal dailyRate = annualRate.divide(daysInYearDec, SCALE, ROUNDING)
                .divide(HUNDRED, SCALE, ROUNDING);

        // Daily interest = Principal × Daily rate
        BigDecimal interest = principalDec.multiply(dailyRate);

        return interest.setScale(0, ROUNDING).longValue();
    }

    @Override
    public BigDecimal normalizeToAnnualRate(BigDecimal rate, LocalDate referenceDate) {
        // Already annual rate
        return rate;
    }

    @Override
    public BigDecimal fromAnnualRate(BigDecimal annualRate, LocalDate referenceDate) {
        // Already annual rate
        return annualRate;
    }

    private int getDaysInYear(LocalDate date) {
        return Year.of(date.getYear()).isLeap() ? 366 : 365;
    }
}
