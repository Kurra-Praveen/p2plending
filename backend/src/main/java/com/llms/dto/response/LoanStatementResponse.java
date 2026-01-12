package com.llms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanStatementResponse {
    private UUID loanId;
    private String borrowerName;
    private Long principal;
    private Long totalInterest;
    private Long totalPayable;
    private Long totalPaid;
    private Long totalOutstanding;
    private String status;
    private List<RepaymentScheduleResponse> schedule;
    private List<PaymentResponse> payments;
}
