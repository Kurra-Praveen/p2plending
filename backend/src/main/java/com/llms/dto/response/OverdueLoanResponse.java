package com.llms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverdueLoanResponse {
    private UUID loanId;
    private String borrowerName;
    private String borrowerPhone;
    private Long principalOutstanding;
    private Long interestOutstanding;
    private Long penaltyOutstanding;
    private Long totalOutstanding;
    private Integer daysOverdue;
    private Integer overdueEmiCount;
}
