package com.llms.controller;

import com.llms.BaseIntegrationTest;
import com.llms.entity.AuditLog;
import com.llms.entity.Borrower;
import com.llms.enums.AuditAction;
import com.llms.enums.BorrowerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuditController Integration Tests")
class AuditControllerTest extends BaseIntegrationTest {

    private static final String AUDIT_BASE_URL = "/api/v1/audit";

    private Borrower testBorrower;

    @BeforeEach
    void setUp() {
        testBorrower = Borrower.builder()
                .fullName("Audit Test Borrower")
                .phone("6666666666")
                .email("audit@test.com")
                .status(BorrowerStatus.ACTIVE)
                .createdBy(lenderUser)
                .build();
        testBorrower = borrowerRepository.save(testBorrower);

        // Create some audit logs
        createAuditLog("Borrower", testBorrower.getId(), AuditAction.CREATE);
        createAuditLog("Borrower", testBorrower.getId(), AuditAction.UPDATE);
    }

    private void createAuditLog(String entity, UUID entityId, AuditAction action) {
        Map<String, Object> beforeState = new HashMap<>();
        beforeState.put("status", "OLD");

        Map<String, Object> afterState = new HashMap<>();
        afterState.put("status", "NEW");

        AuditLog log = AuditLog.builder()
                .entity(entity)
                .entityId(entityId)
                .action(action)
                .performedBy(lenderUser)
                .beforeState(action == AuditAction.CREATE ? null : beforeState)
                .afterState(afterState)
                .ipAddress("127.0.0.1")
                .performedAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    @Nested
    @DisplayName("GET /api/v1/audit/entity/{entity}/{entityId}")
    class GetAuditLogsForEntityTests {

        @Test
        @DisplayName("ADMIN should access audit logs for entity")
        void adminShouldAccessAuditLogs() throws Exception {
            mockMvc.perform(get(AUDIT_BASE_URL + "/entity/Borrower/" + testBorrower.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                    .andExpect(jsonPath("$.content[0].entity", is("Borrower")))
                    .andExpect(jsonPath("$.content[0].entityId", is(testBorrower.getId().toString())));
        }

        @Test
        @DisplayName("AUDITOR should access audit logs for entity")
        void auditorShouldAccessAuditLogs() throws Exception {
            mockMvc.perform(get(AUDIT_BASE_URL + "/entity/Borrower/" + testBorrower.getId())
                            .header("Authorization", "Bearer " + auditorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));
        }

        @Test
        @DisplayName("LENDER should NOT access audit logs")
        void lenderShouldNotAccessAuditLogs() throws Exception {
            mockMvc.perform(get(AUDIT_BASE_URL + "/entity/Borrower/" + testBorrower.getId())
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return empty for non-existent entity")
        void shouldReturnEmptyForNonExistent() throws Exception {
            mockMvc.perform(get(AUDIT_BASE_URL + "/entity/Borrower/" + UUID.randomUUID())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        @DisplayName("Should support pagination")
        void shouldSupportPagination() throws Exception {
            mockMvc.perform(get(AUDIT_BASE_URL + "/entity/Borrower/" + testBorrower.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .param("page", "0")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/audit/user/{userId}")
    class GetAuditLogsByUserTests {

        @Test
        @DisplayName("ADMIN should access user audit logs")
        void adminShouldAccessUserAuditLogs() throws Exception {
            mockMvc.perform(get(AUDIT_BASE_URL + "/user/" + lenderUser.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                    .andExpect(jsonPath("$.content[0].performedBy", is(lenderUser.getEmail())));
        }

        @Test
        @DisplayName("AUDITOR should access user audit logs")
        void auditorShouldAccessUserAuditLogs() throws Exception {
            mockMvc.perform(get(AUDIT_BASE_URL + "/user/" + lenderUser.getId())
                            .header("Authorization", "Bearer " + auditorToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("LENDER should NOT access user audit logs")
        void lenderShouldNotAccessUserAuditLogs() throws Exception {
            mockMvc.perform(get(AUDIT_BASE_URL + "/user/" + lenderUser.getId())
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return empty for user with no actions")
        void shouldReturnEmptyForUserWithNoActions() throws Exception {
            mockMvc.perform(get(AUDIT_BASE_URL + "/user/" + auditorUser.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }
}
