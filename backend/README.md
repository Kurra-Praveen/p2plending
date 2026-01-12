# Lender Loan Management System (LLMS) - Backend

A comprehensive, production-ready backend system for managing the complete lifecycle of loans from a lender's perspective.

## Features

- **Authentication & Authorization**: JWT-based auth with role-based access (LENDER, ADMIN, AUDITOR)
- **Borrower Management**: Full CRUD operations with audit logging
- **Loan Management**: Create loans, auto-generate EMI schedules, track disbursements
- **Repayment Engine**: Payment recording with proper allocation (Penalty → Interest → Principal)
- **Delinquency & Penalty Engine**: Daily overdue detection, penalty accrual, auto-default classification
- **Reporting APIs**: Portfolio summary, loan statements, collections reports
- **Audit Logging**: Immutable logs for all critical operations

## Tech Stack

- **Framework**: Spring Boot 3.2
- **Language**: Java 17
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA / Hibernate
- **Auth**: JWT (jjwt)
- **Migrations**: Flyway
- **Build**: Maven

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+

## Setup Instructions

### 1. Database Setup

```sql
-- Create database
CREATE DATABASE llms;

-- Create user (optional, or use existing postgres user)
CREATE USER llms_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE llms TO llms_user;
```

### 2. Configure Environment Variables

Create a `.env` file or set environment variables:

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
export JWT_SECRET=your-256-bit-secret-key-for-jwt-signing-must-be-at-least-256-bits
```

### 3. Build the Application

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR directly:

```bash
java -jar target/lender-loan-management-system-1.0.0.jar
```

The server will start at `http://localhost:8080`

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login and get JWT token |

### Borrowers

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/borrowers` | Create borrower |
| GET | `/api/v1/borrowers/{id}` | Get borrower by ID |
| GET | `/api/v1/borrowers` | List all borrowers |
| PUT | `/api/v1/borrowers/{id}` | Update borrower |
| DELETE | `/api/v1/borrowers/{id}` | Soft delete borrower |

### Loans

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/loans` | Create loan |
| POST | `/api/v1/loans/{id}/disburse` | Disburse loan |
| GET | `/api/v1/loans/{id}` | Get loan details |
| GET | `/api/v1/loans` | List all loans |
| GET | `/api/v1/loans/{id}/schedule` | Get repayment schedule |
| POST | `/api/v1/loans/{id}/close` | Close loan |

### Payments

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/loans/{loanId}/payments` | Record payment |
| GET | `/api/v1/loans/{loanId}/payments` | Get loan payments |

### Reports

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/reports/portfolio` | Portfolio summary |
| GET | `/api/v1/reports/loans/{id}/statement` | Loan statement |
| GET | `/api/v1/reports/collections` | Collections summary |
| GET | `/api/v1/reports/overdue` | Overdue loans |

### Audit

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/audit/entity/{entity}/{entityId}` | Entity audit logs |
| GET | `/api/v1/audit/user/{userId}` | User activity logs |

## API Usage Examples

### Register a Lender

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Lender",
    "email": "john@lender.com",
    "password": "password123",
    "role": "LENDER"
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@lender.com",
    "password": "password123"
  }'
```

### Create Borrower

```bash
curl -X POST http://localhost:8080/api/v1/borrowers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "fullName": "Ravi Kumar",
    "phone": "9876543210",
    "email": "ravi@mail.com",
    "address": "Bangalore",
    "riskScore": 650
  }'
```

### Create Loan

```bash
curl -X POST http://localhost:8080/api/v1/loans \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "borrowerId": "<borrower-uuid>",
    "principal": 10000000,
    "interestRate": 12.5,
    "interestType": "REDUCING",
    "tenureMonths": 12
  }'
```

### Disburse Loan

```bash
curl -X POST http://localhost:8080/api/v1/loans/<loan-id>/disburse \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "amount": 10000000,
    "mode": "BANK",
    "reference": "TXN123456"
  }'
```

### Record Payment

```bash
curl -X POST http://localhost:8080/api/v1/loans/<loan-id>/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "amount": 500000,
    "paymentDate": "2026-01-10",
    "mode": "UPI",
    "reference": "UPI9988"
  }'
```

## Financial Calculations

### EMI Calculation

**Flat Interest:**
```
Total Interest = Principal × Rate × Tenure / 12 / 100
EMI = (Principal + Total Interest) / Tenure
```

**Reducing Balance (PMT Formula):**
```
EMI = P × r × (1+r)^n / ((1+r)^n - 1)
where:
  P = Principal
  r = Monthly Rate (Annual Rate / 12 / 100)
  n = Tenure in months
```

### Payment Allocation Order

1. **Penalties** (oldest first)
2. **Interest** (oldest EMI first)
3. **Principal** (oldest EMI first)

### Penalty Calculation

```
Penalty = Outstanding Amount × Daily Rate × Days Overdue / 100
```

## Configuration

Key configuration in `application.yml`:

```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration-ms: 86400000  # 24 hours

penalty:
  rate-per-day: 0.05      # 0.05% per day
  grace-period-days: 0
  max-dpd-for-default: 90 # Days past due for auto-default
```

## Database Migrations

Migrations are managed by Flyway and run automatically on startup.

Migration files are in: `src/main/resources/db/migration/`

## Project Structure

```
src/main/java/com/llms/
├── LlmsApplication.java          # Main application
├── config/                       # Configuration classes
│   └── SecurityConfig.java
├── controller/                   # REST controllers
│   ├── AuthController.java
│   ├── BorrowerController.java
│   ├── LoanController.java
│   ├── PaymentController.java
│   ├── ReportController.java
│   └── AuditController.java
├── dto/                          # Data transfer objects
│   ├── request/
│   └── response/
├── entity/                       # JPA entities
│   ├── User.java
│   ├── Borrower.java
│   ├── Loan.java
│   ├── RepaymentSchedule.java
│   ├── Payment.java
│   ├── PaymentAllocation.java
│   ├── Penalty.java
│   └── AuditLog.java
├── enums/                        # Enumerations
├── exception/                    # Exception handling
├── repository/                   # JPA repositories
├── scheduler/                    # Scheduled jobs
├── security/                     # Security components
├── service/                      # Business logic
│   ├── AuthService.java
│   ├── BorrowerService.java
│   ├── LoanService.java
│   ├── PaymentService.java
│   ├── DelinquencyService.java
│   ├── ReportingService.java
│   └── AuditService.java
└── util/                         # Utilities
    └── EmiCalculator.java
```

## Loan State Machine

```
CREATED → ACTIVE → CLOSED
                 ↘ DEFAULTED
```

- **CREATED**: Loan created, pending disbursement
- **ACTIVE**: Loan disbursed, repayments in progress
- **CLOSED**: All dues paid, loan completed
- **DEFAULTED**: Auto/manual default classification (90+ DPD)

## Security

- All endpoints (except auth) require JWT authentication
- Role-based access control (RBAC)
- Passwords hashed with BCrypt
- All money values stored in smallest unit (paise)
- No hard deletes (soft delete with audit)
- Immutable audit logs

## Scheduled Jobs

| Job | Schedule | Description |
|-----|----------|-------------|
| Daily Overdue Scanner | 1:00 AM | Detects overdue EMIs, applies penalties, marks defaults |

## Important Notes

- All monetary values are in **paise** (smallest currency unit)
- No floating-point arithmetic for money calculations
- Every state change is audited
- ACID transactions for all financial operations
- Idempotent payment recording (duplicate reference check)

## License

Proprietary - All rights reserved
