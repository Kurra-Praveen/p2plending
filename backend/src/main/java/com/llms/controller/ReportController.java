package com.llms.controller;

import com.llms.dto.response.*;
import com.llms.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportingService reportingService;

    @GetMapping("/portfolio")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<PortfolioSummaryResponse> getPortfolioSummary() {
        return ResponseEntity.ok(reportingService.getPortfolioSummary());
    }

    @GetMapping("/loans/{loanId}/statement")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<LoanStatementResponse> getLoanStatement(@PathVariable UUID loanId) {
        return ResponseEntity.ok(reportingService.getLoanStatement(loanId));
    }

    @GetMapping("/collections")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<CollectionsSummaryResponse> getCollectionsSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportingService.getCollectionsSummary(startDate, endDate));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<OverdueLoanResponse>> getOverdueLoans() {
        return ResponseEntity.ok(reportingService.getOverdueLoans());
    }
}
