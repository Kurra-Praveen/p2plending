package com.llms.dto.request;

import com.llms.enums.InterestRateMode;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for switching interest rate mode on an existing loan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwitchInterestModeRequest {

    @NotNull(message = "New interest rate mode is required")
    private InterestRateMode newMode;

    @NotNull(message = "New interest rate is required")
    @DecimalMin(value = "0.0001", message = "Interest rate must be positive")
    private BigDecimal newRate;

    @NotNull(message = "Effective date is required")
    @FutureOrPresent(message = "Effective date must be in the future")
    private LocalDate effectiveDate;

    @NotBlank(message = "Reason for change is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
