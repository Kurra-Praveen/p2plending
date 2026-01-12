# Frontend Integration Guide - Advanced Flexible Loan Configuration

**Version:** 1.0
**Date:** January 2026
**Status:** Ready for Frontend Development

---

## Table of Contents

1. [Overview](#1-overview)
2. [Key Concepts](#2-key-concepts)
3. [API Endpoints](#3-api-endpoints)
4. [Data Types & Enums](#4-data-types--enums)
5. [UI Components Guide](#5-ui-components-guide)
6. [Workflow Diagrams](#6-workflow-diagrams)
7. [Validation Rules](#7-validation-rules)
8. [Error Handling](#8-error-handling)
9. [Best Practices](#9-best-practices)

---

## 1. Overview

### 1.1 Feature Summary

The Advanced Flexible Loan Configuration feature introduces:

- **Multiple Interest Rate Modes**: Annual %, Monthly %, Daily %, Fixed Rupee/Day, Fixed Rupee/Month
- **Repayment Frequencies**: Monthly and Weekly schedules
- **Configurable Charges**: Processing fees, insurance, documentation, custom charges
- **Interest Mode Switching**: Change interest calculation for future periods
- **Loan Preview**: Calculate EMI without creating a loan

### 1.2 Critical Frontend Rules

> **IMPORTANT: Frontend must NEVER calculate EMI, interest, or charges locally.**

All financial calculations are performed by the backend. The frontend should:
1. Collect user input
2. Send to backend for calculation
3. Display backend-provided results

This ensures:
- Financial accuracy (0 paise tolerance)
- Audit compliance
- Consistent behavior across platforms

---

## 2. Key Concepts

### 2.1 Monetary Values

All monetary values are in **paise** (1/100th of a Rupee):
- ₹1,00,000 = 10000000 paise
- ₹50,000.50 = 5000050 paise

**Display Conversion:**
```javascript
// Convert paise to rupees for display
const formatCurrency = (paise) => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2
  }).format(paise / 100);
};

// Convert rupees to paise for API
const toPaise = (rupees) => Math.round(rupees * 100);
```

### 2.2 Interest Rate Modes

| Mode | Description | Rate Example | When to Use |
|------|-------------|--------------|-------------|
| `ANNUAL_PERCENTAGE` | Annual interest rate | 12.5% per year | Standard loans |
| `MONTHLY_PERCENTAGE` | Monthly interest rate (not derived) | 1.5% per month | Microfinance |
| `DAILY_PERCENTAGE` | Daily interest rate | 0.05% per day | Short-term loans |
| `FIXED_RUPEE_PER_DAY` | Fixed amount per day (in paise) | ₹50/day = 5000 paise | Daily interest loans |
| `FIXED_RUPEE_PER_MONTH` | Fixed amount per month (in paise) | ₹500/month = 50000 paise | Fixed charge loans |

### 2.3 Loan Frequencies

| Frequency | Tenure Field | Description |
|-----------|--------------|-------------|
| `MONTHLY` | `tenureMonths` | EMI due every month |
| `WEEKLY` | `tenureUnits` | EMI due every week |

### 2.4 Interest Types

| Type | Description |
|------|-------------|
| `FLAT` | Interest calculated on original principal throughout |
| `REDUCING` | Interest calculated on outstanding balance (standard EMI) |

---

## 3. API Endpoints

### 3.1 Loan Preview (Calculate EMI)

**Endpoint:** `POST /api/v1/loans/preview`
**Purpose:** Calculate EMI and schedule without creating a loan
**Use Case:** Show potential EMI while user is configuring loan parameters

**Request:**
```json
{
  "principal": 10000000,           // Required: Amount in paise (₹1,00,000)
  "interestRate": 12.5,            // Required: Rate value
  "interestRateMode": "ANNUAL_PERCENTAGE",  // Required
  "interestType": "REDUCING",      // Required: FLAT or REDUCING
  "frequency": "MONTHLY",          // Required: MONTHLY or WEEKLY
  "tenureMonths": 12,              // Required for MONTHLY frequency
  "tenureUnits": null,             // Required for WEEKLY frequency
  "startDate": "2026-02-01",       // Optional: defaults to today
  "timezone": "Asia/Kolkata"       // Optional: defaults to Asia/Kolkata
}
```

**Response:**
```json
{
  "emiAmount": 888488,             // EMI in paise (₹8,884.88)
  "totalInterest": 661856,         // Total interest in paise
  "totalPayable": 10661856,        // Principal + Interest in paise
  "schedule": [
    {
      "installmentNo": 1,
      "dueDate": "2026-03-01",
      "periodStartDate": "2026-02-01",
      "periodEndDate": "2026-03-01",
      "principalDue": 784655,      // Principal component
      "interestDue": 103833,       // Interest component
      "totalDue": 888488,          // Total EMI
      "openingBalance": 10000000,
      "closingBalance": 9215345
    },
    // ... more installments
  ]
}
```

**UI Usage:**
- Call this endpoint whenever user changes any loan parameter
- Debounce calls (300ms recommended) to avoid excessive API calls
- Display results in real-time as user adjusts parameters

---

### 3.2 Get Loan Configuration

**Endpoint:** `GET /api/v1/loans/{loanId}/configuration`
**Purpose:** Get current configuration for an existing loan

**Response:**
```json
{
  "id": "uuid",
  "loanId": "uuid",
  "interestRateMode": "ANNUAL_PERCENTAGE",
  "interestRate": 12.5,
  "interestType": "REDUCING",
  "frequency": "MONTHLY",
  "tenureMonths": 12,
  "tenureUnits": null,
  "timezone": "Asia/Kolkata",
  "activeHistoryId": "uuid",
  "createdAt": "2026-01-15T10:30:00",
  "updatedAt": "2026-01-15T10:30:00"
}
```

---

### 3.3 Get Interest Configuration History

**Endpoint:** `GET /api/v1/loans/{loanId}/interest-history`
**Purpose:** Get audit trail of interest configuration changes

**Response:**
```json
[
  {
    "id": "uuid",
    "loanId": "uuid",
    "interestRateMode": "MONTHLY_PERCENTAGE",
    "interestRate": 1.5,
    "interestType": "REDUCING",
    "effectiveFrom": "2026-03-01",
    "effectiveTo": null,           // null = currently active
    "changedBy": "uuid",
    "changedByEmail": "lender@example.com",
    "changeReason": "Customer request for lower rate",
    "isActive": true,
    "createdAt": "2026-02-15T14:30:00"
  },
  {
    "id": "uuid",
    "loanId": "uuid",
    "interestRateMode": "ANNUAL_PERCENTAGE",
    "interestRate": 12.5,
    "interestType": "REDUCING",
    "effectiveFrom": "2026-01-15",
    "effectiveTo": "2026-02-28",
    "changedBy": "uuid",
    "changedByEmail": "lender@example.com",
    "changeReason": "Initial configuration",
    "isActive": false,
    "createdAt": "2026-01-15T10:30:00"
  }
]
```

---

### 3.4 Switch Interest Mode

**Endpoint:** `POST /api/v1/loans/{loanId}/switch-interest-mode`
**Purpose:** Change interest calculation mode for future periods
**Restrictions:** Only for ACTIVE loans, effective date must be in the future

**Request:**
```json
{
  "newMode": "MONTHLY_PERCENTAGE",
  "newRate": 1.2,
  "effectiveDate": "2026-03-01",   // Must be strictly in the future
  "reason": "Customer requested rate change"
}
```

**Response:** Returns updated `LoanConfigurationResponse`

**Error Cases:**
- `INVALID_STATE`: Loan is not ACTIVE
- `INVALID_EFFECTIVE_DATE`: Date is not in the future
- `RESTRICTED_COMBINATION`: WEEKLY + DAILY_PERCENTAGE + REDUCING not allowed

---

### 3.5 Charge Definitions

#### List Active Charge Definitions
**Endpoint:** `GET /api/v1/charge-definitions`

**Response:**
```json
[
  {
    "id": "uuid",
    "name": "Processing Fee",
    "code": "PROCESSING_FEE",
    "description": "One-time loan processing fee",
    "chargeType": "PROCESSING_FEE",
    "calculationType": "PERCENTAGE_OF_PRINCIPAL",
    "amount": null,
    "percentage": 1.5,             // 1.5% of principal
    "applicationTiming": "DISBURSEMENT",
    "isActive": true,
    "isMandatory": true,
    "createdBy": "uuid",
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-01T00:00:00"
  },
  {
    "id": "uuid",
    "name": "Documentation Fee",
    "code": "DOC_FEE",
    "description": "Document preparation charges",
    "chargeType": "DOCUMENTATION",
    "calculationType": "FIXED",
    "amount": 50000,               // ₹500 fixed
    "percentage": null,
    "applicationTiming": "DISBURSEMENT",
    "isActive": true,
    "isMandatory": false,
    "createdBy": "uuid",
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-01T00:00:00"
  }
]
```

#### Create Charge Definition (Admin Only)
**Endpoint:** `POST /api/v1/charge-definitions`

**Request:**
```json
{
  "name": "Insurance Premium",
  "code": "INSURANCE_PREMIUM",
  "description": "Loan protection insurance",
  "chargeType": "INSURANCE",
  "calculationType": "PERCENTAGE_OF_PRINCIPAL",
  "amount": null,
  "percentage": 0.5,
  "applicationTiming": "SPREAD_ACROSS_TENURE",
  "isMandatory": false
}
```

---

### 3.6 Loan Charges

#### Get Loan Charges
**Endpoint:** `GET /api/v1/loans/{loanId}/charges`

**Response:**
```json
[
  {
    "id": "uuid",
    "loanId": "uuid",
    "chargeDefinitionId": "uuid",
    "name": "Processing Fee",
    "chargeType": "PROCESSING_FEE",
    "amount": 150000,              // ₹1,500
    "amountPaid": 150000,
    "amountWaived": 0,
    "outstandingAmount": 0,
    "status": "PAID",
    "dueDate": "2026-01-15",
    "paidAt": "2026-01-15T12:00:00",
    "waivedAt": null,
    "waivedBy": null,
    "waiverReason": null,
    "createdAt": "2026-01-15T10:30:00"
  }
]
```

#### Get Outstanding Charges
**Endpoint:** `GET /api/v1/loans/{loanId}/charges/outstanding`

#### Waive Charge (Admin Only)
**Endpoint:** `POST /api/v1/charges/{chargeId}/waive`

**Request:**
```json
{
  "reason": "Goodwill waiver for long-term customer"
}
```

---

## 4. Data Types & Enums

### 4.1 Enums Reference

```typescript
// Interest Rate Mode
type InterestRateMode =
  | 'ANNUAL_PERCENTAGE'
  | 'MONTHLY_PERCENTAGE'
  | 'DAILY_PERCENTAGE'
  | 'FIXED_RUPEE_PER_DAY'
  | 'FIXED_RUPEE_PER_MONTH';

// Interest Type
type InterestType = 'FLAT' | 'REDUCING';

// Loan Frequency
type LoanFrequency = 'MONTHLY' | 'WEEKLY';

// Charge Type
type ChargeType =
  | 'PROCESSING_FEE'
  | 'INSURANCE'
  | 'DOCUMENTATION'
  | 'CUSTOM';

// Charge Calculation Type
type ChargeCalculationType =
  | 'FIXED'
  | 'PERCENTAGE_OF_PRINCIPAL'
  | 'PERCENTAGE_OF_DISBURSEMENT';

// Charge Application Timing
type ChargeApplicationTiming =
  | 'DISBURSEMENT'
  | 'FIRST_EMI'
  | 'SPREAD_ACROSS_TENURE'
  | 'CUSTOM_DATE';

// Charge Status
type ChargeStatus = 'PENDING' | 'PARTIAL' | 'PAID' | 'WAIVED';

// Loan Status (existing)
type LoanStatus = 'CREATED' | 'ACTIVE' | 'CLOSED' | 'DEFAULTED';
```

### 4.2 TypeScript Interfaces

```typescript
interface LoanPreviewRequest {
  principal: number;              // in paise
  interestRate: number;           // decimal value
  interestRateMode: InterestRateMode;
  interestType: InterestType;
  frequency: LoanFrequency;
  tenureMonths?: number;
  tenureUnits?: number;
  startDate?: string;             // ISO date
  timezone?: string;
}

interface LoanPreviewResponse {
  emiAmount: number;
  totalInterest: number;
  totalPayable: number;
  schedule: ScheduleItem[];
}

interface ScheduleItem {
  installmentNo: number;
  dueDate: string;
  periodStartDate: string;
  periodEndDate: string;
  principalDue: number;
  interestDue: number;
  totalDue: number;
  openingBalance: number;
  closingBalance: number;
}

interface LoanConfiguration {
  id: string;
  loanId: string;
  interestRateMode: InterestRateMode;
  interestRate: number;
  interestType: InterestType;
  frequency: LoanFrequency;
  tenureMonths?: number;
  tenureUnits?: number;
  timezone: string;
  activeHistoryId?: string;
  createdAt: string;
  updatedAt: string;
}

interface SwitchInterestModeRequest {
  newMode: InterestRateMode;
  newRate: number;
  effectiveDate: string;          // ISO date, must be future
  reason: string;
}

interface ChargeDefinition {
  id: string;
  name: string;
  code: string;
  description?: string;
  chargeType: ChargeType;
  calculationType: ChargeCalculationType;
  amount?: number;                // in paise, for FIXED
  percentage?: number;            // for percentage-based
  applicationTiming: ChargeApplicationTiming;
  isActive: boolean;
  isMandatory: boolean;
}

interface LoanCharge {
  id: string;
  loanId: string;
  name: string;
  chargeType: ChargeType;
  amount: number;
  amountPaid: number;
  amountWaived: number;
  outstandingAmount: number;
  status: ChargeStatus;
  dueDate?: string;
}
```

---

## 5. UI Components Guide

### 5.1 Loan Creation Form

#### Component: LoanConfigurationForm

**Fields:**

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| Borrower | Select | Yes | Existing borrower dropdown |
| Principal Amount | Currency Input | Yes | Min: ₹1, display as ₹ but send paise |
| Interest Rate Mode | Select | Yes | 5 options |
| Interest Rate | Number Input | Yes | Decimal, label changes based on mode |
| Interest Type | Radio/Select | Yes | FLAT or REDUCING |
| Frequency | Radio/Select | Yes | MONTHLY or WEEKLY |
| Tenure | Number Input | Yes | Label: "months" or "weeks" based on frequency |
| Timezone | Select | No | Default: Asia/Kolkata |
| Charges | Multi-select | No | From charge definitions |

**Dynamic Behavior:**

```javascript
// Interest Rate Label based on mode
const getRateLabel = (mode) => {
  switch (mode) {
    case 'ANNUAL_PERCENTAGE': return 'Annual Rate (%)';
    case 'MONTHLY_PERCENTAGE': return 'Monthly Rate (%)';
    case 'DAILY_PERCENTAGE': return 'Daily Rate (%)';
    case 'FIXED_RUPEE_PER_DAY': return 'Fixed Amount per Day (₹)';
    case 'FIXED_RUPEE_PER_MONTH': return 'Fixed Amount per Month (₹)';
  }
};

// Tenure Label based on frequency
const getTenureLabel = (frequency) => {
  return frequency === 'WEEKLY' ? 'Tenure (Weeks)' : 'Tenure (Months)';
};

// For FIXED_RUPEE modes, convert to paise
const getRateForApi = (mode, rate) => {
  if (mode === 'FIXED_RUPEE_PER_DAY' || mode === 'FIXED_RUPEE_PER_MONTH') {
    return rate * 100; // Convert rupees to paise
  }
  return rate;
};
```

### 5.2 EMI Preview Component

**Features:**
- Real-time EMI calculation as user types
- Debounced API calls (300ms)
- Loading state during calculation
- Display EMI breakdown

**Example Layout:**
```
┌─────────────────────────────────────────┐
│         LOAN PREVIEW                    │
├─────────────────────────────────────────┤
│  Monthly EMI:        ₹8,884.88          │
│  Total Interest:     ₹6,618.56          │
│  Total Payable:      ₹1,06,618.56       │
├─────────────────────────────────────────┤
│  [View Full Schedule]                   │
└─────────────────────────────────────────┘
```

### 5.3 Repayment Schedule Table

**Columns:**
| # | Due Date | Principal | Interest | Total EMI | Balance |
|---|----------|-----------|----------|-----------|---------|
| 1 | Mar 01, 2026 | ₹7,846.55 | ₹1,038.33 | ₹8,884.88 | ₹92,153.45 |
| 2 | Apr 01, 2026 | ₹7,928.15 | ₹956.73 | ₹8,884.88 | ₹84,225.30 |
| ... | ... | ... | ... | ... | ... |

### 5.4 Interest Mode Switch Form

**Use Case:** Lender wants to change interest rate for future EMIs

**Form Fields:**
- New Interest Mode (Select)
- New Rate (Number, label dynamic)
- Effective From (Date picker, min: tomorrow)
- Reason (Textarea, required)

**Validation:**
- Effective date must be after today
- Reason is mandatory
- Show warning for restricted combinations

### 5.5 Charges Management

#### Charge Selection (Loan Creation)
```
┌─────────────────────────────────────────┐
│  APPLICABLE CHARGES                     │
├─────────────────────────────────────────┤
│  ☑ Processing Fee (1.5%)    ₹1,500.00  │ [Mandatory]
│  ☐ Documentation Fee        ₹500.00    │
│  ☐ Insurance Premium (0.5%) ₹500.00    │
├─────────────────────────────────────────┤
│  Total Charges:             ₹2,500.00   │
└─────────────────────────────────────────┘
```

#### Charges View (Loan Details)
```
┌──────────────────────────────────────────────────────────┐
│  LOAN CHARGES                                            │
├──────────────────────────────────────────────────────────┤
│  Processing Fee    ₹1,500   PAID      Jan 15, 2026      │
│  Insurance         ₹500     PENDING   Due: Feb 15       │ [Waive]
└──────────────────────────────────────────────────────────┘
```

---

## 6. Workflow Diagrams

### 6.1 Loan Creation Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Enter      │     │   Preview    │     │   Create     │
│   Loan       │────▶│   EMI        │────▶│   Loan       │
│   Details    │     │   (API)      │     │   (API)      │
└──────────────┘     └──────────────┘     └──────────────┘
       │                    │                    │
       │              On every change            │
       │              with debounce              │
       ▼                    ▼                    ▼
  User Input          Show Preview          Loan Created
                      in Real-time          Status: CREATED
```

### 6.2 Interest Mode Switch Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   View Loan  │     │  Enter New   │     │   Confirm    │
│   Config     │────▶│  Mode/Rate   │────▶│   Switch     │
└──────────────┘     └──────────────┘     └──────────────┘
       │                    │                    │
       │              Validate:                  │
       │              - Future date              │
       │              - Valid combination        │
       ▼                    ▼                    ▼
  Current Config       Show Impact          History Updated
                       Preview              New Config Active
```

### 6.3 Payment Allocation Flow

```
Payment Received
       │
       ▼
┌──────────────┐
│  1. Charges  │ ◀── Oldest first
└──────────────┘
       │
       ▼
┌──────────────┐
│  2. Penalty  │ ◀── Oldest first
└──────────────┘
       │
       ▼
┌──────────────┐
│  3. Interest │ ◀── Oldest EMI first
└──────────────┘
       │
       ▼
┌──────────────┐
│  4. Principal│ ◀── Oldest EMI first
└──────────────┘
       │
       ▼
   Any Excess
       │
       ▼
┌──────────────┐
│   Reduce     │
│   Principal  │
└──────────────┘
```

---

## 7. Validation Rules

### 7.1 Loan Creation Validation

| Field | Rule | Error Message |
|-------|------|---------------|
| Principal | Min: 1 paise | "Principal must be positive" |
| Interest Rate | Min: 0.0001 | "Interest rate must be positive" |
| Tenure (Monthly) | 1-360 | "Tenure must be 1-360 months" |
| Tenure (Weekly) | 1-520 | "Tenure must be 1-520 weeks" |
| Frequency + Tenure | Must match | "Provide tenureMonths for MONTHLY or tenureUnits for WEEKLY" |

### 7.2 Interest Mode Switch Validation

| Rule | Error Code | Message |
|------|------------|---------|
| Loan must be ACTIVE | INVALID_STATE | "Interest mode can only be switched for ACTIVE loans" |
| Date must be future | INVALID_EFFECTIVE_DATE | "Effective date must be strictly in the future" |
| No WEEKLY + DAILY + REDUCING | RESTRICTED_COMBINATION | "This combination is restricted" |
| Reason required | validation | "Reason for change is required" |

### 7.3 Frontend Validation (Before API Call)

```javascript
const validateLoanPreview = (data) => {
  const errors = {};

  if (!data.principal || data.principal <= 0) {
    errors.principal = 'Principal must be positive';
  }

  if (!data.interestRate || data.interestRate <= 0) {
    errors.interestRate = 'Interest rate must be positive';
  }

  if (data.frequency === 'MONTHLY' && !data.tenureMonths) {
    errors.tenureMonths = 'Tenure in months is required';
  }

  if (data.frequency === 'WEEKLY' && !data.tenureUnits) {
    errors.tenureUnits = 'Tenure in weeks is required';
  }

  return errors;
};
```

---

## 8. Error Handling

### 8.1 Error Response Format

```json
{
  "timestamp": "2026-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "code": "INVALID_STATE",
  "message": "Loan can only be disbursed from CREATED state. Current state: ACTIVE",
  "path": "/api/v1/loans/uuid/disburse"
}
```

### 8.2 Common Error Codes

| Code | HTTP Status | Description | UI Action |
|------|-------------|-------------|-----------|
| `INVALID_STATE` | 400 | Operation not allowed in current state | Show state-specific message |
| `INVALID_EFFECTIVE_DATE` | 400 | Date validation failed | Highlight date field |
| `RESTRICTED_COMBINATION` | 400 | Invalid config combination | Show warning, suggest alternatives |
| `RESOURCE_NOT_FOUND` | 404 | Entity doesn't exist | Redirect to list |
| `DUPLICATE_REFERENCE` | 400 | Duplicate payment reference | Show specific field error |
| `AMOUNT_MISMATCH` | 400 | Disbursement amount incorrect | Show expected amount |

### 8.3 Error Display Patterns

```javascript
// Field-level errors
<TextField
  error={!!errors.principal}
  helperText={errors.principal}
  ...
/>

// Toast/Snackbar for API errors
showToast({
  type: 'error',
  message: error.message,
  duration: 5000
});

// Modal for critical errors
showModal({
  title: 'Cannot Complete Action',
  message: error.message,
  actions: [{ label: 'OK', onClick: closeModal }]
});
```

---

## 9. Best Practices

### 9.1 API Integration

```javascript
// Use debouncing for preview
import { debounce } from 'lodash';

const debouncedPreview = debounce(async (formData) => {
  setLoading(true);
  try {
    const response = await api.post('/api/v1/loans/preview', formData);
    setPreview(response.data);
  } catch (error) {
    setPreviewError(error.message);
  } finally {
    setLoading(false);
  }
}, 300);

// Call on form change
useEffect(() => {
  if (isFormValid(formData)) {
    debouncedPreview(formData);
  }
}, [formData]);
```

### 9.2 Currency Formatting

```javascript
// Constants
const PAISE_PER_RUPEE = 100;

// Utility functions
export const formatCurrency = (paise) => {
  if (paise == null) return '₹0.00';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(paise / PAISE_PER_RUPEE);
};

export const parseCurrency = (rupeeString) => {
  const cleaned = rupeeString.replace(/[₹,\s]/g, '');
  return Math.round(parseFloat(cleaned) * PAISE_PER_RUPEE);
};

// Usage
<span>{formatCurrency(loan.emiAmount)}</span>  // ₹8,884.88
```

### 9.3 Date Handling

```javascript
// Always use ISO format for API
const formatDateForApi = (date) => {
  return date.toISOString().split('T')[0]; // "2026-01-15"
};

// Display format
const formatDateForDisplay = (isoDate) => {
  return new Date(isoDate).toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  }); // "15 Jan 2026"
};
```

### 9.4 State Management

```javascript
// Recommended state structure for loan creation
const initialState = {
  form: {
    borrowerId: null,
    principal: '',
    interestRate: '',
    interestRateMode: 'ANNUAL_PERCENTAGE',
    interestType: 'REDUCING',
    frequency: 'MONTHLY',
    tenureMonths: '',
    tenureUnits: '',
    timezone: 'Asia/Kolkata',
    chargeDefinitionIds: []
  },
  preview: null,
  previewLoading: false,
  previewError: null,
  charges: [],
  chargesLoading: false,
  submitting: false,
  errors: {}
};
```

### 9.5 Accessibility

- Use proper ARIA labels for dynamic content
- Announce EMI preview updates to screen readers
- Ensure keyboard navigation for all forms
- Provide clear error messages

```jsx
<div
  role="status"
  aria-live="polite"
  aria-label="EMI Preview"
>
  {previewLoading ? 'Calculating...' : `Monthly EMI: ${formatCurrency(preview.emiAmount)}`}
</div>
```

---

## Appendix A: Quick Reference Card

### Endpoints Summary

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/loans/preview` | Calculate EMI preview |
| GET | `/api/v1/loans/{id}/configuration` | Get loan config |
| GET | `/api/v1/loans/{id}/interest-history` | Get config history |
| POST | `/api/v1/loans/{id}/switch-interest-mode` | Change interest mode |
| GET | `/api/v1/charge-definitions` | List charges |
| POST | `/api/v1/charge-definitions` | Create charge (Admin) |
| GET | `/api/v1/loans/{id}/charges` | Get loan charges |
| POST | `/api/v1/charges/{id}/waive` | Waive charge (Admin) |

### Headers Required

```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

---

**End of Document**
