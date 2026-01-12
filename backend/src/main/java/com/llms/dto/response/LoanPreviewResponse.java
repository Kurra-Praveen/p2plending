package com.llms.dto.response;

import com.llms.service.schedule.ScheduleGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for loan preview (EMI calculation without creating loan).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanPreviewResponse {

    private long emiAmount;
    private long totalInterest;
    private long totalPayable;
    private List<ScheduleItemResponse> schedule;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleItemResponse {
        private int installmentNo;
        private LocalDate dueDate;
        private LocalDate periodStartDate;
        private LocalDate periodEndDate;
        private long principalDue;
        private long interestDue;
        private long totalDue;
        private long openingBalance;
        private long closingBalance;
    }

    public static LoanPreviewResponse from(ScheduleGenerator.ScheduleResult result) {
        List<ScheduleItemResponse> scheduleItems = result.getItems().stream()
                .map(item -> ScheduleItemResponse.builder()
                        .installmentNo(item.getInstallmentNo())
                        .dueDate(item.getDueDate())
                        .periodStartDate(item.getPeriodStartDate())
                        .periodEndDate(item.getPeriodEndDate())
                        .principalDue(item.getPrincipalDue())
                        .interestDue(item.getInterestDue())
                        .totalDue(item.getTotalDue())
                        .openingBalance(item.getOpeningBalance())
                        .closingBalance(item.getClosingBalance())
                        .build())
                .collect(Collectors.toList());

        return LoanPreviewResponse.builder()
                .emiAmount(result.getEmiAmount())
                .totalInterest(result.getTotalInterest())
                .totalPayable(result.getTotalPayable())
                .schedule(scheduleItems)
                .build();
    }
}
