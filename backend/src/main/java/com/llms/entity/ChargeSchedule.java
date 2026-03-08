package com.llms.entity;

import com.llms.enums.ChargeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a scheduled charge payment, used when charges are spread across tenure.
 * Links to both the LoanCharge and optionally to a RepaymentSchedule entry.
 */
@Entity
@Table(name = "charge_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_charge_id", nullable = false)
    private LoanCharge loanCharge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private RepaymentSchedule repaymentSchedule;

    @Column(name = "amount_due", nullable = false)
    private Long amountDue;

    @Column(name = "amount_paid")
    private Long amountPaid;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ChargeStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

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
        return amountDue - amountPaid;
    }

    /**
     * Returns true if this scheduled charge is fully paid.
     */
    public boolean isFullyPaid() {
        return getOutstandingAmount() <= 0;
    }

    /**
     * Applies a payment to this scheduled charge.
     * @param paymentAmount Amount to apply in paise
     * @return Amount actually applied
     */
    public long applyPayment(long paymentAmount) {
        long outstanding = getOutstandingAmount();
        long applied = Math.min(paymentAmount, outstanding);
        amountPaid += applied;
        updateStatus();
        return applied;
    }

    private void updateStatus() {
        if (isFullyPaid()) {
            status = ChargeStatus.PAID;
            paidAt = LocalDateTime.now();
        } else if (amountPaid > 0) {
            status = ChargeStatus.PARTIAL;
        }
    }
}
