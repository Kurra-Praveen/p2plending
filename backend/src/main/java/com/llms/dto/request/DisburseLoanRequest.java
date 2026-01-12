package com.llms.dto.request;

import com.llms.enums.PaymentMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisburseLoanRequest {

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be positive")
    private Long amount;

    @NotNull(message = "Payment mode is required")
    private PaymentMode mode;

    private String reference;
}
