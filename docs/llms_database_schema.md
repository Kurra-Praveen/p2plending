# 🗄️ Database Schema – Lender Loan Management System (LLMS)

---

## 1. Design Principles
- ACID-compliant transactions
- Immutable financial records
- Soft deletes (no hard deletes)
- All monetary values stored in **smallest currency unit (paise)**

---

## 2. Core Tables

### 2.1 users
Stores lender/admin users

| Column | Type | Description |
|------|------|------------|
| id | UUID (PK) | User identifier |
| name | VARCHAR | Full name |
| email | VARCHAR (unique) | Login email |
| password_hash | VARCHAR | Encrypted password |
| role | VARCHAR | LENDER / ADMIN / AUDITOR |
| status | VARCHAR | ACTIVE / INACTIVE |
| created_at | TIMESTAMP | Created time |

---

### 2.2 borrowers

| Column | Type | Description |
|------|------|------------|
| id | UUID (PK) | Borrower ID |
| full_name | VARCHAR | Borrower name |
| phone | VARCHAR | Contact number |
| email | VARCHAR | Email |
| address | TEXT | Address |
| risk_score | INT | Internal score |
| status | VARCHAR | ACTIVE / BLOCKED |
| created_at | TIMESTAMP | Created time |

---

### 2.3 loans

| Column | Type | Description |
|------|------|------------|
| id | UUID (PK) | Loan ID |
| borrower_id | UUID (FK) | Borrower reference |
| principal_amount | BIGINT | Principal (paise) |
| interest_rate | DECIMAL(5,2) | Annual interest % |
| interest_type | VARCHAR | FLAT / REDUCING |
| tenure_months | INT | Loan tenure |
| emi_amount | BIGINT | EMI amount |
| status | VARCHAR | CREATED / ACTIVE / CLOSED / DEFAULTED |
| disbursed_at | TIMESTAMP | Disbursement time |
| created_at | TIMESTAMP | Created time |

---

### 2.4 repayment_schedule

| Column | Type | Description |
|------|------|------------|
| id | UUID (PK) | Schedule ID |
| loan_id | UUID (FK) | Loan reference |
| emi_no | INT | EMI sequence |
| due_date | DATE | Due date |
| principal_due | BIGINT | Principal component |
| interest_due | BIGINT | Interest component |
| status | VARCHAR | PENDING / PAID / PARTIAL |

---

### 2.5 payments

| Column | Type | Description |
|------|------|------------|
| id | UUID (PK) | Payment ID |
| loan_id | UUID (FK) | Loan reference |
| amount_paid | BIGINT | Paid amount |
| payment_date | DATE | Date |
| mode | VARCHAR | CASH / UPI / BANK |
| reference | VARCHAR | Transaction reference |
| created_at | TIMESTAMP | Created time |

---

### 2.6 payment_allocations

| Column | Type | Description |
|------|------|------------|
| id | UUID (PK) | Allocation ID |
| payment_id | UUID (FK) | Payment reference |
| type | VARCHAR | PENALTY / INTEREST / PRINCIPAL |
| amount | BIGINT | Allocated amount |

---

### 2.7 penalties

| Column | Type | Description |
|------|------|------------|
| id | UUID (PK) | Penalty ID |
| loan_id | UUID (FK) | Loan reference |
| emi_no | INT | EMI number |
| amount | BIGINT | Penalty amount |
| status | VARCHAR | UNPAID / PAID |
| applied_at | TIMESTAMP | Applied time |

---

### 2.8 audit_logs

| Column | Type | Description |
|------|------|------------|
| id | UUID (PK) | Audit ID |
| entity | VARCHAR | Entity name |
| entity_id | UUID | Entity reference |
| action | VARCHAR | CREATE / UPDATE / DELETE |
| performed_by | UUID | User ID |
| before_state | JSONB | Previous state |
| after_state | JSONB | New state |
| performed_at | TIMESTAMP | Time |

---

## 3. Relationships

- borrowers 1 → N loans
- loans 1 → N repayment_schedule
- loans 1 → N payments
- payments 1 → N payment_allocations

---

📌 **This schema ensures financial correctness, traceability, and audit readiness.**

