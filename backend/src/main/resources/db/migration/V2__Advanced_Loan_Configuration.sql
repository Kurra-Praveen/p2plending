-- V2: Advanced Flexible Loan Configuration
-- This migration adds support for:
-- 1. Multiple interest rate modes (annual%, monthly%, daily%, fixed rupee)
-- 2. Weekly and monthly repayment frequencies
-- 3. Configurable charges (processing fee, insurance, custom)
-- 4. Interest configuration history for audit trail

-- =============================================================================
-- STEP 1: Add new columns to loans table (additive only, nullable with defaults)
-- =============================================================================

-- Add frequency support (default MONTHLY for backward compatibility)
ALTER TABLE loans ADD COLUMN IF NOT EXISTS frequency VARCHAR(20) DEFAULT 'MONTHLY';
ALTER TABLE loans ADD CONSTRAINT chk_loan_frequency CHECK (frequency IN ('MONTHLY', 'WEEKLY'));

-- Add tenure_units for weekly loans (weeks)
ALTER TABLE loans ADD COLUMN IF NOT EXISTS tenure_units INTEGER;

-- Add timezone for financial calculations (default Asia/Kolkata)
ALTER TABLE loans ADD COLUMN IF NOT EXISTS timezone VARCHAR(50) DEFAULT 'Asia/Kolkata';

-- =============================================================================
-- STEP 2: Create loan_configurations table
-- =============================================================================

CREATE TABLE IF NOT EXISTS loan_configurations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES loans(id) ON DELETE CASCADE,

    -- Interest Configuration
    interest_rate_mode VARCHAR(30) NOT NULL DEFAULT 'ANNUAL_PERCENTAGE',
    interest_rate DECIMAL(10, 6) NOT NULL,
    interest_type VARCHAR(20) NOT NULL DEFAULT 'REDUCING',

    -- Frequency Configuration
    frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    tenure_months INTEGER,
    tenure_units INTEGER,

    -- Timezone
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',

    -- Active configuration reference
    active_history_id UUID,

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_config_interest_rate_mode CHECK (interest_rate_mode IN (
        'ANNUAL_PERCENTAGE',
        'MONTHLY_PERCENTAGE',
        'DAILY_PERCENTAGE',
        'FIXED_RUPEE_PER_DAY',
        'FIXED_RUPEE_PER_MONTH'
    )),
    CONSTRAINT chk_config_interest_type CHECK (interest_type IN ('FLAT', 'REDUCING')),
    CONSTRAINT chk_config_frequency CHECK (frequency IN ('MONTHLY', 'WEEKLY')),
    CONSTRAINT uq_loan_config UNIQUE (loan_id)
);

-- Index for quick lookup by loan
CREATE INDEX IF NOT EXISTS idx_loan_config_loan_id ON loan_configurations(loan_id);

-- =============================================================================
-- STEP 3: Create loan_interest_config_history table (immutable audit trail)
-- =============================================================================

CREATE TABLE IF NOT EXISTS loan_interest_config_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES loans(id) ON DELETE CASCADE,

    -- Interest Configuration Snapshot
    interest_rate_mode VARCHAR(30) NOT NULL,
    interest_rate DECIMAL(10, 6) NOT NULL,
    interest_type VARCHAR(20) NOT NULL,

    -- Effective period (NULL effective_to means currently active)
    effective_from DATE NOT NULL,
    effective_to DATE,

    -- Change metadata
    changed_by UUID REFERENCES users(id),
    change_reason VARCHAR(500),

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_history_interest_rate_mode CHECK (interest_rate_mode IN (
        'ANNUAL_PERCENTAGE',
        'MONTHLY_PERCENTAGE',
        'DAILY_PERCENTAGE',
        'FIXED_RUPEE_PER_DAY',
        'FIXED_RUPEE_PER_MONTH'
    )),
    CONSTRAINT chk_history_interest_type CHECK (interest_type IN ('FLAT', 'REDUCING')),
    CONSTRAINT chk_effective_dates CHECK (effective_to IS NULL OR effective_to > effective_from)
);

-- Ensure exactly one active configuration per loan
CREATE UNIQUE INDEX IF NOT EXISTS ux_active_interest_config
ON loan_interest_config_history (loan_id)
WHERE effective_to IS NULL;

-- Index for querying history
CREATE INDEX IF NOT EXISTS idx_interest_history_loan_id ON loan_interest_config_history(loan_id);
CREATE INDEX IF NOT EXISTS idx_interest_history_effective ON loan_interest_config_history(loan_id, effective_from, effective_to);

-- =============================================================================
-- STEP 4: Create charge_definitions table (template for charges)
-- =============================================================================

CREATE TABLE IF NOT EXISTS charge_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Charge metadata
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    charge_type VARCHAR(30) NOT NULL,

    -- Calculation method
    calculation_type VARCHAR(30) NOT NULL DEFAULT 'FIXED',
    amount BIGINT,  -- For fixed charges (in paise)
    percentage DECIMAL(5, 4),  -- For percentage-based charges

    -- Application timing
    application_timing VARCHAR(30) NOT NULL DEFAULT 'DISBURSEMENT',

    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    is_mandatory BOOLEAN DEFAULT FALSE,

    -- Ownership
    created_by UUID REFERENCES users(id),

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_charge_type CHECK (charge_type IN ('PROCESSING_FEE', 'INSURANCE', 'DOCUMENTATION', 'CUSTOM')),
    CONSTRAINT chk_calculation_type CHECK (calculation_type IN ('FIXED', 'PERCENTAGE_OF_PRINCIPAL', 'PERCENTAGE_OF_DISBURSEMENT')),
    CONSTRAINT chk_application_timing CHECK (application_timing IN ('DISBURSEMENT', 'FIRST_EMI', 'SPREAD_ACROSS_TENURE', 'CUSTOM_DATE')),
    CONSTRAINT uq_charge_code UNIQUE (code)
);

-- Index for active charge lookups
CREATE INDEX IF NOT EXISTS idx_charge_definitions_active ON charge_definitions(is_active) WHERE is_active = TRUE;

-- =============================================================================
-- STEP 5: Create loan_charges table (charges applied to specific loans)
-- =============================================================================

CREATE TABLE IF NOT EXISTS loan_charges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES loans(id) ON DELETE CASCADE,
    charge_definition_id UUID REFERENCES charge_definitions(id),

    -- Charge details (snapshot from definition or custom)
    name VARCHAR(100) NOT NULL,
    charge_type VARCHAR(30) NOT NULL,

    -- Calculated amounts (in paise)
    amount BIGINT NOT NULL,
    amount_paid BIGINT DEFAULT 0,
    amount_waived BIGINT DEFAULT 0,

    -- Status
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    -- Due date (if applicable)
    due_date DATE,
    paid_at TIMESTAMP,
    waived_at TIMESTAMP,
    waived_by UUID REFERENCES users(id),
    waiver_reason VARCHAR(500),

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_loan_charge_type CHECK (charge_type IN ('PROCESSING_FEE', 'INSURANCE', 'DOCUMENTATION', 'CUSTOM')),
    CONSTRAINT chk_loan_charge_status CHECK (status IN ('PENDING', 'PARTIAL', 'PAID', 'WAIVED'))
);

-- Indexes for charge lookups
CREATE INDEX IF NOT EXISTS idx_loan_charges_loan_id ON loan_charges(loan_id);
CREATE INDEX IF NOT EXISTS idx_loan_charges_status ON loan_charges(status) WHERE status IN ('PENDING', 'PARTIAL');

-- =============================================================================
-- STEP 6: Create charge_schedule table (for charges spread across tenure)
-- =============================================================================

CREATE TABLE IF NOT EXISTS charge_schedule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_charge_id UUID NOT NULL REFERENCES loan_charges(id) ON DELETE CASCADE,
    schedule_id UUID REFERENCES repayment_schedule(id),

    -- Scheduled amount (in paise)
    amount_due BIGINT NOT NULL,
    amount_paid BIGINT DEFAULT 0,

    -- Due date
    due_date DATE NOT NULL,

    -- Status
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP,

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_charge_schedule_status CHECK (status IN ('PENDING', 'PARTIAL', 'PAID'))
);

-- Index for charge schedule lookups
CREATE INDEX IF NOT EXISTS idx_charge_schedule_loan_charge ON charge_schedule(loan_charge_id);
CREATE INDEX IF NOT EXISTS idx_charge_schedule_due_date ON charge_schedule(due_date) WHERE status IN ('PENDING', 'PARTIAL');

-- =============================================================================
-- STEP 7: Update payment_allocations to include charges
-- =============================================================================

-- Add CHARGE to allocation type (update CHECK constraint)
ALTER TABLE payment_allocations DROP CONSTRAINT IF EXISTS payment_allocations_type_check;
ALTER TABLE payment_allocations ADD CONSTRAINT payment_allocations_type_check
    CHECK (type IN ('PENALTY', 'INTEREST', 'PRINCIPAL', 'CHARGE'));

-- Add reference to charge_schedule for charge allocations
ALTER TABLE payment_allocations ADD COLUMN IF NOT EXISTS charge_schedule_id UUID REFERENCES charge_schedule(id);

-- =============================================================================
-- STEP 8: Add outstanding charges to loans table
-- =============================================================================

ALTER TABLE loans ADD COLUMN IF NOT EXISTS outstanding_charges BIGINT DEFAULT 0;

-- =============================================================================
-- STEP 9: Backfill existing loans with default configuration
-- =============================================================================

-- Create loan_configurations for existing loans
INSERT INTO loan_configurations (
    loan_id,
    interest_rate_mode,
    interest_rate,
    interest_type,
    frequency,
    tenure_months,
    timezone
)
SELECT
    l.id,
    'ANNUAL_PERCENTAGE',
    l.interest_rate,
    l.interest_type,
    'MONTHLY',
    l.tenure_months,
    'Asia/Kolkata'
FROM loans l
WHERE NOT EXISTS (
    SELECT 1 FROM loan_configurations lc WHERE lc.loan_id = l.id
);

-- Create initial history records for existing loans
INSERT INTO loan_interest_config_history (
    loan_id,
    interest_rate_mode,
    interest_rate,
    interest_type,
    effective_from,
    effective_to,
    change_reason
)
SELECT
    l.id,
    'ANNUAL_PERCENTAGE',
    l.interest_rate,
    l.interest_type,
    COALESCE(l.disbursed_at::DATE, l.created_at::DATE),
    NULL,
    'Initial configuration (backfill from V2 migration)'
FROM loans l
WHERE NOT EXISTS (
    SELECT 1 FROM loan_interest_config_history h WHERE h.loan_id = l.id
);

-- Update loan_configurations with active_history_id
UPDATE loan_configurations lc
SET active_history_id = (
    SELECT h.id
    FROM loan_interest_config_history h
    WHERE h.loan_id = lc.loan_id AND h.effective_to IS NULL
    LIMIT 1
)
WHERE lc.active_history_id IS NULL;

-- =============================================================================
-- STEP 10: Add foreign key from loan_configurations to history
-- =============================================================================

ALTER TABLE loan_configurations
ADD CONSTRAINT fk_config_active_history
FOREIGN KEY (active_history_id) REFERENCES loan_interest_config_history(id);
