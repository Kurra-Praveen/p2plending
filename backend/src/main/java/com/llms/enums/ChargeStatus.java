package com.llms.enums;

/**
 * Status of a charge applied to a loan.
 */
public enum ChargeStatus {
    /**
     * Charge is pending payment.
     */
    PENDING,

    /**
     * Charge is partially paid.
     */
    PARTIAL,

    /**
     * Charge is fully paid.
     */
    PAID,

    /**
     * Charge has been waived.
     */
    WAIVED
}
