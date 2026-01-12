# 🔌 API Contracts – Lender Loan Management System (LLMS)

---

## 1. API Standards
- RESTful APIs
- JSON request/response
- JWT-based authentication
- Versioned APIs (/api/v1)

---

## 2. Authentication APIs

### POST /api/v1/auth/login

**Request**
```json
{
  "email": "lender@mail.com",
  "password": "******"
}
```

**Response**
```json
{
  "token": "jwt-token",
  "role": "LENDER"
}
```

---

## 3. Borrower APIs

### POST /api/v1/borrowers

**Request**
```json
{
  "fullName": "Ravi Kumar",
  "phone": "9876543210",
  "email": "ravi@mail.com",
  "address": "Bangalore",
  "riskScore": 650
}
```

### GET /api/v1/borrowers/{id}

**Response**
```json
{
  "id": "uuid",
  "fullName": "Ravi Kumar",
  "status": "ACTIVE"
}
```

---

## 4. Loan APIs

### POST /api/v1/loans

**Request**
```json
{
  "borrowerId": "uuid",
  "principal": 10000000,
  "interestRate": 10.5,
  "interestType": "REDUCING",
  "tenureMonths": 12
}
```

### POST /api/v1/loans/{loanId}/disburse

```json
{
  "amount": 10000000,
  "mode": "BANK",
  "reference": "TXN123"
}
```

### GET /api/v1/loans/{loanId}

**Response**
```json
{
  "loanId": "uuid",
  "status": "ACTIVE",
  "outstanding": 8500000
}
```

---

## 5. Repayment APIs

### POST /api/v1/loans/{loanId}/payments

**Request**
```json
{
  "amount": 500000,
  "paymentDate": "2026-01-10",
  "mode": "UPI",
  "reference": "UPI9988"
}
```

---

## 6. Reporting APIs

### GET /api/v1/reports/portfolio

**Response**
```json
{
  "totalDisbursed": 500000000,
  "outstanding": 210000000,
  "activeLoans": 120,
  "defaultedLoans": 6
}
```

---

## 7. Error Handling (Common)

```json
{
  "errorCode": "INVALID_STATE",
  "message": "Loan is already closed"
}
```

---

## 8. Security Headers
- Authorization: Bearer <token>
- Content-Type: application/json

---

📌 **These API contracts directly align with the DB schema, PRD, and Technical Architecture.**
