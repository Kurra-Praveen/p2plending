package com.llms.service;

import com.llms.entity.*;
import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;
import com.llms.enums.LoanFrequency;
import com.llms.enums.LoanStatus;
import com.llms.exception.InvalidStateException;
import com.llms.exception.ResourceNotFoundException;
import com.llms.repository.LoanConfigurationRepository;
import com.llms.repository.LoanInterestConfigHistoryRepository;
import com.llms.repository.LoanRepository;
import com.llms.security.SecurityUtils;
import com.llms.service.schedule.ScheduleGenerator;
import com.llms.service.schedule.ScheduleGeneratorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing loan configurations and interest mode switching.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanConfigurationService {

    private final LoanRepository loanRepository;
    private final LoanConfigurationRepository configurationRepository;
    private final LoanInterestConfigHistoryRepository historyRepository;
    private final ScheduleGeneratorFactory scheduleGeneratorFactory;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    /**
     * Creates initial configuration for a new loan.
     */
    @Transactional
    public LoanConfiguration createInitialConfiguration(
            Loan loan,
            InterestRateMode interestRateMode,
            BigDecimal interestRate,
            InterestType interestType,
            LoanFrequency frequency,
            Integer tenureMonths,
            Integer tenureUnits,
            String timezone
    ) {
        User currentUser = securityUtils.getCurrentUser();
        LocalDate effectiveFrom = LocalDate.now();

        // Create history record first
        LoanInterestConfigHistory history = LoanInterestConfigHistory.builder()
                .loan(loan)
                .interestRateMode(interestRateMode)
                .interestRate(interestRate)
                .interestType(interestType)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(null)
                .changedBy(currentUser)
                .changeReason("Initial configuration")
                .build();

        history = historyRepository.save(history);

        // Create configuration
        LoanConfiguration configuration = LoanConfiguration.builder()
                .loan(loan)
                .interestRateMode(interestRateMode)
                .interestRate(interestRate)
                .interestType(interestType)
                .frequency(frequency)
                .tenureMonths(tenureMonths)
                .tenureUnits(tenureUnits)
                .timezone(timezone != null ? timezone : "Asia/Kolkata")
                .activeHistory(history)
                .build();

        configuration = configurationRepository.save(configuration);

        log.info("Created initial configuration for loan: {}", loan.getId());

        return configuration;
    }

    /**
     * Gets the configuration for a loan.
     */
    @Transactional(readOnly = true)
    public LoanConfiguration getConfiguration(UUID loanId) {
        return configurationRepository.findByLoanId(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("LoanConfiguration", "loanId", loanId));
    }

    /**
     * Gets the interest configuration history for a loan.
     */
    @Transactional(readOnly = true)
    public List<LoanInterestConfigHistory> getInterestConfigHistory(UUID loanId) {
        return historyRepository.findByLoanIdOrderByEffectiveFromDesc(loanId);
    }

    /**
     * Gets the active interest configuration for a loan.
     */
    @Transactional(readOnly = true)
    public LoanInterestConfigHistory getActiveInterestConfig(UUID loanId) {
        return historyRepository.findActiveByLoanId(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("LoanInterestConfigHistory", "loanId", loanId));
    }

    /**
     * Gets the interest configuration that was active on a specific date.
     */
    @Transactional(readOnly = true)
    public LoanInterestConfigHistory getInterestConfigOnDate(UUID loanId, LocalDate date) {
        return historyRepository.findActiveOnDate(loanId, date)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "LoanInterestConfigHistory", "loanId and date", loanId + " / " + date));
    }

    /**
     * Switches the interest rate mode for a loan.
     * Only allowed for ACTIVE loans with a future effective date.
     *
     * @param loanId        The loan ID
     * @param newMode       The new interest rate mode
     * @param newRate       The new interest rate
     * @param effectiveDate The date from which the new mode takes effect (must be in the future)
     * @param reason        Reason for the change
     * @return The updated configuration
     */
    @Transactional
    public LoanConfiguration switchInterestMode(
            UUID loanId,
            InterestRateMode newMode,
            BigDecimal newRate,
            LocalDate effectiveDate,
            String reason
    ) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", "id", loanId));

        User currentUser = securityUtils.getCurrentUser();

        // Validate loan status - only ACTIVE loans can have interest mode switched
        if (loan.getStatus() != LoanStatus.ACTIVE) {
            throw new InvalidStateException("INVALID_STATE",
                    "Interest mode can only be switched for ACTIVE loans. Current status: " + loan.getStatus());
        }

        // Validate effective date is strictly in the future
        LocalDate today = LocalDate.now();
        if (!effectiveDate.isAfter(today)) {
            throw new InvalidStateException("INVALID_EFFECTIVE_DATE",
                    "Effective date must be strictly in the future. Provided: " + effectiveDate);
        }

        // Validate: WEEKLY + DAILY_PERCENTAGE + REDUCING is restricted
        LoanConfiguration config = getConfiguration(loanId);
        if (config.getFrequency() == LoanFrequency.WEEKLY &&
            newMode == InterestRateMode.DAILY_PERCENTAGE &&
            config.getInterestType() == InterestType.REDUCING) {
            throw new InvalidStateException("RESTRICTED_COMBINATION",
                    "WEEKLY + DAILY_PERCENTAGE + REDUCING combination is restricted");
        }

        // Close current active history record
        LoanInterestConfigHistory currentHistory = historyRepository.findActiveByLoanId(loanId)
                .orElse(null);

        if (currentHistory != null) {
            currentHistory.setEffectiveTo(effectiveDate.minusDays(1));
            historyRepository.save(currentHistory);
        }

        // Create new history record
        LoanInterestConfigHistory newHistory = LoanInterestConfigHistory.builder()
                .loan(loan)
                .interestRateMode(newMode)
                .interestRate(newRate)
                .interestType(config.getInterestType()) // Interest type remains the same
                .effectiveFrom(effectiveDate)
                .effectiveTo(null)
                .changedBy(currentUser)
                .changeReason(reason)
                .build();

        newHistory = historyRepository.save(newHistory);

        // Store old config for audit
        LoanConfiguration oldConfig = LoanConfiguration.builder()
                .interestRateMode(config.getInterestRateMode())
                .interestRate(config.getInterestRate())
                .build();

        // Update configuration
        config.setInterestRateMode(newMode);
        config.setInterestRate(newRate);
        config.setActiveHistory(newHistory);

        config = configurationRepository.save(config);

        log.info("Interest mode switched for loan: {} to {} effective from {}",
                loanId, newMode, effectiveDate);

        auditService.logUpdate("LoanConfiguration", config.getId(), oldConfig, config);

        return config;
    }

    /**
     * Generates a loan preview with the given configuration.
     * This is used for showing potential EMI and schedule before loan creation.
     */
    public ScheduleGenerator.ScheduleResult generateLoanPreview(
            long principal,
            BigDecimal interestRate,
            InterestRateMode interestRateMode,
            InterestType interestType,
            LoanFrequency frequency,
            int tenure,
            LocalDate startDate,
            String timezone
    ) {
        ScheduleGenerator generator = scheduleGeneratorFactory.getGenerator(frequency);

        ScheduleGenerator.ScheduleRequest request = ScheduleGenerator.ScheduleRequest.builder()
                .principal(principal)
                .interestRate(interestRate)
                .interestRateMode(interestRateMode)
                .interestType(interestType)
                .tenure(tenure)
                .startDate(startDate)
                .timezone(timezone != null ? timezone : "Asia/Kolkata")
                .build();

        return generator.generateSchedule(request);
    }
}
