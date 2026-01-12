package com.llms.enums;

/**
 * Defines the types of charges that can be applied to a loan.
 * Charges are separate from principal and interest.
 */
public enum ChargeType {
    /**
     * One-time processing fee charged at disbursement.
     */
    PROCESSING_FEE,

    /**
     * Insurance premium for loan protection.
     */
    INSURANCE,

    /**
     * Documentation and stamp duty charges.
     */
    DOCUMENTATION,

    /**
     * Custom charge defined by lender.
     */
    CUSTOM
}
