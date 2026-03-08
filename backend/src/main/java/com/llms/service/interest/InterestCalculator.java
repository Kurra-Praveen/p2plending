package com.llms.service.interest;

import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Strategy interface for interest calculation.
 * Each implementation handles a specific InterestRateMode.
 */
public interface InterestCalculator {

    /**
     * Returns the interest rate mode this calculator handles.
     */
    InterestRateMode getMode();

    /**
     * Calculates the interest for a given period.
     *
     * @param principal        The principal amount in paise
     * @param rate             The interest rate (interpretation depends on mode)
     * @param interestType     FLAT or REDUCING
     * @param periodStartDate  Start date of the period
     * @param periodEndDate    End date of the period
     * @param timezone         The loan timezone
     * @return Interest amount in paise
     */
    long calculateInterest(
            long principal,
            BigDecimal rate,
            InterestType interestType,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            String timezone
    );

    /**
     * Calculates daily interest for a given day.
     *
     * @param principal  The principal amount in paise
     * @param rate       The interest rate
     * @param date       The date for which to calculate interest
     * @param timezone   The loan timezone
     * @return Daily interest amount in paise
     */
    long calculateDailyInterest(
            long principal,
            BigDecimal rate,
            LocalDate date,
            String timezone
    );

    /**
     * Normalizes the rate to an annual percentage equivalent.
     * Used for display and comparison purposes.
     *
     * @param rate       The rate in this mode's format
     * @param referenceDate A reference date for calculations needing calendar info
     * @return Equivalent annual percentage rate
     */
    BigDecimal normalizeToAnnualRate(BigDecimal rate, LocalDate referenceDate);

    /**
     * Converts an annual rate to this mode's rate format.
     *
     * @param annualRate     The annual percentage rate
     * @param referenceDate  A reference date for calculations needing calendar info
     * @return Rate in this mode's format
     */
    BigDecimal fromAnnualRate(BigDecimal annualRate, LocalDate referenceDate);
}
