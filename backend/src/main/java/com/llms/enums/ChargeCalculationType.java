package com.llms.enums;

/**
 * Defines how a charge amount is calculated.
 */
public enum ChargeCalculationType {
    /**
     * Fixed amount in paise.
     */
    FIXED,

    /**
     * Percentage of the loan principal amount.
     */
    PERCENTAGE_OF_PRINCIPAL,

    /**
     * Percentage of the disbursement amount.
     */
    PERCENTAGE_OF_DISBURSEMENT
}
