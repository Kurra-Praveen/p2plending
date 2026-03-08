package com.llms.repository;

import com.llms.entity.LoanConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanConfigurationRepository extends JpaRepository<LoanConfiguration, UUID> {

    Optional<LoanConfiguration> findByLoanId(UUID loanId);

    boolean existsByLoanId(UUID loanId);
}
