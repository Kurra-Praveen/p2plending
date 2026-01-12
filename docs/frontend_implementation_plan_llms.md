# 🧱 Frontend Implementation Plan
## Lender Loan Management System (LLMS)

This document defines a **clear, backend-aligned, production-grade frontend implementation plan** for the Lender Loan Management System (LLMS).

The frontend **must strictly follow backend APIs and business logic**. All financial calculations are performed **only on the backend**.

---

## 1. Objectives

The frontend application must:
- Authenticate lenders securely using JWT
- Display backend-calculated financial data
- Provide workflows for borrowers, loans, payments, and reports
- Enforce role-based access
- Handle backend failures gracefully

---

## 2. Technology Stack

| Layer | Technology |
|-----|-----------|
| Framework | React + TypeScript |
| Build Tool | Vite |
| Routing | React Router v6 |
| API Client | Axios |
| State Management | Redux Toolkit |
| Forms | React Hook Form + Zod |
| UI | MUI or Tailwind CSS |
| Charts | Recharts |
| Testing | Jest, React Testing Library, Playwright |

---

## 3. Backend Alignment (Source of Truth)

The frontend must consume the following backend APIs:

- `POST /api/v1/auth/login`
- `GET /api/v1/borrowers`
- `POST /api/v1/borrowers`
- `POST /api/v1/loans`
- `POST /api/v1/loans/{id}/disburse`
- `POST /api/v1/loans/{id}/payments`
- `GET /api/v1/reports/*`

### Non-Negotiable Rules
- No frontend financial calculations
- No assumptions about database schema
- No API behavior invention

---

## 4. High-Level Frontend Architecture

```
src/
 ├── app/
 │   ├── store.ts
 │   ├── router.tsx
 │   └── authGuard.tsx
 ├── api/
 │   ├── axios.ts
 │   ├── auth.api.ts
 │   ├── borrower.api.ts
 │   ├── loan.api.ts
 │   └── report.api.ts
 ├── features/
 │   ├── auth/
 │   ├── dashboard/
 │   ├── borrowers/
 │   ├── loans/
 │   ├── payments/
 │   └── reports/
 ├── components/
 │   ├── layout/
 │   ├── tables/
 │   ├── forms/
 │   └── charts/
 ├── types/
 ├── utils/
 └── main.tsx
```

---

## 5. Authentication Flow

### Backend
- JWT issued via `/auth/login`
- JWT contains userId, role, expiry

### Frontend Responsibilities
1. Render login screen
2. Send credentials to backend
3. Store JWT securely (memory / session)
4. Attach JWT to all API calls
5. Protect routes using role-based guards

### Security Rules
- Do not store JWT in localStorage
- Logout on HTTP 401
- Show access denied on HTTP 403

---

## 6. API Communication Layer

### Axios Configuration

- Base URL via environment variable
- Authorization header injection
- Global error handling

### Error Handling Strategy

| Status | Behavior |
|------|----------|
| 401 | Logout + redirect to login |
| 403 | Show forbidden page |
| 400 | Show validation error |
| 500 | Show generic error message |

---

## 7. Feature Implementation Plan

### Phase 1: Dashboard

**Backend APIs**
- `GET /reports/portfolio`

**Frontend Features**
- Total disbursed amount
- Outstanding balance
- Active / closed / defaulted loans
- Monthly collection chart

---

### Phase 2: Borrower Management

**Backend APIs**
- `POST /borrowers`
- `GET /borrowers/{id}`

**Frontend Features**
- Borrower creation form
- Borrower profile page
- Borrower loan listing

---

### Phase 3: Loan Management

**Backend APIs**
- `POST /loans`
- `POST /loans/{id}/disburse`
- `GET /loans/{id}`

**Frontend Features**
- Loan creation wizard
- Loan disbursement screen
- Loan details view
- EMI schedule (read-only)

---

### Phase 4: Payments

**Backend APIs**
- `POST /loans/{id}/payments`

**Frontend Features**
- Record payment form
- Payment history table
- Outstanding balance view

---

### Phase 5: Reports & Delinquency

**Backend APIs**
- `GET /reports/overdue`
- `GET /reports/loan-statement`

**Frontend Features**
- Overdue loan list
- Loan statement view
- CSV / Excel export

---

## 8. State Management Strategy

| Data | Storage |
|----|--------|
| Auth token | Memory / session |
| User profile | Redux |
| Borrowers | Redux |
| Loans | Redux |
| Reports | Query-based |

---

## 9. Validation Strategy

- Frontend validates input format only
- Backend validates business rules
- Backend error messages displayed verbatim

---

## 10. Security Guidelines

- HTTPS only
- Role-based routing
- Backend is authority for all data
- No sensitive data logged

---

## 11. Testing Plan

| Test Type | Scope |
|--------|------|
| Unit | Components |
| Integration | API layer |
| E2E | Critical user flows |
| Security | Auth & routing |

---

## 12. Deployment & Configuration

### Environment Variables

```
VITE_API_BASE_URL=https://api.llms.com
```

### Environments
- Dev
- QA
- Production

---

## 13. Sprint Breakdown

| Sprint | Deliverables |
|------|-------------|
| 1 | Auth + Dashboard |
| 2 | Borrowers |
| 3 | Loans |
| 4 | Payments |
| 5 | Reports |
| 6 | Hardening & security |

---

## 14. Non-Negotiable Rules

- No frontend financial calculations
- Backend is single source of truth
- Fail fast on authentication errors
- Maintain audit transparency

---

## ✅ Final Outcome

A secure, scalable, lender-grade frontend that aligns perfectly with the backend architecture and business rules.

