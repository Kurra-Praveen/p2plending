# API Documentation

Base URL: `/api/v1`

## Common Concepts

### Authentication
Most endpoints require authentication. Include the JWT token obtained from `/auth/login` or `/auth/register` in the `Authorization` header of your requests.

**Header Format:**
`Authorization: Bearer <your_jwt_token>`

### Pagination & Sorting
Endpoints that return a list of resources (marked with `Page<...>`) support pagination and sorting via query parameters.

*   `page`: Page number (0-indexed, default: 0)
*   `size`: Number of items per page (default: 20)
*   `sort`: Sorting criteria in the format `property,asc|desc` (e.g., `createdAt,desc`). Can be used multiple times.

**Example Request:**
`GET /api/v1/borrowers?page=1&size=10&sort=createdAt,desc`

**Page Response Structure:**
```json
{
  "content": [ ... ],           // The list of items
  "pageable": {
    "pageNumber": 1,
    "pageSize": 10,
    // ... other pagination details
  },
  "totalElements": 100,         // Total number of items across all pages
  "totalPages": 10,             // Total number of pages
  "last": false,                // Is this the last page?
  "first": false,               // Is this the first page?
  "empty": false                // Is the result empty?
}
```

## Authentication

### Register User
*   **Endpoint:** `/auth/register`
*   **Method:** `POST`
*   **Access:** Public
*   **Request Body:**
    ```json
    {
      "name": "John Doe",           // Required
      "email": "john@example.com",  // Required, Valid Email
      "password": "password123",    // Required, Min 8 chars
      "role": "LENDER"              // Required, Enum: [LENDER, ADMIN, AUDITOR]
    }
    ```
*   **Response:**
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "role": "LENDER",
      "name": "John Doe"
    }
    ```

### Login
*   **Endpoint:** `/auth/login`
*   **Method:** `POST`
*   **Access:** Public
*   **Request Body:**
    ```json
    {
      "email": "john@example.com",  // Required, Valid Email
      "password": "password123"     // Required
    }
    ```
*   **Response:**
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "role": "LENDER",
      "name": "John Doe"
    }
    ```

---

## Borrowers

### Create Borrower
*   **Endpoint:** `/borrowers`
*   **Method:** `POST`
*   **Access:** `LENDER`, `ADMIN`
*   **Request Body:**
    ```json
    {
      "fullName": "Alice Smith",        // Required
      "phone": "+1234567890",           // Required
      "email": "alice@example.com",     // Optional, Valid Email
      "address": "123 Main St",         // Optional
      "riskScore": 750                  // Optional, Min 0
    }
    ```
*   **Response:**
    ```json
    {
      "id": "a1b2c3d4-...",
      "fullName": "Alice Smith",
      "phone": "+1234567890",
      "email": "alice@example.com",
      "address": "123 Main St",
      "riskScore": 750,
      "status": "ACTIVE",
      "createdAt": "2023-10-25T10:00:00"
    }
    ```

### Get Borrower
*   **Endpoint:** `/borrowers/{id}`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Response:**
    ```json
    {
      "id": "a1b2c3d4-...",
      "fullName": "Alice Smith",
      "phone": "+1234567890",
      "email": "alice@example.com",
      "address": "123 Main St",
      "riskScore": 750,
      "status": "ACTIVE",
      "createdAt": "2023-10-25T10:00:00"
    }
    ```

### Get All Borrowers
*   **Endpoint:** `/borrowers`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Query Params:** `page`, `size`, `sort`
*   **Response:**
    ```json
    {
      "content": [
        {
          "id": "a1b2c3d4-...",
          "fullName": "Alice Smith",
          "phone": "+1234567890",
          "email": "alice@example.com",
          "address": "123 Main St",
          "riskScore": 750,
          "status": "ACTIVE",
          "createdAt": "2023-10-25T10:00:00"
        }
      ],
      "pageable": { ... },
      "totalElements": 100,
      "totalPages": 10
    }
    ```

### Get Borrowers by Status
*   **Endpoint:** `/borrowers/status/{status}`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Path Params:** `status` (Enum: `ACTIVE`, `BLOCKED`)
*   **Query Params:** `page`, `size`, `sort`
*   **Response:**
    ```json
    {
      "content": [
        {
          "id": "a1b2c3d4-...",
          "fullName": "Alice Smith",
          "phone": "+1234567890",
          "email": "alice@example.com",
          "address": "123 Main St",
          "riskScore": 750,
          "status": "ACTIVE",
          "createdAt": "2023-10-25T10:00:00"
        }
      ],
      "pageable": { ... },
      "totalElements": 50,
      "totalPages": 5
    }
    ```

### Update Borrower
*   **Endpoint:** `/borrowers/{id}`
*   **Method:** `PUT`
*   **Access:** `LENDER`, `ADMIN`
*   **Request Body:**
    ```json
    {
      "fullName": "Alice Smith-Jones",  // Optional
      "email": "newemail@example.com",  // Optional, Valid Email
      "address": "456 New St",          // Optional
      "riskScore": 800                  // Optional, Min 0
    }
    ```
*   **Response:**
    ```json
    {
      "id": "a1b2c3d4-...",
      "fullName": "Alice Smith-Jones",
      "phone": "+1234567890",
      "email": "newemail@example.com",
      "address": "456 New St",
      "riskScore": 800,
      "status": "ACTIVE",
      "createdAt": "2023-10-25T10:00:00"
    }
    ```

### Block Borrower
*   **Endpoint:** `/borrowers/{id}/block`
*   **Method:** `POST`
*   **Access:** `LENDER`, `ADMIN`
*   **Response:** `204 No Content`

### Unblock Borrower
*   **Endpoint:** `/borrowers/{id}/unblock`
*   **Method:** `POST`
*   **Access:** `LENDER`, `ADMIN`
*   **Response:** `204 No Content`

### Delete Borrower
*   **Endpoint:** `/borrowers/{id}`
*   **Method:** `DELETE`
*   **Access:** `ADMIN`
*   **Response:** `204 No Content`

---

## Loans

### Create Loan
*   **Endpoint:** `/loans`
*   **Method:** `POST`
*   **Access:** `LENDER`, `ADMIN`
*   **Request Body:**
    ```json
    {
      "borrowerId": "uuid-string",      // Required
      "principal": 10000,               // Required, Min 1
      "interestRate": 12.5,             // Required, Min 0.01, Max 100.00
      "interestType": "FLAT",           // Required, Enum: [FLAT, REDUCING]
      "tenureMonths": 12                // Required, Min 1, Max 360
    }
    ```
*   **Response:**
    ```json
    {
      "loanId": "b2c3d4e5-...",
      "borrowerId": "a1b2c3d4-...",
      "borrowerName": "Alice Smith",
      "principal": 10000,
      "interestRate": 12.5,
      "interestType": "FLAT",
      "tenureMonths": 12,
      "emiAmount": 938,
      "totalInterest": 1250,
      "totalPayable": 11250,
      "outstandingPrincipal": 10000,
      "outstandingInterest": 0,
      "outstandingPenalty": 0,
      "totalOutstanding": 10000,
      "status": "CREATED",
      "createdAt": "2023-10-25T10:00:00"
    }
    ```

### Disburse Loan
*   **Endpoint:** `/loans/{loanId}/disburse`
*   **Method:** `POST`
*   **Access:** `LENDER`, `ADMIN`
*   **Request Body:**
    ```json
    {
      "amount": 10000,                  // Required, Min 1
      "mode": "BANK",                   // Required, Enum: [CASH, UPI, BANK, CHEQUE]
      "reference": "TXN123456"          // Optional
    }
    ```
*   **Response:**
    ```json
    {
      "loanId": "b2c3d4e5-...",
      "status": "ACTIVE",
      "disbursedAt": "2023-10-25T10:05:00",
      // ... other loan fields
    }
    ```

### Get Loan
*   **Endpoint:** `/loans/{id}`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Response:**
    ```json
    {
      "loanId": "b2c3d4e5-...",
      "borrowerId": "a1b2c3d4-...",
      "borrowerName": "Alice Smith",
      "principal": 10000,
      "interestRate": 12.5,
      "interestType": "FLAT",
      "tenureMonths": 12,
      "emiAmount": 938,
      "totalInterest": 1250,
      "totalPayable": 11250,
      "outstandingPrincipal": 9500,
      "outstandingInterest": 100,
      "outstandingPenalty": 0,
      "totalOutstanding": 9600,
      "status": "ACTIVE",
      "disbursedAt": "2023-10-25T10:05:00",
      "createdAt": "2023-10-25T10:00:00"
    }
    ```

### Get All Loans
*   **Endpoint:** `/loans`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Query Params:** `page`, `size`, `sort`
*   **Response:**
    ```json
    {
      "content": [
        {
          "loanId": "b2c3d4e5-...",
          "borrowerName": "Alice Smith",
          "principal": 10000,
          "status": "ACTIVE",
          // ... other loan fields
        }
      ],
      "pageable": { ... },
      "totalElements": 25,
      "totalPages": 3
    }
    ```

### Get Loans by Status
*   **Endpoint:** `/loans/status/{status}`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Path Params:** `status` (Enum: `CREATED`, `ACTIVE`, `CLOSED`, `DEFAULTED`)
*   **Response:** `Page<LoanResponse>` (Same format as Get All Loans)

### Get Loans by Borrower
*   **Endpoint:** `/loans/borrower/{borrowerId}`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Response:** `Page<LoanResponse>` (Same format as Get All Loans)

### Get Repayment Schedule
*   **Endpoint:** `/loans/{loanId}/schedule`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Response:**
    ```json
    [
      {
        "id": "c3d4e5f6-...",
        "emiNo": 1,
        "dueDate": "2023-11-25",
        "principalDue": 800,
        "interestDue": 138,
        "totalDue": 938,
        "principalPaid": 800,
        "interestPaid": 138,
        "penaltyPaid": 0,
        "principalRemaining": 0,
        "interestRemaining": 0,
        "status": "PAID"
      },
      {
        "id": "d4e5f6g7-...",
        "emiNo": 2,
        "dueDate": "2023-12-25",
        "principalDue": 810,
        "interestDue": 128,
        "totalDue": 938,
        "principalPaid": 0,
        "interestPaid": 0,
        "penaltyPaid": 0,
        "principalRemaining": 810,
        "interestRemaining": 128,
        "status": "PENDING"
      }
    ]
    ```

### Close Loan
*   **Endpoint:** `/loans/{loanId}/close`
*   **Method:** `POST`
*   **Access:** `LENDER`, `ADMIN`
*   **Response:** `204 No Content`

---

## Payments

### Record Payment
*   **Endpoint:** `/loans/{loanId}/payments`
*   **Method:** `POST`
*   **Access:** `LENDER`, `ADMIN`
*   **Request Body:**
    ```json
    {
      "amount": 1500,                   // Required, Min 1
      "paymentDate": "2023-10-25",      // Required, YYYY-MM-DD
      "mode": "UPI",                    // Required, Enum: [CASH, UPI, BANK, CHEQUE]
      "reference": "UPI123456789"       // Optional
    }
    ```
*   **Response:**
    ```json
    {
      "id": "e5f6g7h8-...",
      "loanId": "b2c3d4e5-...",
      "amountPaid": 1500,
      "paymentDate": "2023-10-25",
      "mode": "UPI",
      "reference": "UPI123456789",
      "createdAt": "2023-10-25T14:30:00",
      "allocations": [
        {
          "type": "PENALTY",
          "amount": 50
        },
        {
          "type": "INTEREST",
          "amount": 200
        },
        {
          "type": "PRINCIPAL",
          "amount": 1250
        }
      ]
    }
    ```

### Get Payments (Paged)
*   **Endpoint:** `/loans/{loanId}/payments`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Query Params:** `page`, `size`, `sort`
*   **Response:**
    ```json
    {
      "content": [
        {
          "id": "e5f6g7h8-...",
          "amountPaid": 1500,
          "mode": "UPI",
          // ... other payment fields
        }
      ],
      "pageable": { ... },
      "totalElements": 5,
      "totalPages": 1
    }
    ```

### Get All Payments
*   **Endpoint:** `/loans/{loanId}/payments/all`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Response:** `List<PaymentResponse>` (Array of payment objects)

---

## Reports

### Portfolio Summary
*   **Endpoint:** `/reports/portfolio`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Response:**
    ```json
    {
      "totalDisbursed": 500000,
      "outstanding": 350000,
      "totalCollected": 150000,
      "activeLoans": 45,
      "closedLoans": 12,
      "defaultedLoans": 3,
      "totalBorrowers": 50
    }
    ```

### Loan Statement
*   **Endpoint:** `/reports/loans/{loanId}/statement`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Response:**
    ```json
    {
      "loanId": "b2c3d4e5-...",
      "borrowerName": "Alice Smith",
      "principal": 10000,
      "totalInterest": 1250,
      "totalPayable": 11250,
      "totalPaid": 5000,
      "totalOutstanding": 6250,
      "status": "ACTIVE",
      "schedule": [ ... ], // List of RepaymentScheduleResponse
      "payments": [ ... ]  // List of PaymentResponse
    }
    ```

### Collections Summary
*   **Endpoint:** `/reports/collections`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Query Params:**
    *   `startDate` (Required, YYYY-MM-DD)
    *   `endDate` (Required, YYYY-MM-DD)
*   **Response:**
    ```json
    {
      "startDate": "2023-10-01",
      "endDate": "2023-10-31",
      "totalCollected": 25000,
      "principalCollected": 20000,
      "interestCollected": 4500,
      "penaltyCollected": 500
    }
    ```

### Overdue Loans
*   **Endpoint:** `/reports/overdue`
*   **Method:** `GET`
*   **Access:** `LENDER`, `ADMIN`, `AUDITOR`
*   **Response:**
    ```json
    [
      {
        "loanId": "f7g8h9i0-...",
        "borrowerName": "Bob Jones",
        "borrowerPhone": "+9876543210",
        "principalOutstanding": 5000,
        "interestOutstanding": 500,
        "penaltyOutstanding": 100,
        "totalOutstanding": 5600,
        "daysOverdue": 15,
        "overdueEmiCount": 1
      }
    ]
    ```

---

## Audit

### Get Audit Logs for Entity
*   **Endpoint:** `/audit/entity/{entity}/{entityId}`
*   **Method:** `GET`
*   **Access:** `ADMIN`, `AUDITOR`
*   **Path Params:**
    *   `entity`: Name of the entity (e.g., "Loan", "Borrower")
    *   `entityId`: UUID
*   **Response:**
    ```json
    {
      "content": [
        {
          "id": "audit-id-...",
          "entity": "Loan",
          "entityId": "loan-id-...",
          "action": "CREATE",
          "actor": "admin@example.com",
          "timestamp": "2023-10-25T10:00:00",
          "details": "Created loan for borrower..."
        }
      ],
      "pageable": { ... }
    }
    ```

### Get Audit Logs by User
*   **Endpoint:** `/audit/user/{userId}`
*   **Method:** `GET`
*   **Access:** `ADMIN`, `AUDITOR`
*   **Response:** `Page<AuditLogResponse>` (Same format as above)

---

## Enums

### UserRole
*   `LENDER`
*   `ADMIN`
*   `AUDITOR`

### InterestType
*   `FLAT`
*   `REDUCING`

### PaymentMode
*   `CASH`
*   `UPI`
*   `BANK`
*   `CHEQUE`

### BorrowerStatus
*   `ACTIVE`
*   `BLOCKED`

### LoanStatus
*   `CREATED`
*   `ACTIVE`
*   `CLOSED`
*   `DEFAULTED`

---

## Error Handling

The API uses standard HTTP status codes and a consistent error response format.

### Error Response Format

```json
{
  "errorCode": "ERROR_CODE_STRING",
  "message": "Human readable error message",
  "timestamp": "2023-10-25T14:30:00",
  "details": {                  // Optional, present for VALIDATION_ERROR
    "fieldName": "Error message for this field"
  }
}
```

### Common Error Codes

| HTTP Status | Error Code | Description |
| :--- | :--- | :--- |
| 400 Bad Request | `VALIDATION_ERROR` | Request body validation failed. See `details` for field-specific errors. |
| 400 Bad Request | `BAD_REQUEST` | Missing required parameters or invalid request format. |
| 401 Unauthorized | `UNAUTHORIZED` | Authentication failed or token is missing/invalid. |
| 403 Forbidden | `FORBIDDEN` | Authenticated user does not have permission to access the resource. |
| 404 Not Found | `NOT_FOUND` | The requested resource (Loan, Borrower, etc.) was not found. |
| 409 Conflict | `DUPLICATE` | Resource already exists (e.g., duplicate email). |
| 500 Internal Server Error | `INTERNAL_ERROR` | An unexpected error occurred on the server. |

### Business Logic Errors
Other specific error codes may be returned with `400 Bad Request` based on business rules (e.g., `LOAN_ALREADY_DISBURSED`, `INSUFFICIENT_FUNDS`).
