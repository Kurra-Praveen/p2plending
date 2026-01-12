# 📊 Advanced Flexible Loan Configuration – Feature Analysis Document (Updated)

**Version:** 1.1
**Date:** January 2026
**Author:** System Architect
**Status:** UPDATED – Ready for Implementation Review

---

## 1. Executive Summary

### 1.1 Objective

Introduce **advanced, flexible loan configuration** capabilities to the LLMS platform while maintaining strict fintech correctness, backward compatibility, and auditability.

The platform will support:

* Weekly and monthly repayment frequencies
* Optional, configurable charges (processing fee, insurance, custom)
* Multiple interest calculation modes (annual %, monthly %, daily %, fixed rupee)
* Safe switching of interest calculation modes for **future periods only**

### 1.2 Non-Negotiable Constraints

* **No breaking changes** to existing loans
* **Immutability** of past financial records
* **All monetary values stored in paise** (integer / BigDecimal only)
* **Backend-only calculations**
* **Additive-only schema evolution**
* **Audit-ready by design**

---

## 2. Interest Rate Semantics & Normalization (NEW – MANDATORY)

To avoid ambiguity, **interest rates are strictly normalized and interpreted as follows**:

### 2.1 Canonical Definitions

| Mode                  | Meaning                                     | Notes                        |
| --------------------- | ------------------------------------------- | ---------------------------- |
| ANNUAL_PERCENTAGE     | Annual percentage rate provided by lender   | Canonical rate               |
| MONTHLY_PERCENTAGE    | Monthly percentage rate provided explicitly | NOT derived from annual      |
| DAILY_PERCENTAGE      | Daily percentage rate provided explicitly   | Uses actual day count        |
| FIXED_RUPEE_PER_DAY   | Fixed rupee amount per calendar day         | No percentage math           |
| FIXED_RUPEE_PER_MONTH | Fixed rupee amount per calendar month       | Pro-rated for partial months |

### 2.2 Normalization Rules

* Annual → Monthly: `annualRate / 12`
* Annual → Daily: `annualRate / actualDaysInYear (365 / 366)`
* Monthly → Daily: `monthlyRate / daysInMonth`
* Leap years use **actual calendar days**
* All conversions use BigDecimal with explicit scale and rounding

> **Rule:** Two engineers implementing this independently must arrive at identical results.

---

## 3. Configuration Ownership & Immutability (UPDATED)

### 3.1 Source of Truth

| Table                        | Responsibility                        |
| ---------------------------- | ------------------------------------- |
| loan_configurations          | Current active configuration snapshot |
| loan_interest_config_history | Immutable audit trail of all changes  |

### 3.2 Invariants

* Exactly **one active interest configuration** per loan
* History records are **append-only**
* loan_configurations must always reflect the active history record

```sql
CREATE UNIQUE INDEX ux_active_interest_config
ON loan_interest_config_history (loan_id)
WHERE effective_to IS NULL;
```

---

## 4. Loan Frequency & Tenure Rules

### 4.1 Supported Frequencies

* MONTHLY – tenure measured in months
* WEEKLY – tenure measured in weeks

### 4.2 Rules

* Frequency is **immutable after loan creation**
* WEEKLY loans must use `tenure_units` (weeks)
* Monthly tenure (`tenure_months`) is ignored for weekly loans

---

## 5. Charges & EMI Separation (UPDATED)

### 5.1 Core Rule

> **Charges are NEVER merged into principal or interest.**

### 5.2 Financial Breakdown

| Concept     | Definition                     |
| ----------- | ------------------------------ |
| EMI Total   | `principal_due + interest_due` |
| Cycle Total | `emi_total + charges_due`      |

Charges may align with EMI dates but remain **logically independent** for reporting, audit, and allocation.

---

## 6. Payment Allocation & Overpayment Policy (UPDATED)

### 6.1 Allocation Order

1. Charges (oldest first)
2. Penalties
3. Interest
4. Principal

### 6.2 Overpayment Policy

* Default behavior: reduce outstanding principal
* Overpayment **never auto-applies to future charges**
* Advance EMI settlement is out of scope (future enhancement)

> Overpayment behavior must be explicitly disclosed to lenders.

---

## 7. Interest Mode Switching (Clarified)

### 7.1 Rules

* Allowed only when loan status is ACTIVE
* Effective date must be **strictly in the future**
* Past EMIs remain immutable

### 7.2 Guardrails

* WEEKLY + DAILY_PERCENTAGE + REDUCING is **restricted**
* Requires explicit lender consent flag (Phase-1 safeguard)

---

## 8. Timezone & Date Handling (NEW)

* All financial calculations use **loan timezone** (default: Asia/Kolkata)
* No UTC boundary crossing for interest calculation
* Daily interest uses calendar day boundaries in loan timezone

---

## 9. Data Model Changes (Summary)

Changes remain **additive only**:

* loan_configurations
* loan_interest_config_history
* charge_definitions
* loan_charges
* charge_schedule

Existing tables gain nullable columns with defaults.

---

## 10. Backend Calculation Engine (Confirmed)

* Strategy pattern for interest calculators
* Dedicated weekly/monthly schedule generators
* Isolated charge calculator
* Immutable recalculation logic for future periods only

---

## 11. Frontend Alignment Rules (UPDATED)

Frontend MUST:

* Never calculate EMI, interest, or charges
* Display backend-provided previews only
* Treat backend as the single source of truth

Optional enhancement (recommended):

```
POST /api/v1/loans/preview
```

---

## 12. Migration & Rollback Safety

* Zero-downtime additive migration
* Backfill existing loans safely
* Clear point-of-no-return defined
* Rollback allowed only if no advanced loans exist

---

## 13. Testing & Validation Expectations

### Mandatory Test Coverage

* Leap year daily interest
* Weekly schedule rounding
* Interest mode switch immutability
* Charge + payment allocation correctness
* Overpayment accuracy (0 paise tolerance)

---

## 14. Compliance & Audit Readiness

The system guarantees:

* Full calculation traceability
* Explainable lender actions
* Clear borrower statements
* Dispute-defensible records

---

## 15. Final Recommendation

**Status:** ✅ APPROVE WITH CHANGES INCORPORATED

This document is now:

* Fintech-safe
* Auditor-friendly
* Implementation-ready
* Backward-compatible

---

**End of Document**
