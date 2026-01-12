-- V1__Initial_Schema.sql
-- Lender Loan Management System Database Schema
-- All monetary values in paise (smallest currency unit)

-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_user_role CHECK (role IN ('LENDER', 'ADMIN', 'AUDITOR')),
    CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- Borrowers table
CREATE TABLE borrowers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    address TEXT,
    risk_score INT DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_borrower_status CHECK (status IN ('ACTIVE', 'BLOCKED'))
);

CREATE INDEX idx_borrowers_phone ON borrowers(phone);
CREATE INDEX idx_borrowers_status ON borrowers(status);

-- Loans table
CREATE TABLE loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    borrower_id UUID NOT NULL REFERENCES borrowers(id),
    principal_amount BIGINT NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    interest_type VARCHAR(20) NOT NULL,
    tenure_months INT NOT NULL,
    emi_amount BIGINT NOT NULL,
    total_interest BIGINT NOT NULL,
    total_payable BIGINT NOT NULL,
    outstanding_principal BIGINT NOT NULL,
    outstanding_interest BIGINT NOT NULL,
    outstanding_penalty BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    disbursed_at TIMESTAMP,
    closed_at TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_loan_interest_type CHECK (interest_type IN ('FLAT', 'REDUCING')),
    CONSTRAINT chk_loan_status CHECK (status IN ('CREATED', 'ACTIVE', 'CLOSED', 'DEFAULTED')),
    CONSTRAINT chk_positive_principal CHECK (principal_amount > 0),
    CONSTRAINT chk_positive_tenure CHECK (tenure_months > 0)
);

CREATE INDEX idx_loans_borrower ON loans(borrower_id);
CREATE INDEX idx_loans_status ON loans(status);

-- Repayment Schedule table
CREATE TABLE repayment_schedule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES loans(id),
    emi_no INT NOT NULL,
    due_date DATE NOT NULL,
    principal_due BIGINT NOT NULL,
    interest_due BIGINT NOT NULL,
    total_due BIGINT NOT NULL,
    principal_paid BIGINT NOT NULL DEFAULT 0,
    interest_paid BIGINT NOT NULL DEFAULT 0,
    penalty_paid BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_emi_status CHECK (status IN ('PENDING', 'PAID', 'PARTIAL', 'OVERDUE')),
    CONSTRAINT uk_loan_emi UNIQUE (loan_id, emi_no)
);

CREATE INDEX idx_schedule_loan ON repayment_schedule(loan_id);
CREATE INDEX idx_schedule_due_date ON repayment_schedule(due_date);
CREATE INDEX idx_schedule_status ON repayment_schedule(status);

-- Disbursements table
CREATE TABLE disbursements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES loans(id),
    amount BIGINT NOT NULL,
    mode VARCHAR(50) NOT NULL,
    reference VARCHAR(255),
    disbursed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_disbursement_mode CHECK (mode IN ('CASH', 'UPI', 'BANK', 'CHEQUE'))
);

-- Payments table
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES loans(id),
    amount_paid BIGINT NOT NULL,
    payment_date DATE NOT NULL,
    mode VARCHAR(50) NOT NULL,
    reference VARCHAR(255),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_mode CHECK (mode IN ('CASH', 'UPI', 'BANK', 'CHEQUE')),
    CONSTRAINT chk_positive_payment CHECK (amount_paid > 0)
);

CREATE INDEX idx_payments_loan ON payments(loan_id);
CREATE INDEX idx_payments_date ON payments(payment_date);

-- Payment Allocations table
CREATE TABLE payment_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(id),
    schedule_id UUID REFERENCES repayment_schedule(id),
    type VARCHAR(50) NOT NULL,
    amount BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_allocation_type CHECK (type IN ('PENALTY', 'INTEREST', 'PRINCIPAL'))
);

CREATE INDEX idx_allocations_payment ON payment_allocations(payment_id);

-- Penalties table
CREATE TABLE penalties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES loans(id),
    schedule_id UUID NOT NULL REFERENCES repayment_schedule(id),
    emi_no INT NOT NULL,
    amount BIGINT NOT NULL,
    days_overdue INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UNPAID',
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_penalty_status CHECK (status IN ('UNPAID', 'PAID', 'PARTIAL', 'WAIVED'))
);

CREATE INDEX idx_penalties_loan ON penalties(loan_id);
CREATE INDEX idx_penalties_status ON penalties(status);

-- Audit Logs table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by UUID NOT NULL REFERENCES users(id),
    before_state JSONB,
    after_state JSONB,
    ip_address VARCHAR(45),
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_audit_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'STATUS_CHANGE'))
);

CREATE INDEX idx_audit_entity ON audit_logs(entity, entity_id);
CREATE INDEX idx_audit_performed_by ON audit_logs(performed_by);
CREATE INDEX idx_audit_performed_at ON audit_logs(performed_at);

-- Loan Status History table
CREATE TABLE loan_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES loans(id),
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    reason TEXT,
    changed_by UUID NOT NULL REFERENCES users(id),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_status_history_loan ON loan_status_history(loan_id);
