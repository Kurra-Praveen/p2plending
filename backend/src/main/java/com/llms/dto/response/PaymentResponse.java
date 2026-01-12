package com.llms.dto.response;

import com.llms.entity.Payment;
import com.llms.entity.PaymentAllocation;
import com.llms.enums.PaymentMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID id;
    private UUID loanId;
    private Long amountPaid;
    private LocalDate paymentDate;
    private PaymentMode mode;
    private String reference;
    private LocalDateTime createdAt;
    private List<AllocationDetail> allocations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationDetail {
        private String type;
        private Long amount;
    }

    public static PaymentResponse from(Payment payment) {
        List<AllocationDetail> allocationDetails = payment.getAllocations().stream()
                .map(a -> AllocationDetail.builder()
                        .type(a.getType().name())
                        .amount(a.getAmount())
                        .build())
                .collect(Collectors.toList());

        return PaymentResponse.builder()
                .id(payment.getId())
                .loanId(payment.getLoan().getId())
                .amountPaid(payment.getAmountPaid())
                .paymentDate(payment.getPaymentDate())
                .mode(payment.getMode())
                .reference(payment.getReference())
                .createdAt(payment.getCreatedAt())
                .allocations(allocationDetails)
                .build();
    }
}
