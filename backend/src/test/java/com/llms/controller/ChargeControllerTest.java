package com.llms.controller;

import com.llms.BaseIntegrationTest;
import com.llms.dto.request.CreateChargeDefinitionRequest;
import com.llms.entity.Borrower;
import com.llms.entity.ChargeDefinition;
import com.llms.entity.Loan;
import com.llms.entity.LoanCharge;
import com.llms.enums.*;
import com.llms.repository.ChargeDefinitionRepository;
import com.llms.repository.LoanChargeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ChargeController Integration Tests")
class ChargeControllerTest extends BaseIntegrationTest {

    private static final String CHARGE_DEF_URL = "/api/v1/charge-definitions";
    private static final String LOANS_URL = "/api/v1/loans";
    private static final String CHARGES_URL = "/api/v1/charges";

    @Autowired
    private ChargeDefinitionRepository chargeDefinitionRepository;

    @Autowired
    private LoanChargeRepository loanChargeRepository;

    private Borrower testBorrower;
    private Loan testLoan;

    @BeforeEach
    void setUp() {
        testBorrower = Borrower.builder()
                .fullName("Test Borrower")
                .phone("9876543210")
                .email("borrower@test.com")
                .address("Test Address")
                .riskScore(650)
                .status(BorrowerStatus.ACTIVE)
                .createdBy(lenderUser)
                .build();
        testBorrower = borrowerRepository.save(testBorrower);

        testLoan = Loan.builder()
                .borrower(testBorrower)
                .principalAmount(10000000L)
                .interestRate(BigDecimal.valueOf(12.0))
                .interestType(InterestType.REDUCING)
                .tenureMonths(12)
                .emiAmount(888488L)
                .totalInterest(661856L)
                .totalPayable(10661856L)
                .outstandingPrincipal(10000000L)
                .outstandingInterest(661856L)
                .outstandingPenalty(0L)
                .outstandingCharges(0L)
                .status(LoanStatus.ACTIVE)
                .createdBy(lenderUser)
                .build();
        testLoan = loanRepository.save(testLoan);
    }

    @Nested
    @DisplayName("POST /api/v1/charge-definitions")
    class CreateChargeDefinitionTests {

        @Test
        @DisplayName("Should create charge definition successfully")
        void shouldCreateChargeDefinition() throws Exception {
            CreateChargeDefinitionRequest request = CreateChargeDefinitionRequest.builder()
                    .name("Processing Fee")
                    .code("PROCESSING_FEE_TEST")
                    .description("Standard processing fee")
                    .chargeType(ChargeType.PROCESSING_FEE)
                    .calculationType(ChargeCalculationType.PERCENTAGE_OF_PRINCIPAL)
                    .percentage(BigDecimal.valueOf(1.5))
                    .applicationTiming(ChargeApplicationTiming.DISBURSEMENT)
                    .isMandatory(true)
                    .build();

            mockMvc.perform(post(CHARGE_DEF_URL)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is("Processing Fee")))
                    .andExpect(jsonPath("$.code", is("PROCESSING_FEE_TEST")))
                    .andExpect(jsonPath("$.isActive", is(true)));
        }

        @Test
        @DisplayName("Should fail with lender token")
        void shouldFailWithLenderToken() throws Exception {
            CreateChargeDefinitionRequest request = CreateChargeDefinitionRequest.builder()
                    .name("Processing Fee")
                    .code("PROCESSING_FEE_TEST_2")
                    .chargeType(ChargeType.PROCESSING_FEE)
                    .calculationType(ChargeCalculationType.FIXED)
                    .amount(1000L)
                    .applicationTiming(ChargeApplicationTiming.DISBURSEMENT)
                    .build();

            mockMvc.perform(post(CHARGE_DEF_URL)
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should validate request")
        void shouldValidateRequest() throws Exception {
            CreateChargeDefinitionRequest request = CreateChargeDefinitionRequest.builder()
                    .name("") // Invalid
                    .code("invalid code") // Invalid format
                    .build();

            mockMvc.perform(post(CHARGE_DEF_URL)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/charge-definitions")
    class GetChargeDefinitionsTests {

        @Test
        @DisplayName("Should get active charge definitions")
        void shouldGetActiveDefinitions() throws Exception {
            // Create a definition directly
            ChargeDefinition def = ChargeDefinition.builder()
                    .name("Active Charge")
                    .code("ACTIVE_CHARGE")
                    .chargeType(ChargeType.CUSTOM)
                    .calculationType(ChargeCalculationType.FIXED)
                    .amount(500L)
                    .applicationTiming(ChargeApplicationTiming.DISBURSEMENT)
                    .isActive(true)
                    .createdBy(lenderUser)
                    .build();
            chargeDefinitionRepository.save(def);

            mockMvc.perform(get(CHARGE_DEF_URL)
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[*].code", hasItem("ACTIVE_CHARGE")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/loans/{loanId}/charges")
    class GetLoanChargesTests {

        @Test
        @DisplayName("Should get charges for loan")
        void shouldGetLoanCharges() throws Exception {
            // Add a charge to the loan
            LoanCharge charge = LoanCharge.builder()
                    .loan(testLoan)
                    .name("Test Charge")
                    .chargeType(ChargeType.PROCESSING_FEE)
                    .amount(1000L)
                    .amountPaid(0L)
                    .amountWaived(0L)
                    .status(ChargeStatus.PENDING)
                    .dueDate(LocalDate.now())
                    .build();
            loanChargeRepository.save(charge);

            mockMvc.perform(get(LOANS_URL + "/" + testLoan.getId() + "/charges")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Test Charge")))
                    .andExpect(jsonPath("$[0].amount", is(1000)));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/charges/{id}/waive")
    class WaiveChargeTests {

        @Test
        @DisplayName("Should waive charge successfully")
        void shouldWaiveCharge() throws Exception {
            // Add a charge to the loan
            LoanCharge charge = LoanCharge.builder()
                    .loan(testLoan)
                    .name("Waive Me")
                    .chargeType(ChargeType.CUSTOM)
                    .amount(5000L)
                    .amountPaid(0L)
                    .amountWaived(0L)
                    .status(ChargeStatus.PENDING)
                    .dueDate(LocalDate.now())
                    .build();
            charge = loanChargeRepository.save(charge);

            Map<String, String> request = Map.of("reason", "Customer complaint");

            mockMvc.perform(post(CHARGES_URL + "/" + charge.getId() + "/waive")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("WAIVED")))
                    .andExpect(jsonPath("$.amountWaived", is(5000)))
                    .andExpect(jsonPath("$.waiverReason", is("Customer complaint")));
        }

        @Test
        @DisplayName("Should fail with lender token")
        void shouldFailWithLenderToken() throws Exception {
            // Add a charge to the loan
            LoanCharge charge = LoanCharge.builder()
                    .loan(testLoan)
                    .name("Waive Me")
                    .chargeType(ChargeType.CUSTOM)
                    .amount(5000L)
                    .amountPaid(0L)
                    .amountWaived(0L)
                    .status(ChargeStatus.PENDING)
                    .dueDate(LocalDate.now())
                    .build();
            charge = loanChargeRepository.save(charge);

            Map<String, String> request = Map.of("reason", "Customer complaint");

            mockMvc.perform(post(CHARGES_URL + "/" + charge.getId() + "/waive")
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
