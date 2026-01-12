package com.llms.repository;

import com.llms.entity.Penalty;
import com.llms.enums.PenaltyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, UUID> {

    List<Penalty> findByLoanIdOrderByEmiNoAsc(UUID loanId);

    List<Penalty> findByLoanIdAndStatus(UUID loanId, PenaltyStatus status);

    @Query("SELECT p FROM Penalty p JOIN FETCH p.schedule WHERE p.loan.id = :loanId AND p.status IN ('UNPAID', 'PARTIAL') ORDER BY p.emiNo ASC")
    List<Penalty> findUnpaidByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Penalty p WHERE p.loan.id = :loanId AND p.status IN ('UNPAID', 'PARTIAL')")
    Long sumUnpaidPenaltiesByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT p FROM Penalty p WHERE p.schedule.id = :scheduleId AND p.status IN ('UNPAID', 'PARTIAL')")
    List<Penalty> findUnpaidByScheduleId(@Param("scheduleId") UUID scheduleId);
}
