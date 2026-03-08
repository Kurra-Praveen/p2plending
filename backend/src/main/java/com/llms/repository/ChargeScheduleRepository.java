package com.llms.repository;

import com.llms.entity.ChargeSchedule;
import com.llms.enums.ChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChargeScheduleRepository extends JpaRepository<ChargeSchedule, UUID> {

    List<ChargeSchedule> findByLoanChargeIdOrderByDueDateAsc(UUID loanChargeId);

    List<ChargeSchedule> findByRepaymentScheduleId(UUID scheduleId);

    @Query("SELECT cs FROM ChargeSchedule cs WHERE cs.loanCharge.loan.id = :loanId " +
           "AND cs.status IN ('PENDING', 'PARTIAL') ORDER BY cs.dueDate ASC")
    List<ChargeSchedule> findOutstandingByLoanId(@Param("loanId") UUID loanId);

    @Query("SELECT cs FROM ChargeSchedule cs WHERE cs.dueDate <= :date " +
           "AND cs.status IN ('PENDING', 'PARTIAL')")
    List<ChargeSchedule> findOverdueChargeSchedules(@Param("date") LocalDate date);
}
