package com.llms.enums;

/**
 * Defines how interest rates are interpreted and calculated.
 * Each mode has specific normalization rules for converting to daily rates.
 */
public enum InterestRateMode {
    /**
     * Annual percentage rate provided by lender.
     * Canonical rate - all other modes can be converted to/from this.
     * Daily rate = annualRate / actualDaysInYear (365 or 366)
     */
    ANNUAL_PERCENTAGE,

    /**
     * Monthly percentage rate provided explicitly.
     * NOT derived from annual rate - used as-is.
     * Daily rate = monthlyRate / daysInMonth
     */
    MONTHLY_PERCENTAGE,

    /**
     * Daily percentage rate provided explicitly.
     * Uses actual day count for interest calculation.
     */
    DAILY_PERCENTAGE,

    /**
     * Fixed rupee amount per calendar day.
     * No percentage math - direct amount in paise.
     */
    FIXED_RUPEE_PER_DAY,

    /**
     * Fixed rupee amount per calendar month.
     * Pro-rated for partial months.
     */
    FIXED_RUPEE_PER_MONTH
}
