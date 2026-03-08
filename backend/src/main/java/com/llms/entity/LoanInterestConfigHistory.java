package com.llms.entity;

import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable audit trail for interest configuration changes.
 * Records are append-only and never modified.
 * Exactly one record per loan has effective_to = NULL (the active configuration).
 */
@Entity
@Table(name = "loan_interest_config_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanInterestConfigHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_rate_mode", nullable = false, length = 30)
    private InterestRateMode interestRateMode;

    @Column(name = "interest_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", nullable = false, length = 20)
    private InterestType interestType;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Returns true if this configuration is currently active.
     */
    public boolean isActive() {
        return effectiveTo == null;
    }

    /**
     * Returns true if this configuration was active on the given date.
     */
    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(effectiveFrom)) {
            return false;
        }
        return effectiveTo == null || !date.isAfter(effectiveTo);
    }
}
