package com.llms.repository;

import com.llms.entity.Borrower;
import com.llms.enums.BorrowerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BorrowerRepository extends JpaRepository<Borrower, UUID> {

    @Query("SELECT b FROM Borrower b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Borrower> findByIdAndNotDeleted(@Param("id") UUID id);

    @Query("SELECT b FROM Borrower b WHERE b.deletedAt IS NULL")
    Page<Borrower> findAllNotDeleted(Pageable pageable);

    @Query("SELECT b FROM Borrower b WHERE b.status = :status AND b.deletedAt IS NULL")
    Page<Borrower> findByStatusAndNotDeleted(@Param("status") BorrowerStatus status, Pageable pageable);

    @Query("SELECT b FROM Borrower b WHERE b.phone = :phone AND b.deletedAt IS NULL")
    Optional<Borrower> findByPhoneAndNotDeleted(@Param("phone") String phone);

    boolean existsByPhone(String phone);

    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.deletedAt IS NULL")
    long countActive();

    // ========== LENDER-FILTERED QUERIES (Data Isolation Fix) ==========

    @Query("SELECT b FROM Borrower b WHERE b.id = :id AND b.createdBy.id = :userId AND b.deletedAt IS NULL")
    Optional<Borrower> findByIdAndCreatedByAndNotDeleted(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT b FROM Borrower b WHERE b.createdBy.id = :userId AND b.deletedAt IS NULL")
    Page<Borrower> findAllByCreatedByAndNotDeleted(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT b FROM Borrower b WHERE b.status = :status AND b.createdBy.id = :userId AND b.deletedAt IS NULL")
    Page<Borrower> findByStatusAndCreatedByAndNotDeleted(@Param("status") BorrowerStatus status, @Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(b) FROM Borrower b WHERE b.createdBy.id = :userId AND b.deletedAt IS NULL")
    long countActiveByCreatedBy(@Param("userId") UUID userId);
}
