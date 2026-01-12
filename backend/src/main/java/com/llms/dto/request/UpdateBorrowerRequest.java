package com.llms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBorrowerRequest {

    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    private String address;

    @Min(value = 0, message = "Risk score must be non-negative")
    private Integer riskScore;
}
