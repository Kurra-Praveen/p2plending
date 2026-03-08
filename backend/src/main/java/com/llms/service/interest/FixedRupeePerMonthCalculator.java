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
 * Calculates interest as a fixed rupee amount per calendar month.
 * Pro-rated for partial months.
 */
@Component
public class FixedRupeePerMonthCalculator implements InterestCalculator {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    @Override
    public InterestRateMode getMode() {
        return InterestRateMode.FIXED_RUPEE_PER_MONTH;
    }

    @Override
    public long calculateInterest(
            long principal,
            BigDecimal fixedAmountPerMonth,
            InterestType interestType,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String timezone
    ) {
        if (fixedAmountPerMonth.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        long days = ChronoUnit.DAYS.between(periodStartDate, periodEndDate);
        if (days <= 0) {
            return 0L;
        }

        BigDecimal totalInterest = BigDecimal.ZERO;

        // Calculate interest for each day in the period, pro-rating the monthly amount
        LocalDate currentDate = periodStartDate;
        while (currentDate.isBefore(periodEndDate)) {
            int daysInMonth = YearMonth.from(currentDate).lengthOfMonth();
            BigDecimal dailyAmount = fixedAmountPerMonth
                    .divide(BigDecimal.valueOf(daysInMonth), SCALE, ROUNDING);
            totalInterest = totalInterest.add(dailyAmount);
            currentDate = currentDate.plusDays(1);
        }

        return totalInterest.setScale(0, ROUNDING).longValue();
    }

    @Override
    public long calculateDailyInterest(
            long principal,
            BigDecimal fixedAmountPerMonth,
            LocalDate date,
            String timezone
    ) {
        if (fixedAmountPerMonth.compareTo(BigDecimal.ZERO) <= 0) {
            return 0L;
        }

        int daysInMonth = YearMonth.from(date).lengthOfMonth();

        // Pro-rate the monthly amount to a daily amount
        BigDecimal dailyAmount = fixedAmountPerMonth
                .divide(BigDecimal.valueOf(daysInMonth), SCALE, ROUNDING);

        return dailyAmount.setScale(0, ROUNDING).longValue();
    }

    @Override
    public BigDecimal normalizeToAnnualRate(BigDecimal fixedAmountPerMonth, LocalDate referenceDate) {
        // Annualized fixed amount = monthly × 12
        return fixedAmountPerMonth.multiply(TWELVE);
    }

    @Override
    public BigDecimal fromAnnualRate(BigDecimal annualRate, LocalDate referenceDate) {
        // This mode doesn't derive from percentage rates
        // Return the annual amount divided by 12 as an approximation
        return annualRate.divide(TWELVE, SCALE, ROUNDING);
    }
}
