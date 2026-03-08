package com.llms.dto.request;

import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;
import com.llms.enums.LoanFrequency;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a loan with advanced configuration options.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdvancedLoanRequest {

    @NotNull(message = "Borrower ID is required")
    private UUID borrowerId;

    @NotNull(message = "Principal amount is required")
    @Min(value = 1, message = "Principal must be positive")
    private Long principal;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0001", message = "Interest rate must be positive")
    private BigDecimal interestRate;

    @NotNull(message = "Interest rate mode is required")
    private InterestRateMode interestRateMode;

    @NotNull(message = "Interest type is required")
    private InterestType interestType;

    @NotNull(message = "Frequency is required")
    private LoanFrequency frequency;

    /**
     * Tenure in months (for MONTHLY frequency).
     */
    @Min(value = 1, message = "Tenure months must be at least 1")
    @Max(value = 360, message = "Tenure months cannot exceed 360")
    private Integer tenureMonths;

    /**
     * Tenure in weeks (for WEEKLY frequency).
     */
    @Min(value = 1, message = "Tenure units must be at least 1")
    @Max(value = 520, message = "Tenure units cannot exceed 520 weeks")
    private Integer tenureUnits;

    /**
     * Timezone for financial calculations (default: Asia/Kolkata).
     */
    private String timezone;

    /**
     * List of charge definition IDs to apply to this loan.
     */
    private List<UUID> chargeDefinitionIds;
}
