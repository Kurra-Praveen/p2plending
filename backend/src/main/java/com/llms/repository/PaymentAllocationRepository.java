package com.llms.repository;

import com.llms.entity.PaymentAllocation;
import com.llms.enums.AllocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, UUID> {

    List<PaymentAllocation> findByPaymentId(UUID paymentId);

    @Query("SELECT COALESCE(SUM(pa.amount), 0) FROM PaymentAllocation pa WHERE pa.payment.loan.id = :loanId AND pa.type = :type")
    Long sumAllocationsByLoanAndType(@Param("loanId") UUID loanId, @Param("type") AllocationType type);

    // ========== LENDER-FILTERED QUERIES (Data Isolation Fix) ==========

    @Query("SELECT COALESCE(SUM(pa.amount), 0) FROM PaymentAllocation pa WHERE pa.type = :type AND pa.payment.loan.createdBy.id = :userId")
    Long sumAllocationsByTypeAndCreatedBy(@Param("type") AllocationType type, @Param("userId") UUID userId);
}
