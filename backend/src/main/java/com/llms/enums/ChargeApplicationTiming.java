package com.llms.enums;

/**
 * Defines when a charge is applied to a loan.
 */
public enum ChargeApplicationTiming {
    /**
     * Charge is due at loan disbursement.
     */
    DISBURSEMENT,

    /**
     * Charge is added to the first EMI.
     */
    FIRST_EMI,

    /**
     * Charge is spread equally across all EMIs.
     */
    SPREAD_ACROSS_TENURE,

    /**
     * Charge is due on a custom date.
     */
    CUSTOM_DATE
}
