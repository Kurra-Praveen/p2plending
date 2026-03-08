package com.llms.controller;

import com.llms.BaseIntegrationTest;
import com.llms.dto.request.LoanPreviewRequest;
import com.llms.dto.request.SwitchInterestModeRequest;
import com.llms.entity.Borrower;
import com.llms.entity.Loan;
import com.llms.entity.LoanConfiguration;
import com.llms.entity.LoanInterestConfigHistory;
import com.llms.enums.BorrowerStatus;
import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;
import com.llms.enums.LoanFrequency;
import com.llms.enums.LoanStatus;
import com.llms.repository.LoanConfigurationRepository;
import com.llms.repository.LoanInterestConfigHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("LoanConfigurationController Integration Tests")
class LoanConfigurationControllerTest extends BaseIntegrationTest {

    private static final String CONFIG_BASE_URL = "/api/v1/loans";

    @Autowired
    private LoanConfigurationRepository configurationRepository;

    @Autowired
    private LoanInterestConfigHistoryRepository historyRepository;

    private Borrower testBorrower;
    private Loan testLoan;
    private LoanConfiguration testConfig;

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
                .principalAmount(10000000L) // 1 lakh in paise
                .interestRate(BigDecimal.valueOf(12.0))
                .interestType(InterestType.REDUCING)
                .tenureMonths(12)
                .emiAmount(888488L)
                .totalInterest(661856L)
                .totalPayable(10661856L)
                .outstandingPrincipal(10000000L)
                .outstandingInterest(661856L)
                .outstandingPenalty(0L)
                .status(LoanStatus.ACTIVE)
                .createdBy(lenderUser)
                .disbursedAt(LocalDateTime.now().minusDays(30))
                .build();
        testLoan = loanRepository.save(testLoan);

        // Create active history
        LoanInterestConfigHistory history = LoanInterestConfigHistory.builder()
                .loan(testLoan)
                .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                .interestRate(BigDecimal.valueOf(12.0))
                .interestType(InterestType.REDUCING)
                .effectiveFrom(LocalDate.now().minusDays(30))
                .effectiveTo(null)
                .changedBy(lenderUser)
                .changeReason("Initial setup")
                .build();
        history = historyRepository.save(history);

        // Create configuration
        testConfig = LoanConfiguration.builder()
                .loan(testLoan)
                .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                .interestRate(BigDecimal.valueOf(12.0))
                .interestType(InterestType.REDUCING)
                .frequency(LoanFrequency.MONTHLY)
                .tenureMonths(12)
                .timezone("Asia/Kolkata")
                .activeHistory(history)
                .build();
        testConfig = configurationRepository.save(testConfig);
    }

    @Nested
    @DisplayName("POST /api/v1/loans/preview")
    class LoanPreviewTests {

        @Test
        @DisplayName("Should generate loan preview")
        void shouldGeneratePreview() throws Exception {
            LoanPreviewRequest request = LoanPreviewRequest.builder()
                    .principal(10000000L)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .frequency(LoanFrequency.MONTHLY)
                    .tenureMonths(12)
                    .timezone("Asia/Kolkata")
                    .build();

            mockMvc.perform(post(CONFIG_BASE_URL + "/preview")
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.emiAmount", greaterThan(0)))
                    .andExpect(jsonPath("$.totalInterest", greaterThan(0)))
                    .andExpect(jsonPath("$.totalPayable", greaterThan(0)))
                    .andExpect(jsonPath("$.schedule", hasSize(12)));
        }

        @Test
        @DisplayName("Should validate preview request")
        void shouldValidateRequest() throws Exception {
            LoanPreviewRequest request = LoanPreviewRequest.builder()
                    .principal(-100L) // Invalid
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestRateMode(InterestRateMode.ANNUAL_PERCENTAGE)
                    .interestType(InterestType.REDUCING)
                    .frequency(LoanFrequency.MONTHLY)
                    .tenureMonths(12)
                    .build();

            mockMvc.perform(post(CONFIG_BASE_URL + "/preview")
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/loans/{id}/configuration")
    class GetConfigurationTests {

        @Test
        @DisplayName("Should get loan configuration")
        void shouldGetConfiguration() throws Exception {
            mockMvc.perform(get(CONFIG_BASE_URL + "/" + testLoan.getId() + "/configuration")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.loanId", is(testLoan.getId().toString())))
                    .andExpect(jsonPath("$.interestRateMode", is("ANNUAL_PERCENTAGE")))
                    .andExpect(jsonPath("$.frequency", is("MONTHLY")));
        }

        @Test
        @DisplayName("Should return 404 if config missing")
        void shouldReturn404IfMissing() throws Exception {
            Loan otherLoan = Loan.builder()
                    .borrower(testBorrower)
                    .principalAmount(5000L)
                    .interestRate(BigDecimal.TEN)
                    .interestType(InterestType.FLAT)
                    .tenureMonths(1)
                    .emiAmount(5000L)
                    .totalInterest(0L)
                    .totalPayable(5000L)
                    .outstandingPrincipal(5000L)
                    .outstandingInterest(0L)
                    .outstandingPenalty(0L)
                    .outstandingCharges(0L)
                    .status(LoanStatus.CREATED)
                    .createdBy(lenderUser)
                    .build();
            otherLoan = loanRepository.save(otherLoan);

            mockMvc.perform(get(CONFIG_BASE_URL + "/" + otherLoan.getId() + "/configuration")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/loans/{id}/interest-history")
    class GetInterestHistoryTests {

        @Test
        @DisplayName("Should get interest history")
        void shouldGetHistory() throws Exception {
            mockMvc.perform(get(CONFIG_BASE_URL + "/" + testLoan.getId() + "/interest-history")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].interestRateMode", is("ANNUAL_PERCENTAGE")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/loans/{id}/switch-interest-mode")
    class SwitchInterestModeTests {

        @Test
        @DisplayName("Should switch interest mode")
        void shouldSwitchMode() throws Exception {
            LocalDate futureDate = LocalDate.now().plusDays(30);

            SwitchInterestModeRequest request = SwitchInterestModeRequest.builder()
                    .newMode(InterestRateMode.MONTHLY_PERCENTAGE)
                    .newRate(BigDecimal.valueOf(1.5))
                    .effectiveDate(futureDate)
                    .reason("Policy change")
                    .build();

            mockMvc.perform(post(CONFIG_BASE_URL + "/" + testLoan.getId() + "/switch-interest-mode")
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.interestRateMode", is("MONTHLY_PERCENTAGE")))
                    .andExpect(jsonPath("$.interestRate", is(1.5)));

            // Verify history updated
            mockMvc.perform(get(CONFIG_BASE_URL + "/" + testLoan.getId() + "/interest-history")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("Should fail with past date")
        void shouldFailWithPastDate() throws Exception {
            SwitchInterestModeRequest request = SwitchInterestModeRequest.builder()
                    .newMode(InterestRateMode.MONTHLY_PERCENTAGE)
                    .newRate(BigDecimal.valueOf(1.5))
                    .effectiveDate(LocalDate.now().minusDays(1))
                    .reason("Policy change")
                    .build();

            mockMvc.perform(post(CONFIG_BASE_URL + "/" + testLoan.getId() + "/switch-interest-mode")
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
