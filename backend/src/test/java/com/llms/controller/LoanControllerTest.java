package com.llms.controller;

import com.llms.BaseIntegrationTest;
import com.llms.dto.request.CreateLoanRequest;
import com.llms.dto.request.DisburseLoanRequest;
import com.llms.entity.Borrower;
import com.llms.entity.Loan;
import com.llms.entity.RepaymentSchedule;
import com.llms.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("LoanController Integration Tests")
class LoanControllerTest extends BaseIntegrationTest {

    private static final String LOANS_BASE_URL = "/api/v1/loans";

    private Borrower testBorrower;

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
    }

    private Loan createTestLoan(LoanStatus status) {
        Loan loan = Loan.builder()
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
                .status(status)
                .createdBy(lenderUser)
                .build();
        loan = loanRepository.save(loan);

        // Create repayment schedule
        for (int i = 1; i <= 12; i++) {
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .loan(loan)
                    .emiNo(i)
                    .dueDate(LocalDate.now().plusMonths(i))
                    .principalDue(833333L)
                    .interestDue(55154L)
                    .totalDue(888487L)
                    .status(EmiStatus.PENDING)
                    .build();
            scheduleRepository.save(schedule);
        }

        return loan;
    }

    @Nested
    @DisplayName("POST /api/v1/loans")
    class CreateLoanTests {

        @Test
        @DisplayName("Should create loan with REDUCING interest successfully")
        void shouldCreateLoanWithReducingInterest() throws Exception {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(testBorrower.getId())
                    .principal(10000000L) // 1 lakh paise
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestType(InterestType.REDUCING)
                    .tenureMonths(12)
                    .build();

            mockMvc.perform(post(LOANS_BASE_URL)
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.loanId", notNullValue()))
                    .andExpect(jsonPath("$.borrowerId", is(testBorrower.getId().toString())))
                    .andExpect(jsonPath("$.principal", is(10000000)))
                    .andExpect(jsonPath("$.interestType", is("REDUCING")))
                    .andExpect(jsonPath("$.tenureMonths", is(12)))
                    .andExpect(jsonPath("$.emiAmount", greaterThan(0)))
                    .andExpect(jsonPath("$.totalInterest", greaterThan(0)))
                    .andExpect(jsonPath("$.status", is("CREATED")));
        }

        @Test
        @DisplayName("Should create loan with FLAT interest successfully")
        void shouldCreateLoanWithFlatInterest() throws Exception {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(testBorrower.getId())
                    .principal(5000000L)
                    .interestRate(BigDecimal.valueOf(10.0))
                    .interestType(InterestType.FLAT)
                    .tenureMonths(6)
                    .build();

            mockMvc.perform(post(LOANS_BASE_URL)
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.interestType", is("FLAT")))
                    .andExpect(jsonPath("$.status", is("CREATED")));
        }

        @Test
        @DisplayName("Should fail with non-existent borrower")
        void shouldFailWithNonExistentBorrower() throws Exception {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(UUID.randomUUID())
                    .principal(10000000L)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestType(InterestType.REDUCING)
                    .tenureMonths(12)
                    .build();

            mockMvc.perform(post(LOANS_BASE_URL)
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should fail with zero principal")
        void shouldFailWithZeroPrincipal() throws Exception {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(testBorrower.getId())
                    .principal(0L)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestType(InterestType.REDUCING)
                    .tenureMonths(12)
                    .build();

            mockMvc.perform(post(LOANS_BASE_URL)
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail with interest rate over 100%")
        void shouldFailWithHighInterestRate() throws Exception {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(testBorrower.getId())
                    .principal(10000000L)
                    .interestRate(BigDecimal.valueOf(150.0))
                    .interestType(InterestType.REDUCING)
                    .tenureMonths(12)
                    .build();

            mockMvc.perform(post(LOANS_BASE_URL)
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail with AUDITOR role")
        void shouldFailWithAuditorRole() throws Exception {
            CreateLoanRequest request = CreateLoanRequest.builder()
                    .borrowerId(testBorrower.getId())
                    .principal(10000000L)
                    .interestRate(BigDecimal.valueOf(12.0))
                    .interestType(InterestType.REDUCING)
                    .tenureMonths(12)
                    .build();

            mockMvc.perform(post(LOANS_BASE_URL)
                            .header("Authorization", "Bearer " + auditorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/loans/{id}/disburse")
    class DisburseLoanTests {

        @Test
        @DisplayName("Should disburse loan successfully")
        void shouldDisburseLoan() throws Exception {
            Loan loan = createTestLoan(LoanStatus.CREATED);

            DisburseLoanRequest request = DisburseLoanRequest.builder()
                    .amount(10000000L)
                    .mode(PaymentMode.BANK)
                    .reference("TXN123456")
                    .build();

            mockMvc.perform(post(LOANS_BASE_URL + "/" + loan.getId() + "/disburse")
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ACTIVE")))
                    .andExpect(jsonPath("$.disbursedAt", notNullValue()));
        }

        @Test
        @DisplayName("Should fail to disburse already active loan")
        void shouldFailToDisbursActiveLoan() throws Exception {
            Loan loan = createTestLoan(LoanStatus.ACTIVE);

            DisburseLoanRequest request = DisburseLoanRequest.builder()
                    .amount(10000000L)
                    .mode(PaymentMode.BANK)
                    .reference("TXN789")
                    .build();

            mockMvc.perform(post(LOANS_BASE_URL + "/" + loan.getId() + "/disburse")
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("INVALID_STATE")));
        }

        @Test
        @DisplayName("Should fail with mismatched disbursement amount")
        void shouldFailWithMismatchedAmount() throws Exception {
            Loan loan = createTestLoan(LoanStatus.CREATED);

            DisburseLoanRequest request = DisburseLoanRequest.builder()
                    .amount(5000000L) // Different from principal
                    .mode(PaymentMode.BANK)
                    .reference("TXN999")
                    .build();

            mockMvc.perform(post(LOANS_BASE_URL + "/" + loan.getId() + "/disburse")
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("AMOUNT_MISMATCH")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/loans/{id}")
    class GetLoanTests {

        @Test
        @DisplayName("Should get loan by ID")
        void shouldGetLoanById() throws Exception {
            Loan loan = createTestLoan(LoanStatus.ACTIVE);

            mockMvc.perform(get(LOANS_BASE_URL + "/" + loan.getId())
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.loanId", is(loan.getId().toString())))
                    .andExpect(jsonPath("$.borrowerName", is("Test Borrower")))
                    .andExpect(jsonPath("$.principal", is(10000000)))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("Should return 404 for non-existent loan")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get(LOANS_BASE_URL + "/" + UUID.randomUUID())
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("AUDITOR should be able to read loan")
        void auditorShouldReadLoan() throws Exception {
            Loan loan = createTestLoan(LoanStatus.ACTIVE);

            mockMvc.perform(get(LOANS_BASE_URL + "/" + loan.getId())
                            .header("Authorization", "Bearer " + auditorToken))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/loans")
    class GetAllLoansTests {

        @Test
        @DisplayName("Should get all loans with pagination")
        void shouldGetAllLoansWithPagination() throws Exception {
            createTestLoan(LoanStatus.ACTIVE);
            createTestLoan(LoanStatus.CREATED);

            mockMvc.perform(get(LOANS_BASE_URL)
                            .header("Authorization", "Bearer " + lenderToken)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))))
                    .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(2)));
        }

        @Test
        @DisplayName("Should get loans by status")
        void shouldGetLoansByStatus() throws Exception {
            createTestLoan(LoanStatus.ACTIVE);
            createTestLoan(LoanStatus.CREATED);

            mockMvc.perform(get(LOANS_BASE_URL + "/status/ACTIVE")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].status", everyItem(is("ACTIVE"))));
        }

        @Test
        @DisplayName("Should get loans by borrower")
        void shouldGetLoansByBorrower() throws Exception {
            createTestLoan(LoanStatus.ACTIVE);

            mockMvc.perform(get(LOANS_BASE_URL + "/borrower/" + testBorrower.getId())
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].borrowerId",
                            everyItem(is(testBorrower.getId().toString()))));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/loans/{id}/schedule")
    class GetRepaymentScheduleTests {

        @Test
        @DisplayName("Should get repayment schedule")
        void shouldGetRepaymentSchedule() throws Exception {
            Loan loan = createTestLoan(LoanStatus.ACTIVE);

            mockMvc.perform(get(LOANS_BASE_URL + "/" + loan.getId() + "/schedule")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(12)))
                    .andExpect(jsonPath("$[0].emiNo", is(1)))
                    .andExpect(jsonPath("$[0].principalDue", greaterThan(0)))
                    .andExpect(jsonPath("$[0].interestDue", greaterThan(0)))
                    .andExpect(jsonPath("$[0].status", is("PENDING")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/loans/{id}/close")
    class CloseLoanTests {

        @Test
        @DisplayName("Should fail to close loan with outstanding balance")
        void shouldFailToCloseWithOutstandingBalance() throws Exception {
            Loan loan = createTestLoan(LoanStatus.ACTIVE);

            mockMvc.perform(post(LOANS_BASE_URL + "/" + loan.getId() + "/close")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("OUTSTANDING_BALANCE")));
        }

        @Test
        @DisplayName("Should close loan with zero balance")
        void shouldCloseLoanWithZeroBalance() throws Exception {
            Loan loan = createTestLoan(LoanStatus.ACTIVE);
            loan.setOutstandingPrincipal(0L);
            loan.setOutstandingInterest(0L);
            loan.setOutstandingPenalty(0L);
            loanRepository.save(loan);

            mockMvc.perform(post(LOANS_BASE_URL + "/" + loan.getId() + "/close")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isNoContent());

            // Verify loan is closed
            mockMvc.perform(get(LOANS_BASE_URL + "/" + loan.getId())
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(jsonPath("$.status", is("CLOSED")));
        }
    }
}
