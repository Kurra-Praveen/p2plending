package com.llms.service;

import com.llms.entity.*;
import com.llms.enums.ChargeApplicationTiming;
import com.llms.enums.ChargeStatus;
import com.llms.repository.ChargeDefinitionRepository;
import com.llms.repository.ChargeScheduleRepository;
import com.llms.repository.LoanChargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for calculating and managing loan charges.
 * Charges are separate from principal and interest in payment allocation.
 */
@Service
@RequiredArgsConstructor
public class ChargeService {

    private final ChargeDefinitionRepository chargeDefinitionRepository;
    private final LoanChargeRepository loanChargeRepository;
    private final ChargeScheduleRepository chargeScheduleRepository;

    /**
     * Gets all active charge definitions.
     */
    public List<ChargeDefinition> getActiveChargeDefinitions() {
        return chargeDefinitionRepository.findByIsActiveTrue();
    }

    /**
     * Gets mandatory charge definitions.
     */
    public List<ChargeDefinition> getMandatoryCharges() {
        return chargeDefinitionRepository.findByIsMandatoryTrueAndIsActiveTrue();
    }

    /**
     * Applies charges to a loan based on charge definitions.
     *
     * @param loan               The loan to apply charges to
     * @param chargeDefinitionIds List of charge definition IDs to apply
     * @param disbursementDate   The disbursement date
     * @param schedules          The repayment schedule (for spreading charges)
     * @return List of applied charges
     */
    @Transactional
    public List<LoanCharge> applyCharges(
            Loan loan,
            List<UUID> chargeDefinitionIds,
            LocalDate disbursementDate,
            List<RepaymentSchedule> schedules
    ) {
        List<LoanCharge> appliedCharges = new ArrayList<>();

        // Apply mandatory charges first
        List<ChargeDefinition> mandatoryCharges = getMandatoryCharges();
        for (ChargeDefinition definition : mandatoryCharges) {
            if (!chargeDefinitionIds.contains(definition.getId())) {
                LoanCharge charge = applyCharge(loan, definition, disbursementDate, schedules);
                appliedCharges.add(charge);
            }
        }

        // Apply selected charges
        for (UUID definitionId : chargeDefinitionIds) {
            ChargeDefinition definition = chargeDefinitionRepository.findById(definitionId)
                    .orElse(null);
            if (definition != null && definition.getIsActive()) {
                LoanCharge charge = applyCharge(loan, definition, disbursementDate, schedules);
                appliedCharges.add(charge);
            }
        }

        // Update loan outstanding charges
        updateLoanOutstandingCharges(loan);

        return appliedCharges;
    }

    /**
     * Applies a single charge to a loan.
     */
    @Transactional
    public LoanCharge applyCharge(
            Loan loan,
            ChargeDefinition definition,
            LocalDate disbursementDate,
            List<RepaymentSchedule> schedules
    ) {
        long chargeAmount = definition.calculateAmount(loan.getPrincipalAmount());

        LoanCharge loanCharge = LoanCharge.builder()
                .loan(loan)
                .chargeDefinition(definition)
                .name(definition.getName())
                .chargeType(definition.getChargeType())
                .amount(chargeAmount)
                .amountPaid(0L)
                .amountWaived(0L)
                .status(ChargeStatus.PENDING)
                .build();

        // Set due date based on application timing
        switch (definition.getApplicationTiming()) {
            case DISBURSEMENT:
                loanCharge.setDueDate(disbursementDate);
                break;
            case FIRST_EMI:
                if (!schedules.isEmpty()) {
                    loanCharge.setDueDate(schedules.get(0).getDueDate());
                } else {
                    loanCharge.setDueDate(disbursementDate.plusMonths(1));
                }
                break;
            case SPREAD_ACROSS_TENURE:
                loanCharge.setDueDate(null); // No single due date
                createChargeSchedule(loanCharge, chargeAmount, schedules);
                break;
            case CUSTOM_DATE:
                // For custom date, caller should set it manually
                break;
        }

        loanCharge = loanChargeRepository.save(loanCharge);
        return loanCharge;
    }

    /**
     * Creates a schedule for spreading charge across EMIs.
     */
    private void createChargeSchedule(
            LoanCharge loanCharge,
            long totalAmount,
            List<RepaymentSchedule> schedules
    ) {
        if (schedules.isEmpty()) {
            return;
        }

        int numInstallments = schedules.size();
        long amountPerInstallment = totalAmount / numInstallments;
        long remainder = totalAmount - (amountPerInstallment * numInstallments);

        List<ChargeSchedule> chargeSchedules = new ArrayList<>();

        for (int i = 0; i < numInstallments; i++) {
            RepaymentSchedule schedule = schedules.get(i);
            long installmentAmount = amountPerInstallment;

            // Add remainder to last installment
            if (i == numInstallments - 1) {
                installmentAmount += remainder;
            }

            ChargeSchedule chargeSchedule = ChargeSchedule.builder()
                    .loanCharge(loanCharge)
                    .repaymentSchedule(schedule)
                    .amountDue(installmentAmount)
                    .amountPaid(0L)
                    .dueDate(schedule.getDueDate())
                    .status(ChargeStatus.PENDING)
                    .build();

            chargeSchedules.add(chargeSchedule);
        }

        loanCharge.setChargeSchedules(chargeSchedules);
    }

    /**
     * Gets outstanding charges for a loan.
     */
    public List<LoanCharge> getOutstandingCharges(UUID loanId) {
        return loanChargeRepository.findOutstandingChargesByLoanId(loanId);
    }

    /**
     * Gets total outstanding charges for a loan.
     */
    public long getTotalOutstandingCharges(UUID loanId) {
        Long total = loanChargeRepository.sumOutstandingChargesByLoanId(loanId);
        return total != null ? total : 0L;
    }

    /**
     * Updates the loan's outstanding charges field.
     */
    @Transactional
    public void updateLoanOutstandingCharges(Loan loan) {
        long totalOutstanding = getTotalOutstandingCharges(loan.getId());
        loan.setOutstandingCharges(totalOutstanding);
    }

    /**
     * Waives a charge.
     */
    @Transactional
    public LoanCharge waiveCharge(UUID chargeId, User user, String reason) {
        LoanCharge charge = loanChargeRepository.findById(chargeId)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found: " + chargeId));

        long outstanding = charge.getOutstandingAmount();
        charge.waive(outstanding, user, reason);

        charge = loanChargeRepository.save(charge);

        // Update loan outstanding
        updateLoanOutstandingCharges(charge.getLoan());

        return charge;
    }

    /**
     * Gets all charges for a loan.
     */
    public List<LoanCharge> getLoanCharges(UUID loanId) {
        return loanChargeRepository.findByLoanIdOrderByCreatedAtAsc(loanId);
    }
}
