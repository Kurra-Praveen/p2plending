package com.llms.scheduler;

import com.llms.service.DelinquencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled jobs for daily processing
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyJobScheduler {

    private final DelinquencyService delinquencyService;

    /**
     * Daily overdue scanner and penalty accrual job
     * Runs every day at 1:00 AM
     */
    @Scheduled(cron = "${scheduler.overdue-job.cron:0 0 1 * * ?}")
    public void runDailyOverdueJob() {
        log.info("Starting daily overdue job");
        try {
            delinquencyService.processOverdueLoans();
            log.info("Daily overdue job completed successfully");
        } catch (Exception e) {
            log.error("Daily overdue job failed: {}", e.getMessage(), e);
        }
    }
}
