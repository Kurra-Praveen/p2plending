package com.llms.controller;

import com.llms.BaseIntegrationTest;
import com.llms.dto.request.CreateBorrowerRequest;
import com.llms.dto.request.UpdateBorrowerRequest;
import com.llms.entity.Borrower;
import com.llms.enums.BorrowerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("BorrowerController Integration Tests")
class BorrowerControllerTest extends BaseIntegrationTest {

    private static final String BORROWERS_BASE_URL = "/api/v1/borrowers";

    private Borrower createTestBorrower(String phone) {
        Borrower borrower = Borrower.builder()
                .fullName("Test Borrower")
                .phone(phone)
                .email("borrower@test.com")
                .address("Test Address")
                .riskScore(650)
                .status(BorrowerStatus.ACTIVE)
                .createdBy(lenderUser)
                .build();
        return borrowerRepository.save(borrower);
    }

    @Nested
    @DisplayName("POST /api/v1/borrowers")
    class CreateBorrowerTests {

        @Test
        @DisplayName("Should create borrower successfully with LENDER role")
        void shouldCreateBorrowerWithLenderRole() throws Exception {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("Ravi Kumar")
                    .phone("9876543210")
                    .email("ravi@test.com")
                    .address("Bangalore")
                    .riskScore(700)
                    .build();

            mockMvc.perform(post(BORROWERS_BASE_URL)
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.fullName", is("Ravi Kumar")))
                    .andExpect(jsonPath("$.phone", is("9876543210")))
                    .andExpect(jsonPath("$.email", is("ravi@test.com")))
                    .andExpect(jsonPath("$.riskScore", is(700)))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("Should create borrower successfully with ADMIN role")
        void shouldCreateBorrowerWithAdminRole() throws Exception {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("Admin Created Borrower")
                    .phone("9876543211")
                    .build();

            mockMvc.perform(post(BORROWERS_BASE_URL)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fullName", is("Admin Created Borrower")));
        }

        @Test
        @DisplayName("Should fail to create borrower with AUDITOR role")
        void shouldFailWithAuditorRole() throws Exception {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("Auditor Borrower")
                    .phone("9876543212")
                    .build();

            mockMvc.perform(post(BORROWERS_BASE_URL)
                            .header("Authorization", "Bearer " + auditorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("No Auth Borrower")
                    .phone("9876543213")
                    .build();

            mockMvc.perform(post(BORROWERS_BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should fail with duplicate phone number")
        void shouldFailWithDuplicatePhone() throws Exception {
            createTestBorrower("9999999999");

            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("Duplicate Phone Borrower")
                    .phone("9999999999")
                    .build();

            mockMvc.perform(post(BORROWERS_BASE_URL)
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode", is("DUPLICATE")));
        }

        @Test
        @DisplayName("Should fail with missing required fields")
        void shouldFailWithMissingFields() throws Exception {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .email("onlyemail@test.com")
                    .build();

            mockMvc.perform(post(BORROWERS_BASE_URL)
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
        }

        @Test
        @DisplayName("Should fail with negative risk score")
        void shouldFailWithNegativeRiskScore() throws Exception {
            CreateBorrowerRequest request = CreateBorrowerRequest.builder()
                    .fullName("Negative Score")
                    .phone("9876543214")
                    .riskScore(-100)
                    .build();

            mockMvc.perform(post(BORROWERS_BASE_URL)
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/borrowers/{id}")
    class GetBorrowerTests {

        @Test
        @DisplayName("Should get borrower by ID with LENDER role")
        void shouldGetBorrowerWithLenderRole() throws Exception {
            Borrower borrower = createTestBorrower("1111111111");

            mockMvc.perform(get(BORROWERS_BASE_URL + "/" + borrower.getId())
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(borrower.getId().toString())))
                    .andExpect(jsonPath("$.fullName", is("Test Borrower")));
        }

        @Test
        @DisplayName("Should get borrower with AUDITOR role (read-only)")
        void shouldGetBorrowerWithAuditorRole() throws Exception {
            Borrower borrower = createTestBorrower("2222222222");

            mockMvc.perform(get(BORROWERS_BASE_URL + "/" + borrower.getId())
                            .header("Authorization", "Bearer " + auditorToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(borrower.getId().toString())));
        }

        @Test
        @DisplayName("Should return 404 for non-existent borrower")
        void shouldReturn404ForNonExistent() throws Exception {
            UUID randomId = UUID.randomUUID();

            mockMvc.perform(get(BORROWERS_BASE_URL + "/" + randomId)
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode", is("NOT_FOUND")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/borrowers")
    class GetAllBorrowersTests {

        @Test
        @DisplayName("Should get all borrowers with pagination")
        void shouldGetAllBorrowersWithPagination() throws Exception {
            createTestBorrower("3333333333");
            createTestBorrower("4444444444");
            createTestBorrower("5555555555");

            mockMvc.perform(get(BORROWERS_BASE_URL)
                            .header("Authorization", "Bearer " + lenderToken)
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(3)));
        }

        @Test
        @DisplayName("Should get borrowers by status")
        void shouldGetBorrowersByStatus() throws Exception {
            createTestBorrower("6666666666");
            Borrower blockedBorrower = createTestBorrower("7777777777");
            blockedBorrower.setStatus(BorrowerStatus.BLOCKED);
            borrowerRepository.save(blockedBorrower);

            mockMvc.perform(get(BORROWERS_BASE_URL + "/status/ACTIVE")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].status", everyItem(is("ACTIVE"))));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/borrowers/{id}")
    class UpdateBorrowerTests {

        @Test
        @DisplayName("Should update borrower successfully")
        void shouldUpdateBorrower() throws Exception {
            Borrower borrower = createTestBorrower("8888888888");

            UpdateBorrowerRequest request = UpdateBorrowerRequest.builder()
                    .fullName("Updated Name")
                    .email("updated@test.com")
                    .address("New Address")
                    .riskScore(800)
                    .build();

            mockMvc.perform(put(BORROWERS_BASE_URL + "/" + borrower.getId())
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName", is("Updated Name")))
                    .andExpect(jsonPath("$.email", is("updated@test.com")))
                    .andExpect(jsonPath("$.address", is("New Address")))
                    .andExpect(jsonPath("$.riskScore", is(800)));
        }

        @Test
        @DisplayName("Should fail update with AUDITOR role")
        void shouldFailUpdateWithAuditorRole() throws Exception {
            Borrower borrower = createTestBorrower("9999999998");

            UpdateBorrowerRequest request = UpdateBorrowerRequest.builder()
                    .fullName("Auditor Update")
                    .build();

            mockMvc.perform(put(BORROWERS_BASE_URL + "/" + borrower.getId())
                            .header("Authorization", "Bearer " + auditorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/borrowers/{id}/block")
    class BlockBorrowerTests {

        @Test
        @DisplayName("Should block borrower successfully")
        void shouldBlockBorrower() throws Exception {
            Borrower borrower = createTestBorrower("1010101010");

            mockMvc.perform(post(BORROWERS_BASE_URL + "/" + borrower.getId() + "/block")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isNoContent());

            // Verify borrower is blocked
            mockMvc.perform(get(BORROWERS_BASE_URL + "/" + borrower.getId())
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("BLOCKED")));
        }

        @Test
        @DisplayName("Should unblock borrower successfully")
        void shouldUnblockBorrower() throws Exception {
            Borrower borrower = createTestBorrower("1212121212");
            borrower.setStatus(BorrowerStatus.BLOCKED);
            borrowerRepository.save(borrower);

            mockMvc.perform(post(BORROWERS_BASE_URL + "/" + borrower.getId() + "/unblock")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(BORROWERS_BASE_URL + "/" + borrower.getId())
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/borrowers/{id}")
    class DeleteBorrowerTests {

        @Test
        @DisplayName("Should soft delete borrower with ADMIN role")
        void shouldSoftDeleteBorrower() throws Exception {
            Borrower borrower = createTestBorrower("1313131313");

            mockMvc.perform(delete(BORROWERS_BASE_URL + "/" + borrower.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            // Verify borrower is not returned
            mockMvc.perform(get(BORROWERS_BASE_URL + "/" + borrower.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should fail delete with LENDER role")
        void shouldFailDeleteWithLenderRole() throws Exception {
            Borrower borrower = createTestBorrower("1414141414");

            mockMvc.perform(delete(BORROWERS_BASE_URL + "/" + borrower.getId())
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isForbidden());
        }
    }
}
