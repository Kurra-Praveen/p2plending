package com.llms.dto.response;

import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;
import com.llms.entity.LoanInterestConfigHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for interest configuration history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestConfigHistoryResponse {

    private UUID id;
    private UUID loanId;
    private InterestRateMode interestRateMode;
    private BigDecimal interestRate;
    private InterestType interestType;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private UUID changedBy;
    private String changedByEmail;
    private String changeReason;
    private boolean isActive;
    private LocalDateTime createdAt;

    public static InterestConfigHistoryResponse from(LoanInterestConfigHistory history) {
        return InterestConfigHistoryResponse.builder()
                .id(history.getId())
                .loanId(history.getLoan().getId())
                .interestRateMode(history.getInterestRateMode())
                .interestRate(history.getInterestRate())
                .interestType(history.getInterestType())
                .effectiveFrom(history.getEffectiveFrom())
                .effectiveTo(history.getEffectiveTo())
                .changedBy(history.getChangedBy() != null ? history.getChangedBy().getId() : null)
                .changedByEmail(history.getChangedBy() != null ? history.getChangedBy().getEmail() : null)
                .changeReason(history.getChangeReason())
                .isActive(history.isActive())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
