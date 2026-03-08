package com.llms.dto.response;

import com.llms.enums.ChargeStatus;
import com.llms.enums.ChargeType;
import com.llms.entity.LoanCharge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for loan charges.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanChargeResponse {

    private UUID id;
    private UUID loanId;
    private UUID chargeDefinitionId;
    private String name;
    private ChargeType chargeType;
    private long amount;
    private long amountPaid;
    private long amountWaived;
    private long outstandingAmount;
    private ChargeStatus status;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private LocalDateTime waivedAt;
    private UUID waivedBy;
    private String waiverReason;
    private LocalDateTime createdAt;

    public static LoanChargeResponse from(LoanCharge charge) {
        return LoanChargeResponse.builder()
                .id(charge.getId())
                .loanId(charge.getLoan().getId())
                .chargeDefinitionId(charge.getChargeDefinition() != null ? charge.getChargeDefinition().getId() : null)
                .name(charge.getName())
                .chargeType(charge.getChargeType())
                .amount(charge.getAmount())
                .amountPaid(charge.getAmountPaid())
                .amountWaived(charge.getAmountWaived())
                .outstandingAmount(charge.getOutstandingAmount())
                .status(charge.getStatus())
                .dueDate(charge.getDueDate())
                .paidAt(charge.getPaidAt())
                .waivedAt(charge.getWaivedAt())
                .waivedBy(charge.getWaivedBy() != null ? charge.getWaivedBy().getId() : null)
                .waiverReason(charge.getWaiverReason())
                .createdAt(charge.getCreatedAt())
                .build();
    }
}
