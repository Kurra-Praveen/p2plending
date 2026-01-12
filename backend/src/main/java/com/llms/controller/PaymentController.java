package com.llms.controller;

import com.llms.dto.request.RecordPaymentRequest;
import com.llms.dto.response.PaymentResponse;
import com.llms.service.PaymentService;
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
@RequestMapping("/api/v1/loans/{loanId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> recordPayment(
            @PathVariable UUID loanId,
            @Valid @RequestBody RecordPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.recordPayment(loanId, request));
    }

    @GetMapping
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<PaymentResponse>> getPayments(
            @PathVariable UUID loanId,
            Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentsByLoan(loanId, pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<PaymentResponse>> getAllPayments(@PathVariable UUID loanId) {
        return ResponseEntity.ok(paymentService.getAllPaymentsByLoan(loanId));
    }
}
