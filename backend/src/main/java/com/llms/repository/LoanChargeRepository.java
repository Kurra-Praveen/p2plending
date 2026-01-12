package com.llms.repository;

import com.llms.entity.LoanCharge;
import com.llms.enums.ChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanChargeRepository extends JpaRepository<LoanCharge, UUID> {

    List<LoanCharge> findByLoanIdOrderByCreatedAtAsc(UUID loanId);

    List<LoanCharge> findByLoanIdAndStatus(UUID loanId, ChargeStatus status);

    @Query("SELECT lc FROM LoanCharge lc WHERE lc.loan.id = :loanId " +
           "AND lc.status IN ('PENDING', 'PARTIAL') ORDER BY lc.dueDate ASC, lc.createdAt ASC")
    List<LoanCharge> findOutstandingChargesByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT COALESCE(SUM(lc.amount - lc.amountPaid - lc.amountWaived), 0) FROM LoanCharge lc " +
           "WHERE lc.loan.id = :loanId AND lc.status IN ('PENDING', 'PARTIAL')")
    Long sumOutstandingChargesByLoanId(@Param("loanId") UUID loanId);
}
