package com.llms.controller;

import com.llms.BaseIntegrationTest;
import com.llms.dto.request.RecordPaymentRequest;
import com.llms.entity.Borrower;
import com.llms.entity.Loan;
import com.llms.entity.Penalty;
import com.llms.entity.RepaymentSchedule;
import com.llms.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("PaymentController Integration Tests")
class PaymentControllerTest extends BaseIntegrationTest {

    private Borrower testBorrower;
    private Loan activeLoan;

    @BeforeEach
    void setUp() {
        testBorrower = Borrower.builder()
                .fullName("Payment Test Borrower")
                .phone("8888888888")
                .email("payment@test.com")
                .status(BorrowerStatus.ACTIVE)
                .createdBy(lenderUser)
                .build();
        testBorrower = borrowerRepository.save(testBorrower);

        activeLoan = createActiveLoan();
    }

    private Loan createActiveLoan() {
        Loan loan = Loan.builder()
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
                .status(LoanStatus.ACTIVE)
                .disbursedAt(LocalDateTime.now())
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
                    .interestDue(55155L)
                    .totalDue(888488L)
                    .principalPaid(0L)
                    .interestPaid(0L)
                    .penaltyPaid(0L)
                    .status(EmiStatus.PENDING)
                    .build();
            scheduleRepository.save(schedule);
        }

        return loan;
    }

    private String getPaymentUrl(UUID loanId) {
        return "/api/v1/loans/" + loanId + "/payments";
    }

    @Nested
    @DisplayName("POST /api/v1/loans/{loanId}/payments")
    class RecordPaymentTests {

        @Test
        @DisplayName("Should record payment successfully")
        void shouldRecordPaymentSuccessfully() throws Exception {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .amount(888488L) // One EMI
                    .paymentDate(LocalDate.now())
                    .mode(PaymentMode.UPI)
                    .reference("UPI123456")
                    .build();

            mockMvc.perform(post(getPaymentUrl(activeLoan.getId()))
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.amountPaid", is(888488)))
                    .andExpect(jsonPath("$.mode", is("UPI")))
                    .andExpect(jsonPath("$.reference", is("UPI123456")))
                    .andExpect(jsonPath("$.allocations", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("Should record partial payment")
        void shouldRecordPartialPayment() throws Exception {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .amount(500000L) // Less than one EMI
                    .paymentDate(LocalDate.now())
                    .mode(PaymentMode.CASH)
                    .build();

            mockMvc.perform(post(getPaymentUrl(activeLoan.getId()))
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amountPaid", is(500000)));
        }

        @Test
        @DisplayName("Should record advance payment covering multiple EMIs")
        void shouldRecordAdvancePayment() throws Exception {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .amount(2665464L) // Three EMIs
                    .paymentDate(LocalDate.now())
                    .mode(PaymentMode.BANK)
                    .reference("BANK789")
                    .build();

            mockMvc.perform(post(getPaymentUrl(activeLoan.getId()))
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amountPaid", is(2665464)))
                    .andExpect(jsonPath("$.allocations", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("Should allocate payment to penalty first")
        void shouldAllocateTopenaltyFirst() throws Exception {
            // Add penalty to loan
            RepaymentSchedule schedule = scheduleRepository.findByLoanIdOrderByEmiNoAsc(activeLoan.getId()).get(0);
            Penalty penalty = Penalty.builder()
                    .loan(activeLoan)
                    .schedule(schedule)
                    .emiNo(1)
                    .amount(10000L)
                    .daysOverdue(5)
                    .status(PenaltyStatus.UNPAID)
                    .appliedAt(LocalDateTime.now())
                    .build();
            penaltyRepository.save(penalty);

            activeLoan.setOutstandingPenalty(10000L);
            loanRepository.save(activeLoan);

            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .amount(50000L)
                    .paymentDate(LocalDate.now())
                    .mode(PaymentMode.UPI)
                    .build();

            mockMvc.perform(post(getPaymentUrl(activeLoan.getId()))
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.allocations[?(@.type=='PENALTY')]", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("Should fail with duplicate reference")
        void shouldFailWithDuplicateReference() throws Exception {
            // First payment
            RecordPaymentRequest request1 = RecordPaymentRequest.builder()
                    .amount(500000L)
                    .paymentDate(LocalDate.now())
                    .mode(PaymentMode.UPI)
                    .reference("DUPLICATE_REF")
                    .build();

            mockMvc.perform(post(getPaymentUrl(activeLoan.getId()))
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request1)))
                    .andExpect(status().isCreated());

            // Second payment with same reference
            RecordPaymentRequest request2 = RecordPaymentRequest.builder()
                    .amount(300000L)
                    .paymentDate(LocalDate.now())
                    .mode(PaymentMode.UPI)
                    .reference("DUPLICATE_REF")
                    .build();

            mockMvc.perform(post(getPaymentUrl(activeLoan.getId()))
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request2)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("DUPLICATE_REFERENCE")));
        }

        @Test
        @DisplayName("Should fail for non-active loan")
        void shouldFailForNonActiveLoan() throws Exception {
            Loan createdLoan = Loan.builder()
                    .borrower(testBorrower)
                    .principalAmount(5000000L)
                    .interestRate(BigDecimal.valueOf(10.0))
                    .interestType(InterestType.FLAT)
                    .tenureMonths(6)
                    .emiAmount(875000L)
                    .totalInterest(250000L)
                    .totalPayable(5250000L)
                    .outstandingPrincipal(5000000L)
                    .outstandingInterest(250000L)
                    .status(LoanStatus.CREATED) // Not active
                    .createdBy(lenderUser)
                    .build();
            createdLoan = loanRepository.save(createdLoan);

            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .amount(500000L)
                    .paymentDate(LocalDate.now())
                    .mode(PaymentMode.CASH)
                    .build();

            mockMvc.perform(post(getPaymentUrl(createdLoan.getId()))
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode", is("INVALID_STATE")));
        }

        @Test
        @DisplayName("Should fail with zero amount")
        void shouldFailWithZeroAmount() throws Exception {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .amount(0L)
                    .paymentDate(LocalDate.now())
                    .mode(PaymentMode.CASH)
                    .build();

            mockMvc.perform(post(getPaymentUrl(activeLoan.getId()))
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should fail with AUDITOR role")
        void shouldFailWithAuditorRole() throws Exception {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .amount(500000L)
                    .paymentDate(LocalDate.now())
                    .mode(PaymentMode.CASH)
                    .build();

            mockMvc.perform(post(getPaymentUrl(activeLoan.getId()))
                            .header("Authorization", "Bearer " + auditorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should fail for non-existent loan")
        void shouldFailForNonExistentLoan() throws Exception {
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .amount(500000L)
                    .paymentDate(LocalDate.now())
                    .mode(PaymentMode.CASH)
                    .build();

            mockMvc.perform(post(getPaymentUrl(UUID.randomUUID()))
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/loans/{loanId}/payments")
    class GetPaymentsTests {

        @Test
        @DisplayName("Should get payments with pagination")
        void shouldGetPaymentsWithPagination() throws Exception {
            // Record a few payments first
            for (int i = 0; i < 3; i++) {
                RecordPaymentRequest request = RecordPaymentRequest.builder()
                        .amount(300000L)
                        .paymentDate(LocalDate.now())
                        .mode(PaymentMode.CASH)
                        .build();

                mockMvc.perform(post(getPaymentUrl(activeLoan.getId()))
                                .header("Authorization", "Bearer " + lenderToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(asJsonString(request)))
                        .andExpect(status().isCreated());
            }

            mockMvc.perform(get(getPaymentUrl(activeLoan.getId()))
                            .header("Authorization", "Bearer " + lenderToken)
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(3)));
        }

        @Test
        @DisplayName("Should get all payments for a loan")
        void shouldGetAllPayments() throws Exception {
            // Record payments
            RecordPaymentRequest request = RecordPaymentRequest.builder()
                    .amount(500000L)
                    .paymentDate(LocalDate.now())
                    .mode(PaymentMode.UPI)
                    .build();

            mockMvc.perform(post(getPaymentUrl(activeLoan.getId()))
                            .header("Authorization", "Bearer " + lenderToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get(getPaymentUrl(activeLoan.getId()) + "/all")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].loanId", is(activeLoan.getId().toString())));
        }

        @Test
        @DisplayName("AUDITOR should be able to view payments")
        void auditorShouldViewPayments() throws Exception {
            mockMvc.perform(get(getPaymentUrl(activeLoan.getId()))
                            .header("Authorization", "Bearer " + auditorToken))
                    .andExpect(status().isOk());
        }
    }
}
