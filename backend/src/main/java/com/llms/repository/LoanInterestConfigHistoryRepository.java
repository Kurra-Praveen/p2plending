package com.llms.repository;

import com.llms.entity.LoanInterestConfigHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanInterestConfigHistoryRepository extends JpaRepository<LoanInterestConfigHistory, UUID> {

    List<LoanInterestConfigHistory> findByLoanIdOrderByEffectiveFromDesc(UUID loanId);

    @Query("SELECT h FROM LoanInterestConfigHistory h WHERE h.loan.id = :loanId AND h.effectiveTo IS NULL")
    Optional<LoanInterestConfigHistory> findActiveByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT h FROM LoanInterestConfigHistory h WHERE h.loan.id = :loanId " +
           "AND h.effectiveFrom <= :date AND (h.effectiveTo IS NULL OR h.effectiveTo >= :date)")
    Optional<LoanInterestConfigHistory> findActiveOnDate(@Param("loanId") UUID loanId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(h) > 0 FROM LoanInterestConfigHistory h WHERE h.loan.id = :loanId AND h.effectiveTo IS NULL")
    boolean hasActiveConfiguration(@Param("loanId") UUID loanId);
}
