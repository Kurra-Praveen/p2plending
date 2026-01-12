# 🏗️ Technical Architecture Document (TAD)
## Lender Loan Management System (LLMS)

---

## 1. Architecture Overview

The Lender Loan Management System (LLMS) is designed as a **secure, scalable, service-oriented system** that supports complete loan lifecycle management from a lender’s perspective. The architecture follows **modular monolith → microservices–ready principles**, enabling future scalability without early complexity.

**Architecture Goals**
- High data accuracy for financial calculations
- Strong auditability and traceability
- Horizontal scalability
- Secure handling of sensitive financial data
- Easy extensibility for future fintech integrations

---

## 2. High-Level Architecture

```
[ Web / Mobile Client ]
          │
          ▼
    API Gateway / BFF
          │
 ┌────────┼────────┬────────┬────────┐
 │ Auth   │ Loan   │Payment │Report  │
 │Service │Service │Service │Service │
 └────────┼────────┴────────┴────────┘
          │
   ┌──────┴─────────────┐
   │ Shared Infrastructure│
   │ DB • Cache • Queue  │
   └─────────────────────┘
```

---

## 3. Client Layer

### 3.1 Web Application
- Technology: React / Next.js
- Role-based dashboards (Lender, Admin, Auditor)
- Real-time loan & payment views
- Secure token-based authentication

### 3.2 Mobile Application (Optional Phase)
- Technology: Flutter / React Native
- Lender-only access
- Offline read support (future)

---

## 4. API Gateway / Backend-for-Frontend (BFF)

**Responsibilities**
- Authentication token validation
- Request routing to services
- Rate limiting & throttling
- Request/response transformation

**Security**
- JWT validation
- HTTPS only
- IP allow/deny lists

---

## 5. Core Backend Services

### 5.1 Authentication & Authorization Service

**Responsibilities**
- User authentication
- Role-based access control (RBAC)
- Token issuance and validation

**Key Components**
- User table
- Roles & permissions mapping
- Session & token store

---

### 5.2 Borrower Service

**Responsibilities**
- Borrower profile management
- KYC document metadata storage
- Risk score assignment

**Core Tables**
- borrowers
- borrower_addresses
- kyc_documents

---

### 5.3 Loan Service (Core Financial Engine)

**Responsibilities**
- Loan creation and lifecycle management
- EMI and amortization calculation
- Outstanding balance computation
- Loan state transitions

**Key Concepts**
- Flat vs Reducing interest
- EMI schedule immutability
- State machine–driven loan status

**Core Tables**
- loans
- loan_terms
- repayment_schedule
- loan_status_history

---

### 5.4 Payment Service

**Responsibilities**
- Payment recording and validation
- Payment allocation logic
- Reconciliation support

**Allocation Order**
1. Penalty
2. Interest
3. Principal

**Core Tables**
- payments
- payment_allocations
- penalties

---

### 5.5 Delinquency & Collections Service

**Responsibilities**
- Overdue detection
- Penalty calculation
- Default classification
- Collection notes tracking

**Scheduled Jobs**
- Daily overdue scanner
- Penalty accrual engine

---

### 5.6 Reporting & Analytics Service

**Responsibilities**
- Portfolio-level metrics
- Loan-level statements
- Exportable reports

**Data Strategy**
- Read replicas
- Pre-aggregated summary tables

---

### 5.7 Notification Service

**Responsibilities**
- Due reminders
- Overdue alerts
- Closure notifications

**Channels**
- Email
- SMS
- WhatsApp (future)

---

## 6. Data Layer Architecture

### 6.1 Primary Database
- PostgreSQL
- ACID-compliant transactions
- Strong referential integrity

### 6.2 Caching Layer
- Redis
- Used for:
  - Session caching
  - Frequently accessed loan summaries

### 6.3 Messaging / Queue (Optional Phase)
- Kafka / RabbitMQ
- Used for:
  - Notification triggers
  - Audit log streaming

---

## 7. Financial Data Integrity Design

- All monetary values stored in smallest currency unit (paise)
- Immutable financial ledgers
- No hard deletes (soft delete + audit)
- Versioned loan terms

---

## 8. Audit & Logging Architecture

### 8.1 Audit Logging
- Every critical action logged
- Who, what, when, before, after

**Audit Targets**
- Loan creation/modification
- Payment updates
- Status changes

### 8.2 Application Logging
- Structured logs (JSON)
- Centralized log aggregation

---

## 9. Security Architecture

### 9.1 Data Security
- AES-256 encryption at rest
- TLS 1.2+ in transit

### 9.2 Access Security
- RBAC
- Principle of least privilege

### 9.3 Compliance Readiness
- Audit trails
- Data retention policies
- Secure backups

---

## 10. Deployment Architecture

### 10.1 Containerization
- Docker for services
- Environment-based configs

### 10.2 Orchestration
- Kubernetes
- Auto-scaling enabled

### 10.3 Environments
- Dev
- QA
- Staging
- Production

---

## 11. Scalability & Performance

- Horizontal scaling of stateless services
- Read replicas for reporting
- Async processing for heavy jobs
- Connection pooling

---

## 12. Disaster Recovery & Backup

- Daily automated DB backups
- Point-in-time recovery
- Multi-zone deployment

---

## 13. Future Architecture Extensions

- Auto-debit (UPI/NACH)
- Credit bureau integration
- AI-based default prediction
- Multi-lender / investor pooling
- Event-driven microservices

---

## 14. Architecture Principles Summary

- Accuracy over speed for financial data
- Immutability for auditability
- Modular design for evolution
- Security by default

---

📌 **This architecture directly aligns with the PRD & FRD and is production-ready for fintech-grade systems.**
