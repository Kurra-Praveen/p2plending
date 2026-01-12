package com.llms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionsSummaryResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalCollected;
    private Long principalCollected;
    private Long interestCollected;
    private Long penaltyCollected;
}
