package com.llms.dto.request;

import com.llms.enums.ChargeApplicationTiming;
import com.llms.enums.ChargeCalculationType;
import com.llms.enums.ChargeType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a charge definition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChargeDefinitionRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Code must be uppercase alphanumeric with underscores")
    private String code;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Charge type is required")
    private ChargeType chargeType;

    @NotNull(message = "Calculation type is required")
    private ChargeCalculationType calculationType;

    /**
     * Fixed amount in paise (for FIXED calculation type).
     */
    @Min(value = 0, message = "Amount must be non-negative")
    private Long amount;

    /**
     * Percentage (for percentage-based calculation types).
     */
    @DecimalMin(value = "0.0001", message = "Percentage must be positive")
    @DecimalMax(value = "100.0000", message = "Percentage cannot exceed 100%")
    private BigDecimal percentage;

    @NotNull(message = "Application timing is required")
    private ChargeApplicationTiming applicationTiming;

    @com.fasterxml.jackson.annotation.JsonProperty("isMandatory")
    private Boolean isMandatory;
}
