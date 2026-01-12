package com.llms.repository;

import com.llms.entity.LoanStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanStatusHistoryRepository extends JpaRepository<LoanStatusHistory, UUID> {

    List<LoanStatusHistory> findByLoanIdOrderByChangedAtDesc(UUID loanId);
}
