package com.llms.repository;

import com.llms.entity.RepaymentSchedule;
import com.llms.enums.EmiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, UUID> {

    List<RepaymentSchedule> findByLoanIdOrderByEmiNoAsc(UUID loanId);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.loan.id = :loanId AND rs.status IN :statuses ORDER BY rs.emiNo ASC")
    List<RepaymentSchedule> findByLoanIdAndStatusIn(@Param("loanId") UUID loanId, @Param("statuses") List<EmiStatus> statuses);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.dueDate < :date AND rs.status IN ('PENDING', 'PARTIAL')")
    List<RepaymentSchedule> findOverdueSchedules(@Param("date") LocalDate date);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.loan.id = :loanId AND rs.status IN ('PENDING', 'PARTIAL') ORDER BY rs.emiNo ASC")
    List<RepaymentSchedule> findPendingByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT rs FROM RepaymentSchedule rs WHERE rs.loan.id = :loanId AND rs.emiNo = :emiNo")
    RepaymentSchedule findByLoanIdAndEmiNo(@Param("loanId") UUID loanId, @Param("emiNo") Integer emiNo);
}
