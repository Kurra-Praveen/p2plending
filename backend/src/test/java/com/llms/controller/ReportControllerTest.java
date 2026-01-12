package com.llms.controller;

import com.llms.BaseIntegrationTest;
import com.llms.entity.Borrower;
import com.llms.entity.Loan;
import com.llms.entity.Payment;
import com.llms.entity.RepaymentSchedule;
import com.llms.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ReportController Integration Tests")
class ReportControllerTest extends BaseIntegrationTest {

    private static final String REPORTS_BASE_URL = "/api/v1/reports";

    private Borrower testBorrower;
    private Loan activeLoan;
    private Loan closedLoan;

    @BeforeEach
    void setUp() {
        testBorrower = Borrower.builder()
                .fullName("Report Test Borrower")
                .phone("7777777777")
                .email("report@test.com")
                .status(BorrowerStatus.ACTIVE)
                .createdBy(lenderUser)
                .build();
        testBorrower = borrowerRepository.save(testBorrower);

        activeLoan = createLoanWithStatus(LoanStatus.ACTIVE, 10000000L);
        closedLoan = createLoanWithStatus(LoanStatus.CLOSED, 5000000L);
    }

    private Loan createLoanWithStatus(LoanStatus status, Long principal) {
        Loan loan = Loan.builder()
                .borrower(testBorrower)
                .principalAmount(principal)
                .interestRate(BigDecimal.valueOf(12.0))
                .interestType(InterestType.REDUCING)
                .tenureMonths(12)
                .emiAmount(principal / 12 + 5000)
                .totalInterest(60000L)
                .totalPayable(principal + 60000L)
                .outstandingPrincipal(status == LoanStatus.CLOSED ? 0L : principal)
                .outstandingInterest(status == LoanStatus.CLOSED ? 0L : 60000L)
                .outstandingPenalty(0L)
                .status(status)
                .disbursedAt(LocalDateTime.now().minusMonths(1))
                .closedAt(status == LoanStatus.CLOSED ? LocalDateTime.now() : null)
                .createdBy(lenderUser)
                .build();
        loan = loanRepository.save(loan);

        // Create schedule
        for (int i = 1; i <= 12; i++) {
            EmiStatus emiStatus = status == LoanStatus.CLOSED ? EmiStatus.PAID : EmiStatus.PENDING;
            RepaymentSchedule schedule = RepaymentSchedule.builder()
                    .loan(loan)
                    .emiNo(i)
                    .dueDate(LocalDate.now().plusMonths(i - 1))
                    .principalDue(principal / 12)
                    .interestDue(5000L)
                    .totalDue(principal / 12 + 5000L)
                    .principalPaid(status == LoanStatus.CLOSED ? principal / 12 : 0L)
                    .interestPaid(status == LoanStatus.CLOSED ? 5000L : 0L)
                    .status(emiStatus)
                    .build();
            scheduleRepository.save(schedule);
        }

        return loan;
    }

    @Nested
    @DisplayName("GET /api/v1/reports/portfolio")
    class PortfolioSummaryTests {

        @Test
        @DisplayName("Should get portfolio summary")
        void shouldGetPortfolioSummary() throws Exception {
            mockMvc.perform(get(REPORTS_BASE_URL + "/portfolio")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDisbursed", greaterThan(0)))
                    .andExpect(jsonPath("$.outstanding", greaterThanOrEqualTo(0)))
                    .andExpect(jsonPath("$.activeLoans", greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.closedLoans", greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$.totalBorrowers", greaterThanOrEqualTo(1)));
        }

        @Test
        @DisplayName("ADMIN should access portfolio summary")
        void adminShouldAccessPortfolio() throws Exception {
            mockMvc.perform(get(REPORTS_BASE_URL + "/portfolio")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("AUDITOR should access portfolio summary")
        void auditorShouldAccessPortfolio() throws Exception {
            mockMvc.perform(get(REPORTS_BASE_URL + "/portfolio")
                            .header("Authorization", "Bearer " + auditorToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should fail without authentication")
        void shouldFailWithoutAuth() throws Exception {
            mockMvc.perform(get(REPORTS_BASE_URL + "/portfolio"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reports/loans/{loanId}/statement")
    class LoanStatementTests {

        @Test
        @DisplayName("Should get loan statement")
        void shouldGetLoanStatement() throws Exception {
            mockMvc.perform(get(REPORTS_BASE_URL + "/loans/" + activeLoan.getId() + "/statement")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.loanId", is(activeLoan.getId().toString())))
                    .andExpect(jsonPath("$.borrowerName", is("Report Test Borrower")))
                    .andExpect(jsonPath("$.principal", is(10000000)))
                    .andExpect(jsonPath("$.schedule", hasSize(12)))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("Should get statement with payments")
        void shouldGetStatementWithPayments() throws Exception {
            // Add a payment
            Payment payment = Payment.builder()
                    .loan(activeLoan)
                    .amountPaid(500000L)
                    .paymentDate(LocalDate.now())
                    .mode(PaymentMode.UPI)
                    .reference("STMT_TEST")
                    .createdBy(lenderUser)
                    .build();
            paymentRepository.save(payment);

            mockMvc.perform(get(REPORTS_BASE_URL + "/loans/" + activeLoan.getId() + "/statement")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.payments", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$.totalPaid", greaterThan(0)));
        }

        @Test
        @DisplayName("Should return 404 for non-existent loan")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get(REPORTS_BASE_URL + "/loans/" + java.util.UUID.randomUUID() + "/statement")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reports/collections")
    class CollectionsSummaryTests {

        @Test
        @DisplayName("Should get collections summary for date range")
        void shouldGetCollectionsSummary() throws Exception {
            // Add payment for collections
            Payment payment = Payment.builder()
                    .loan(activeLoan)
                    .amountPaid(1000000L)
                    .paymentDate(LocalDate.now())
                    .mode(PaymentMode.BANK)
                    .createdBy(lenderUser)
                    .build();
            paymentRepository.save(payment);

            String startDate = LocalDate.now().minusDays(7).toString();
            String endDate = LocalDate.now().plusDays(1).toString();

            mockMvc.perform(get(REPORTS_BASE_URL + "/collections")
                            .header("Authorization", "Bearer " + lenderToken)
                            .param("startDate", startDate)
                            .param("endDate", endDate))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.startDate", is(startDate)))
                    .andExpect(jsonPath("$.endDate", is(endDate)))
                    .andExpect(jsonPath("$.totalCollected", greaterThanOrEqualTo(0)));
        }

        @Test
        @DisplayName("Should fail without date parameters")
        void shouldFailWithoutDates() throws Exception {
            mockMvc.perform(get(REPORTS_BASE_URL + "/collections")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reports/overdue")
    class OverdueLoansTests {

        @Test
        @DisplayName("Should get overdue loans list")
        void shouldGetOverdueLoans() throws Exception {
            // Create an overdue loan
            Loan overdueLoan = Loan.builder()
                    .borrower(testBorrower)
                    .principalAmount(3000000L)
                    .interestRate(BigDecimal.valueOf(15.0))
                    .interestType(InterestType.FLAT)
                    .tenureMonths(6)
                    .emiAmount(550000L)
                    .totalInterest(300000L)
                    .totalPayable(3300000L)
                    .outstandingPrincipal(3000000L)
                    .outstandingInterest(300000L)
                    .outstandingPenalty(5000L)
                    .status(LoanStatus.ACTIVE)
                    .disbursedAt(LocalDateTime.now().minusMonths(2))
                    .createdBy(lenderUser)
                    .build();
            overdueLoan = loanRepository.save(overdueLoan);

            // Create overdue schedule
            RepaymentSchedule overdueSchedule = RepaymentSchedule.builder()
                    .loan(overdueLoan)
                    .emiNo(1)
                    .dueDate(LocalDate.now().minusDays(10)) // Overdue
                    .principalDue(500000L)
                    .interestDue(50000L)
                    .totalDue(550000L)
                    .principalPaid(0L)
                    .interestPaid(0L)
                    .status(EmiStatus.OVERDUE)
                    .build();
            scheduleRepository.save(overdueSchedule);

            mockMvc.perform(get(REPORTS_BASE_URL + "/overdue")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].daysOverdue", greaterThan(0)))
                    .andExpect(jsonPath("$[0].totalOutstanding", greaterThan(0)));
        }

        @Test
        @DisplayName("Should return empty list when no overdue loans")
        void shouldReturnEmptyWhenNoOverdue() throws Exception {
            // Clear all loans first for this test
            scheduleRepository.deleteAll();
            loanRepository.deleteAll();

            mockMvc.perform(get(REPORTS_BASE_URL + "/overdue")
                            .header("Authorization", "Bearer " + lenderToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("AUDITOR should access overdue report")
        void auditorShouldAccessOverdue() throws Exception {
            mockMvc.perform(get(REPORTS_BASE_URL + "/overdue")
                            .header("Authorization", "Bearer " + auditorToken))
                    .andExpect(status().isOk());
        }
    }
}
