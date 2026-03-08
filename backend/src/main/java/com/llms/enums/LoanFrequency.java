package com.llms.enums;

/**
 * Defines the repayment frequency for a loan.
 * Frequency is immutable after loan creation.
 */
public enum LoanFrequency {
    /**
     * Monthly repayment schedule.
     * Tenure is measured in months (tenure_months).
     */
    MONTHLY,

    /**
     * Weekly repayment schedule.
     * Tenure is measured in weeks (tenure_units).
     */
    WEEKLY
}
