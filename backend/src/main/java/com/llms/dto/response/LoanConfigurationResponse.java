package com.llms.dto.response;

import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;
import com.llms.enums.LoanFrequency;
import com.llms.entity.LoanConfiguration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for loan configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanConfigurationResponse {

    private UUID id;
    private UUID loanId;
    private InterestRateMode interestRateMode;
    private BigDecimal interestRate;
    private InterestType interestType;
    private LoanFrequency frequency;
    private Integer tenureMonths;
    private Integer tenureUnits;
    private String timezone;
    private UUID activeHistoryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LoanConfigurationResponse from(LoanConfiguration config) {
        return LoanConfigurationResponse.builder()
                .id(config.getId())
                .loanId(config.getLoan().getId())
                .interestRateMode(config.getInterestRateMode())
                .interestRate(config.getInterestRate())
                .interestType(config.getInterestType())
                .frequency(config.getFrequency())
                .tenureMonths(config.getTenureMonths())
                .tenureUnits(config.getTenureUnits())
                .timezone(config.getTimezone())
                .activeHistoryId(config.getActiveHistory() != null ? config.getActiveHistory().getId() : null)
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
