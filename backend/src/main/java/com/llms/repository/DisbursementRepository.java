package com.llms.repository;

import com.llms.entity.Disbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisbursementRepository extends JpaRepository<Disbursement, UUID> {

    List<Disbursement> findByLoanId(UUID loanId);

    @Query("SELECT COALESCE(SUM(d.amount), 0) FROM Disbursement d WHERE d.loan.id = :loanId")
    Long sumDisbursementsByLoanId(@Param("loanId") UUID loanId);
}
