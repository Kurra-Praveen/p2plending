package com.llms.service.schedule;

import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;
import com.llms.enums.LoanFrequency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Interface for generating loan repayment schedules.
 * Implementations handle different loan frequencies (monthly, weekly).
 */
public interface ScheduleGenerator {

    /**
     * Returns the loan frequency this generator handles.
     */
    LoanFrequency getFrequency();

    /**
     * Generates a repayment schedule for a loan.
     *
     * @param request Schedule generation request parameters
     * @return Generated schedule with EMI details
     */
    ScheduleResult generateSchedule(ScheduleRequest request);

    @Data
    @Builder
    class ScheduleRequest {
        private long principal;
        private BigDecimal interestRate;
        private InterestRateMode interestRateMode;
        private InterestType interestType;
        private int tenure;
        private LocalDate startDate;
        private String timezone;
    }

    @Data
    @Builder
    class ScheduleResult {
        private long emiAmount;
        private long totalInterest;
        private long totalPayable;
        private List<ScheduleItem> items;
    }

    @Data
    @Builder
    class ScheduleItem {
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
}
