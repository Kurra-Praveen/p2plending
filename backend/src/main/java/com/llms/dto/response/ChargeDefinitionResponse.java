package com.llms.dto.response;

import com.llms.enums.ChargeApplicationTiming;
import com.llms.enums.ChargeCalculationType;
import com.llms.enums.ChargeType;
import com.llms.entity.ChargeDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for charge definitions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeDefinitionResponse {

    private UUID id;
    private String name;
    private String code;
    private String description;
    private ChargeType chargeType;
    private ChargeCalculationType calculationType;
    private Long amount;
    private BigDecimal percentage;
    private ChargeApplicationTiming applicationTiming;
    @com.fasterxml.jackson.annotation.JsonProperty("isActive")
    private boolean isActive;
    @com.fasterxml.jackson.annotation.JsonProperty("isMandatory")
    private boolean isMandatory;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChargeDefinitionResponse from(ChargeDefinition definition) {
        return ChargeDefinitionResponse.builder()
                .id(definition.getId())
                .name(definition.getName())
                .code(definition.getCode())
                .description(definition.getDescription())
                .chargeType(definition.getChargeType())
                .calculationType(definition.getCalculationType())
                .amount(definition.getAmount())
                .percentage(definition.getPercentage())
                .applicationTiming(definition.getApplicationTiming())
                .isActive(definition.getIsActive())
                .isMandatory(definition.getIsMandatory())
                .createdBy(definition.getCreatedBy() != null ? definition.getCreatedBy().getId() : null)
                .createdAt(definition.getCreatedAt())
                .updatedAt(definition.getUpdatedAt())
                .build();
    }
}
