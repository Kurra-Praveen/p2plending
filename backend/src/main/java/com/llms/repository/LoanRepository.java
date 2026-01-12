package com.llms.repository;

import com.llms.entity.Loan;
import com.llms.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<Loan, UUID> {

    Page<Loan> findByBorrowerId(UUID borrowerId, Pageable pageable);

    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    List<Loan> findByStatus(LoanStatus status);

    @Query("SELECT l FROM Loan l WHERE l.status IN :statuses")
    List<Loan> findByStatusIn(@Param("statuses") List<LoanStatus> statuses);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = :status")
    long countByStatus(@Param("status") LoanStatus status);

    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l WHERE l.status = 'ACTIVE'")
    Long sumDisbursedPrincipal();

    @Query("SELECT COALESCE(SUM(l.outstandingPrincipal + l.outstandingInterest + l.outstandingPenalty), 0) FROM Loan l WHERE l.status = 'ACTIVE'")
    Long sumTotalOutstanding();

    @Query("SELECT COALESCE(SUM(l.outstandingPrincipal), 0) FROM Loan l WHERE l.status = 'ACTIVE'")
    Long sumOutstandingPrincipal();

    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l WHERE l.status IN ('ACTIVE', 'CLOSED')")
    Long sumTotalDisbursed();

    // ========== LENDER-FILTERED QUERIES (Data Isolation Fix) ==========

    @Query("SELECT l FROM Loan l WHERE l.id = :id AND l.createdBy.id = :userId")
    Optional<Loan> findByIdAndCreatedBy(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT l FROM Loan l WHERE l.createdBy.id = :userId")
    Page<Loan> findAllByCreatedBy(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.status = :status AND l.createdBy.id = :userId")
    Page<Loan> findByStatusAndCreatedBy(@Param("status") LoanStatus status, @Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.status = :status AND l.createdBy.id = :userId")
    List<Loan> findByStatusAndCreatedBy(@Param("status") LoanStatus status, @Param("userId") UUID userId);

    @Query("SELECT l FROM Loan l WHERE l.status IN :statuses AND l.createdBy.id = :userId")
    List<Loan> findByStatusInAndCreatedBy(@Param("statuses") List<LoanStatus> statuses, @Param("userId") UUID userId);

    @Query("SELECT l FROM Loan l WHERE l.borrower.id = :borrowerId AND l.createdBy.id = :userId")
    Page<Loan> findByBorrowerIdAndCreatedBy(@Param("borrowerId") UUID borrowerId, @Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.status = :status AND l.createdBy.id = :userId")
    long countByStatusAndCreatedBy(@Param("status") LoanStatus status, @Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l WHERE l.status = 'ACTIVE' AND l.createdBy.id = :userId")
    Long sumDisbursedPrincipalByCreatedBy(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(l.outstandingPrincipal + l.outstandingInterest + l.outstandingPenalty), 0) FROM Loan l WHERE l.status = 'ACTIVE' AND l.createdBy.id = :userId")
    Long sumTotalOutstandingByCreatedBy(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(l.principalAmount), 0) FROM Loan l WHERE l.status IN ('ACTIVE', 'CLOSED') AND l.createdBy.id = :userId")
    Long sumTotalDisbursedByCreatedBy(@Param("userId") UUID userId);
}
