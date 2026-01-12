# 📘 Product Requirements Document (PRD)
## Lender Loan Management System (LLMS)

---

## 1. Product Overview

### 1.1 Product Name
Lender Loan Management System (LLMS)

### 1.2 Purpose
The purpose of LLMS is to provide a centralized, reliable, and scalable platform for lenders to manage the complete lifecycle of loans—from borrower onboarding and loan disbursement to repayment tracking, delinquency handling, and loan closure.

### 1.3 Target Users
- Individual lenders
- Private financiers
- Small NBFCs
- P2P lenders (future scope)

---

## 2. Problem Statement

Lenders currently rely on manual tools such as Excel sheets or paper records to manage loans, which leads to:
- Inaccurate interest and EMI calculations
- Poor visibility into outstanding balances
- Difficulty tracking overdue and defaulted loans
- Lack of audit trails and compliance readiness

---

## 3. Goals & Success Metrics

### 3.1 Goals
- Centralized loan and borrower management
- Accurate and automated EMI calculations
- Real-time repayment and outstanding tracking
- Clear visibility into defaults and risk
- Scalable foundation for future fintech integrations

### 3.2 Success Metrics
- 100% traceability between loans and repayments
- Zero manual reconciliation dependency
- Accurate monthly and yearly financial reports
- Reduced time to identify defaulters

---

## 4. User Persona

### Primary Persona: Lender
- Creates and disburses loans
- Tracks repayments and defaults
- Reviews portfolio performance
- Ensures financial accuracy

---

## 5. In-Scope Features

### 5.1 Borrower Management
- Create and update borrower profiles
- Store KYC and identity details
- Assign internal risk scores

### 5.2 Loan Management
- Create loans with configurable terms
- Auto-generate repayment schedules
- Modify loan terms with audit logging

### 5.3 Disbursement Tracking
- Record disbursement transactions
- Support multiple payment modes
- Maintain transaction references

### 5.4 Repayment Tracking
- Track EMI, partial, and advance payments
- Automatic allocation of payment amounts
- Real-time outstanding balance updates

### 5.5 Delinquency Management
- Identify overdue loans
- Apply late penalties
- Mark loans as defaulted

### 5.6 Reporting & Analytics
- Active vs closed loans
- Monthly collections
- Outstanding principal
- ROI and default ratios

---

## 6. Out of Scope (Phase 1)
- Borrower-facing mobile application
- Credit bureau integration
- Auto-debit (NACH / UPI Autopay)
- Investor pooling

---

## 7. Assumptions
- Lender has full control over loan creation and disbursement
- Repayments may be recorded manually or via system integration
- Compliance requirements may vary by region

---

## 8. Risks & Dependencies
- Incorrect interest calculations impact trust
- Poor validation may cause financial mismatches
- Security breaches risk sensitive data exposure

---

## 9. Non-Functional Requirements
- High availability (99.9%)
- Secure data encryption at rest and in transit
- Full audit logging
- Horizontal scalability

---

# 📕 Functional Requirements Document (FRD)

---

## 1. Borrower Management Module

### 1.1 Create Borrower
**Inputs:**
- Name
- Contact details
- Address
- ID proof details

**System Behavior:**
- Validate mandatory fields
- Generate unique borrower ID
- Set borrower status as Active

---

### 1.2 Update Borrower
- Allow updates to borrower details except unique ID
- Log all changes in audit records

---

## 2. Loan Management Module

### 2.1 Create Loan
**Inputs:**
- Borrower ID
- Principal amount
- Interest rate
- Interest type (Flat / Reducing)
- Tenure
- EMI frequency
- Penalty configuration

**System Behavior:**
- Generate unique loan ID
- Calculate EMI schedule
- Set loan status as Created

---

### 2.2 Loan Disbursement
**Inputs:**
- Loan ID
- Disbursement date
- Amount
- Payment mode
- Transaction reference

**System Behavior:**
- Validate loan state
- Mark loan as Active
- Record disbursement entry

---

## 3. Repayment Module

### 3.1 Record Repayment
**Inputs:**
- Loan ID
- Payment date
- Amount paid
- Payment mode

**Allocation Logic:**
1. Penalty
2. Interest
3. Principal

**System Behavior:**
- Update outstanding balance
- Update EMI status
- Generate payment receipt reference

---

### 3.2 Partial & Advance Payments
- Allow partial payments
- Mark EMI as Partially Paid
- Carry forward remaining balance

---

## 4. Delinquency & Penalty Module

### 4.1 Overdue Detection
- Daily system job checks EMI due dates
- Identify overdue loans

### 4.2 Penalty Application
- Apply penalty based on configured rules
- Store penalty as a separate financial entry

---

## 5. Loan Closure Module

### 5.1 Normal Closure
- All EMIs paid
- Outstanding balance equals zero

### 5.2 Early Closure
- Calculate foreclosure amount
- Apply discounts if configured

---

## 6. Reporting Module

### 6.1 Financial Reports
- Loan-wise statements
- Monthly collection reports
- Outstanding principal reports

### 6.2 Risk Reports
- Overdue loans
- Defaulted loans
- Borrower risk distribution

---

## 7. Audit & Logging Module

### 7.1 Audit Coverage
- Borrower updates
- Loan creation and modification
- Payment recording
- Status transitions

### 7.2 Data Retention
- Immutable audit records
- Timestamped logs

---

## 8. Access Control

| Role | Permissions |
|-----|------------|
| Lender | Full access |
| Auditor | Read-only |
| Admin | Configuration management |

---

## 9. Error Handling
- Invalid loan state actions are rejected
- Duplicate transactions are blocked
- Overpayments trigger adjustment workflow

---

## 10. Performance Requirements
- EMI calculation < 500 ms
- Report generation < 3 seconds
- Support concurrent users

---

## 11. Security & Compliance
- Encrypt sensitive borrower data
- Secure authentication and authorization
- Activity monitoring and alerts

---

## 12. Future Enhancements
- Borrower mobile app
- Auto-debit integrations
- Credit score integration
- AI-based risk prediction

