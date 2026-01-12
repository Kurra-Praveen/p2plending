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
 * Calculates interest as a fixed rupee amount per calendar day.
 * No percentage math - the rate represents the actual amount in paise per day.
 */
@Component
public class FixedRupeePerDayCalculator implements InterestCalculator {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Override
    public InterestRateMode getMode() {
        return InterestRateMode.FIXED_RUPEE_PER_DAY;
    }

    @Override
    public long calculateInterest(
            long principal,
            BigDecimal fixedAmountPerDay,
            InterestType interestType,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String timezone
    ) {
        // Fixed amount is independent of principal (for FLAT)
        // For REDUCING, we still use the fixed amount as-is
        if (fixedAmountPerDay.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        long days = ChronoUnit.DAYS.between(periodStartDate, periodEndDate);
        if (days <= 0) {
            return 0L;
        }

        // Interest = Fixed amount per day × Days
        BigDecimal interest = fixedAmountPerDay.multiply(BigDecimal.valueOf(days));

        return interest.setScale(0, ROUNDING).longValue();
    }

    @Override
    public long calculateDailyInterest(
            long principal,
            BigDecimal fixedAmountPerDay,
            LocalDate date,
            String timezone
    ) {
        if (fixedAmountPerDay.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        // Simply return the fixed daily amount
        return fixedAmountPerDay.setScale(0, ROUNDING).longValue();
    }

    @Override
    public BigDecimal normalizeToAnnualRate(BigDecimal fixedAmountPerDay, LocalDate referenceDate) {
        // Cannot meaningfully convert fixed rupee to percentage without knowing principal
        // Return the annualized fixed amount (amount × days in year)
        int daysInYear = getDaysInYear(referenceDate);
        return fixedAmountPerDay.multiply(BigDecimal.valueOf(daysInYear));
    }

    @Override
    public BigDecimal fromAnnualRate(BigDecimal annualRate, LocalDate referenceDate) {
        // This mode doesn't derive from percentage rates
        // Return the rate divided by days in year as an approximation
        int daysInYear = getDaysInYear(referenceDate);
        return annualRate.divide(BigDecimal.valueOf(daysInYear), SCALE, ROUNDING);
    }

    private int getDaysInYear(LocalDate date) {
        return Year.of(date.getYear()).isLeap() ? 366 : 365;
    }
}
