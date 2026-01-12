package com.llms.repository;

import com.llms.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findByLoanId(UUID loanId, Pageable pageable);

    List<Payment> findByLoanIdOrderByPaymentDateDesc(UUID loanId);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM Payment p WHERE p.loan.id = :loanId")
    Long sumPaymentsByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    Long sumPaymentsBetweenDates(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    boolean existsByReference(String reference);

    // ========== LENDER-FILTERED QUERIES (Data Isolation Fix) ==========

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate AND p.loan.createdBy.id = :userId")
    Long sumPaymentsBetweenDatesByCreatedBy(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("userId") UUID userId);
}
