package com.llms.dto.response;

import com.llms.entity.Borrower;
import com.llms.enums.BorrowerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowerResponse {
    private UUID id;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private Integer riskScore;
    private BorrowerStatus status;
    private LocalDateTime createdAt;

    public static BorrowerResponse from(Borrower borrower) {
        return BorrowerResponse.builder()
                .id(borrower.getId())
                .fullName(borrower.getFullName())
                .phone(borrower.getPhone())
                .email(borrower.getEmail())
                .address(borrower.getAddress())
                .riskScore(borrower.getRiskScore())
                .status(borrower.getStatus())
                .createdAt(borrower.getCreatedAt())
                .build();
    }
}
