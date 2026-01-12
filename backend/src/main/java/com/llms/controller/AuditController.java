package com.llms.controller;

import com.llms.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/entity/{entity}/{entityId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<AuditService.AuditLogResponse>> getAuditLogsForEntity(
            @PathVariable String entity,
            @PathVariable UUID entityId,
            Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogsForEntity(entity, entityId, pageable));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Page<AuditService.AuditLogResponse>> getAuditLogsByUser(
            @PathVariable UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogsByUser(userId, pageable));
    }
}
