package com.llms.controller;

import com.llms.dto.request.CreateBorrowerRequest;
import com.llms.dto.request.UpdateBorrowerRequest;
import com.llms.dto.response.BorrowerResponse;
import com.llms.enums.BorrowerStatus;
import com.llms.service.BorrowerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/borrowers")
@RequiredArgsConstructor
public class BorrowerController {

    private final BorrowerService borrowerService;

    @PostMapping
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN')")
    public ResponseEntity<BorrowerResponse> createBorrower(@Valid @RequestBody CreateBorrowerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(borrowerService.createBorrower(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<BorrowerResponse> getBorrower(@PathVariable UUID id) {
        return ResponseEntity.ok(borrowerService.getBorrower(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<BorrowerResponse>> getAllBorrowers(Pageable pageable) {
        return ResponseEntity.ok(borrowerService.getAllBorrowers(pageable));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<BorrowerResponse>> getBorrowersByStatus(
            @PathVariable BorrowerStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(borrowerService.getBorrowersByStatus(status, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN')")
    public ResponseEntity<BorrowerResponse> updateBorrower(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBorrowerRequest request) {
        return ResponseEntity.ok(borrowerService.updateBorrower(id, request));
    }

    @PostMapping("/{id}/block")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN')")
    public ResponseEntity<Void> blockBorrower(@PathVariable UUID id) {
        borrowerService.blockBorrower(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unblock")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN')")
    public ResponseEntity<Void> unblockBorrower(@PathVariable UUID id) {
        borrowerService.unblockBorrower(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBorrower(@PathVariable UUID id) {
        borrowerService.deleteBorrower(id);
        return ResponseEntity.noContent().build();
    }
}
