package com.llms.controller;

import com.llms.dto.request.LoanPreviewRequest;
import com.llms.dto.request.SwitchInterestModeRequest;
import com.llms.dto.response.InterestConfigHistoryResponse;
import com.llms.dto.response.LoanConfigurationResponse;
import com.llms.dto.response.LoanPreviewResponse;
import com.llms.entity.LoanConfiguration;
import com.llms.entity.LoanInterestConfigHistory;
import com.llms.enums.LoanFrequency;
import com.llms.service.LoanConfigurationService;
import com.llms.service.schedule.ScheduleGenerator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for loan configuration and preview operations.
 */
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanConfigurationController {

    private final LoanConfigurationService configurationService;

    /**
     * Generate a loan preview (EMI calculation without creating loan).
     * Frontend MUST use this for displaying potential EMI and schedule.
     */
    @PostMapping("/preview")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN')")
    public ResponseEntity<LoanPreviewResponse> previewLoan(@Valid @RequestBody LoanPreviewRequest request) {
        int tenure = request.getFrequency() == LoanFrequency.WEEKLY
                ? request.getTenureUnits()
                : request.getTenureMonths();

        LocalDate startDate = request.getStartDate() != null
                ? request.getStartDate()
                : LocalDate.now();

        ScheduleGenerator.ScheduleResult result = configurationService.generateLoanPreview(
                request.getPrincipal(),
                request.getInterestRate(),
                request.getInterestRateMode(),
                request.getInterestType(),
                request.getFrequency(),
                tenure,
                startDate,
                request.getTimezone()
        );

        return ResponseEntity.ok(LoanPreviewResponse.from(result));
    }

    /**
     * Get the current configuration for a loan.
     */
    @GetMapping("/{loanId}/configuration")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<LoanConfigurationResponse> getConfiguration(@PathVariable UUID loanId) {
        LoanConfiguration config = configurationService.getConfiguration(loanId);
        return ResponseEntity.ok(LoanConfigurationResponse.from(config));
    }

    /**
     * Get the interest configuration history for a loan.
     */
    @GetMapping("/{loanId}/interest-history")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<InterestConfigHistoryResponse>> getInterestHistory(@PathVariable UUID loanId) {
        List<LoanInterestConfigHistory> history = configurationService.getInterestConfigHistory(loanId);
        List<InterestConfigHistoryResponse> response = history.stream()
                .map(InterestConfigHistoryResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Switch the interest rate mode for a loan.
     * Only allowed for ACTIVE loans with a future effective date.
     */
    @PostMapping("/{loanId}/switch-interest-mode")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN')")
    public ResponseEntity<LoanConfigurationResponse> switchInterestMode(
            @PathVariable UUID loanId,
            @Valid @RequestBody SwitchInterestModeRequest request
    ) {
        LoanConfiguration config = configurationService.switchInterestMode(
                loanId,
                request.getNewMode(),
                request.getNewRate(),
                request.getEffectiveDate(),
                request.getReason()
        );
        return ResponseEntity.ok(LoanConfigurationResponse.from(config));
    }
}
