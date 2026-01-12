package com.llms.entity;

import com.llms.enums.ChargeStatus;
import com.llms.enums.ChargeType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a charge applied to a specific loan.
 * Charges are separate from principal and interest in payment allocation.
 */
@Entity
@Table(name = "loan_charges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_definition_id")
    private ChargeDefinition chargeDefinition;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 30)
    private ChargeType chargeType;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "amount_paid")
    private Long amountPaid;

    @Column(name = "amount_waived")
    private Long amountWaived;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ChargeStatus status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "waived_at")
    private LocalDateTime waivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waived_by")
    private User waivedBy;

    @Column(name = "waiver_reason", length = 500)
    private String waiverReason;

    @OneToMany(mappedBy = "loanCharge", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChargeSchedule> chargeSchedules = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (amountPaid == null) {
            amountPaid = 0L;
        }
        if (amountWaived == null) {
            amountWaived = 0L;
        }
        if (status == null) {
            status = ChargeStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the remaining unpaid amount.
     */
    public long getOutstandingAmount() {
        return amount - amountPaid - amountWaived;
    }

    /**
     * Returns true if the charge is fully paid or waived.
     */
    public boolean isFullySettled() {
        return getOutstandingAmount() <= 0;
    }

    /**
     * Applies a payment to this charge.
     * @param paymentAmount Amount to apply in paise
     * @return Amount actually applied (may be less if charge is smaller)
     */
    public long applyPayment(long paymentAmount) {
        long outstanding = getOutstandingAmount();
        long applied = Math.min(paymentAmount, outstanding);
        amountPaid += applied;
        updateStatus();
        return applied;
    }

    /**
     * Waives a portion or all of the remaining charge.
     * @param waiverAmount Amount to waive in paise
     * @param user User performing the waiver
     * @param reason Reason for the waiver
     */
    public void waive(long waiverAmount, User user, String reason) {
        long outstanding = getOutstandingAmount();
        long waived = Math.min(waiverAmount, outstanding);
        amountWaived += waived;
        waivedBy = user;
        waiverReason = reason;
        waivedAt = LocalDateTime.now();
        updateStatus();
    }

    private void updateStatus() {
        long outstanding = getOutstandingAmount();
        if (outstanding <= 0) {
            if (amountWaived > 0 && amountPaid == 0) {
                status = ChargeStatus.WAIVED;
            } else {
                status = ChargeStatus.PAID;
                paidAt = LocalDateTime.now();
            }
        } else if (amountPaid > 0) {
            status = ChargeStatus.PARTIAL;
        }
    }
}
