package com.llms.controller;

import com.llms.dto.request.CreateChargeDefinitionRequest;
import com.llms.dto.response.ChargeDefinitionResponse;
import com.llms.dto.response.LoanChargeResponse;
import com.llms.entity.ChargeDefinition;
import com.llms.entity.LoanCharge;
import com.llms.entity.User;
import com.llms.repository.ChargeDefinitionRepository;
import com.llms.security.SecurityUtils;
import com.llms.service.ChargeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for charge definitions and loan charges.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChargeController {

    private final ChargeService chargeService;
    private final ChargeDefinitionRepository chargeDefinitionRepository;
    private final SecurityUtils securityUtils;

    // ========== Charge Definitions ==========

    /**
     * Create a new charge definition.
     */
    @PostMapping("/charge-definitions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ChargeDefinitionResponse> createChargeDefinition(
            @Valid @RequestBody CreateChargeDefinitionRequest request
    ) {
        User currentUser = securityUtils.getCurrentUser();

        ChargeDefinition definition = ChargeDefinition.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .chargeType(request.getChargeType())
                .calculationType(request.getCalculationType())
                .amount(request.getAmount())
                .percentage(request.getPercentage())
                .applicationTiming(request.getApplicationTiming())
                .isMandatory(request.getIsMandatory() != null ? request.getIsMandatory() : false)
                .isActive(true)
                .createdBy(currentUser)
                .build();

        definition = chargeDefinitionRepository.save(definition);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ChargeDefinitionResponse.from(definition));
    }

    /**
     * Get all active charge definitions.
     */
    @GetMapping("/charge-definitions")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<ChargeDefinitionResponse>> getActiveChargeDefinitions() {
        List<ChargeDefinition> definitions = chargeService.getActiveChargeDefinitions();
        List<ChargeDefinitionResponse> response = definitions.stream()
                .map(ChargeDefinitionResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific charge definition.
     */
    @GetMapping("/charge-definitions/{id}")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<ChargeDefinitionResponse> getChargeDefinition(@PathVariable UUID id) {
        ChargeDefinition definition = chargeDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Charge definition not found: " + id));
        return ResponseEntity.ok(ChargeDefinitionResponse.from(definition));
    }

    /**
     * Deactivate a charge definition.
     */
    @DeleteMapping("/charge-definitions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateChargeDefinition(@PathVariable UUID id) {
        ChargeDefinition definition = chargeDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Charge definition not found: " + id));
        definition.setIsActive(false);
        chargeDefinitionRepository.save(definition);
        return ResponseEntity.noContent().build();
    }

    // ========== Loan Charges ==========

    /**
     * Get all charges for a loan.
     */
    @GetMapping("/loans/{loanId}/charges")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<LoanChargeResponse>> getLoanCharges(@PathVariable UUID loanId) {
        List<LoanCharge> charges = chargeService.getLoanCharges(loanId);
        List<LoanChargeResponse> response = charges.stream()
                .map(LoanChargeResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get outstanding charges for a loan.
     */
    @GetMapping("/loans/{loanId}/charges/outstanding")
    @PreAuthorize("hasRole('LENDER') or hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<LoanChargeResponse>> getOutstandingCharges(@PathVariable UUID loanId) {
        List<LoanCharge> charges = chargeService.getOutstandingCharges(loanId);
        List<LoanChargeResponse> response = charges.stream()
                .map(LoanChargeResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Waive a charge.
     */
    @PostMapping("/charges/{chargeId}/waive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LoanChargeResponse> waiveCharge(
            @PathVariable UUID chargeId,
            @RequestBody Map<String, String> request
    ) {
        String reason = request.getOrDefault("reason", "Waived by admin");
        User currentUser = securityUtils.getCurrentUser();
        LoanCharge charge = chargeService.waiveCharge(chargeId, currentUser, reason);
        return ResponseEntity.ok(LoanChargeResponse.from(charge));
    }
}
