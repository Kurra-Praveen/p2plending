package com.llms.dto.response;

import com.llms.entity.Loan;
import com.llms.enums.InterestType;
import com.llms.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponse {
    private UUID loanId;
    private UUID borrowerId;
    private String borrowerName;
    private Long principal;
    private BigDecimal interestRate;
    private InterestType interestType;
    private Integer tenureMonths;
    private Long emiAmount;
    private Long totalInterest;
    private Long totalPayable;
    private Long outstandingPrincipal;
    private Long outstandingInterest;
    private Long outstandingPenalty;
    private Long totalOutstanding;
    private LoanStatus status;
    private LocalDateTime disbursedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;

    public static LoanResponse from(Loan loan) {
        return LoanResponse.builder()
                .loanId(loan.getId())
                .borrowerId(loan.getBorrower().getId())
                .borrowerName(loan.getBorrower().getFullName())
                .principal(loan.getPrincipalAmount())
                .interestRate(loan.getInterestRate())
                .interestType(loan.getInterestType())
                .tenureMonths(loan.getTenureMonths())
                .emiAmount(loan.getEmiAmount())
                .totalInterest(loan.getTotalInterest())
                .totalPayable(loan.getTotalPayable())
                .outstandingPrincipal(loan.getOutstandingPrincipal())
                .outstandingInterest(loan.getOutstandingInterest())
                .outstandingPenalty(loan.getOutstandingPenalty())
                .totalOutstanding(loan.getTotalOutstanding())
                .status(loan.getStatus())
                .disbursedAt(loan.getDisbursedAt())
                .closedAt(loan.getClosedAt())
                .createdAt(loan.getCreatedAt())
                .build();
    }
}
