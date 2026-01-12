package com.llms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummaryResponse {
    private Long totalDisbursed;
    private Long outstanding;
    private Long totalCollected;
    private Long activeLoans;
    private Long closedLoans;
    private Long defaultedLoans;
    private Long totalBorrowers;
}
