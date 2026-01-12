package com.llms.controller;

import com.llms.dto.request.CreateLoanRequest;
import com.llms.dto.request.DisburseLoanRequest;
import com.llms.dto.response.LoanResponse;
import com.llms.dto.response.RepaymentScheduleResponse;
import com.llms.enums.LoanStatus;
import com.llms.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN')")
    public ResponseEntity<LoanResponse> createLoan(@Valid @RequestBody CreateLoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.createLoan(request));
    }

    @PostMapping("/{loanId}/disburse")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN')")
    public ResponseEntity<LoanResponse> disburseLoan(
            @PathVariable UUID loanId,
            @Valid @RequestBody DisburseLoanRequest request) {
        return ResponseEntity.ok(loanService.disburseLoan(loanId, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<LoanResponse> getLoan(@PathVariable UUID id) {
        return ResponseEntity.ok(loanService.getLoan(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<LoanResponse>> getAllLoans(Pageable pageable) {
        return ResponseEntity.ok(loanService.getAllLoans(pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<LoanResponse>> getLoansByStatus(
            @PathVariable LoanStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(loanService.getLoansByStatus(status, pageable));
    }

    @GetMapping("/borrower/{borrowerId}")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<LoanResponse>> getLoansByBorrower(
            @PathVariable UUID borrowerId,
            Pageable pageable) {
        return ResponseEntity.ok(loanService.getLoansByBorrower(borrowerId, pageable));
    }

    @GetMapping("/{loanId}/schedule")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<RepaymentScheduleResponse>> getRepaymentSchedule(@PathVariable UUID loanId) {
        return ResponseEntity.ok(loanService.getRepaymentSchedule(loanId));
    }

    @PostMapping("/{loanId}/close")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN')")
    public ResponseEntity<Void> closeLoan(@PathVariable UUID loanId) {
        loanService.closeLoan(loanId);
        return ResponseEntity.noContent().build();
    }
}
