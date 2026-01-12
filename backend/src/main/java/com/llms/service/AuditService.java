package com.llms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.llms.entity.AuditLog;
import com.llms.entity.User;
import com.llms.enums.AuditAction;
import com.llms.repository.AuditLogRepository;
import com.llms.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Audit Service for immutable logging of all critical operations.
 * Logs: who, what, when, before state, after state
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    /**
     * Log entity creation
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(String entityName, UUID entityId, Object afterState) {
        createAuditLog(entityName, entityId, AuditAction.CREATE, null, afterState);
    }

    /**
     * Log entity update
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(String entityName, UUID entityId, Object beforeState, Object afterState) {
        createAuditLog(entityName, entityId, AuditAction.UPDATE, beforeState, afterState);
    }

    /**
     * Log entity deletion (soft delete)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDelete(String entityName, UUID entityId, Object beforeState) {
        createAuditLog(entityName, entityId, AuditAction.DELETE, beforeState, null);
    }

    /**
     * Log status change
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logStatusChange(String entityName, UUID entityId, Object beforeState, Object afterState) {
        createAuditLog(entityName, entityId, AuditAction.STATUS_CHANGE, beforeState, afterState);
    }

    private void createAuditLog(String entityName, UUID entityId, AuditAction action,
                                 Object beforeState, Object afterState) {
        try {
            User currentUser = securityUtils.getCurrentUser();
            String ipAddress = getClientIpAddress();

            Map<String, Object> beforeStateMap = convertToMap(beforeState);
            Map<String, Object> afterStateMap = convertToMap(afterState);

            AuditLog auditLog = AuditLog.builder()
                    .entity(entityName)
                    .entityId(entityId)
                    .action(action)
                    .performedBy(currentUser)
                    .beforeState(beforeStateMap)
                    .afterState(afterStateMap)
                    .ipAddress(ipAddress)
                    .performedAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);

            log.debug("Audit log created: {} {} {} by {}",
                    action, entityName, entityId, currentUser.getEmail());

        } catch (Exception e) {
            log.error("Failed to create audit log for {} {}: {}",
                    entityName, entityId, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(obj);
            return objectMapper.readValue(json, HashMap.class);
        } catch (Exception e) {
            log.warn("Failed to convert object to map: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("value", obj.toString());
            return fallback;
        }
    }

    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not get client IP: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get audit logs for an entity
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsForEntity(String entity, UUID entityId, Pageable pageable) {
        return auditLogRepository.findByEntityAndEntityId(entity, entityId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get audit logs by user
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByPerformedById(userId, pageable)
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .entity(auditLog.getEntity())
                .entityId(auditLog.getEntityId())
                .action(auditLog.getAction().name())
                .performedBy(auditLog.getPerformedBy().getEmail())
                .beforeState(auditLog.getBeforeState())
                .afterState(auditLog.getAfterState())
                .ipAddress(auditLog.getIpAddress())
                .performedAt(auditLog.getPerformedAt())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AuditLogResponse {
        private UUID id;
        private String entity;
        private UUID entityId;
        private String action;
        private String performedBy;
        private Map<String, Object> beforeState;
        private Map<String, Object> afterState;
        private String ipAddress;
        private LocalDateTime performedAt;
    }
}
