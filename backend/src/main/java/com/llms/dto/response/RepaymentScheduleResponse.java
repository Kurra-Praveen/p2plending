package com.llms.dto.response;

import com.llms.entity.RepaymentSchedule;
import com.llms.enums.EmiStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentScheduleResponse {
    private UUID id;
    private Integer emiNo;
    private LocalDate dueDate;
    private Long principalDue;
    private Long interestDue;
    private Long totalDue;
    private Long principalPaid;
    private Long interestPaid;
    private Long penaltyPaid;
    private Long principalRemaining;
    private Long interestRemaining;
    private EmiStatus status;

    public static RepaymentScheduleResponse from(RepaymentSchedule schedule) {
        return RepaymentScheduleResponse.builder()
                .id(schedule.getId())
                .emiNo(schedule.getEmiNo())
                .dueDate(schedule.getDueDate())
                .principalDue(schedule.getPrincipalDue())
                .interestDue(schedule.getInterestDue())
                .totalDue(schedule.getTotalDue())
                .principalPaid(schedule.getPrincipalPaid())
                .interestPaid(schedule.getInterestPaid())
                .penaltyPaid(schedule.getPenaltyPaid())
                .principalRemaining(schedule.getPrincipalRemaining())
                .interestRemaining(schedule.getInterestRemaining())
                .status(schedule.getStatus())
                .build();
    }
}
