package com.llms.entity;

import com.llms.enums.InterestRateMode;
import com.llms.enums.InterestType;
import com.llms.enums.LoanFrequency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents the current active configuration for a loan.
 * This table maintains the current snapshot of loan configuration
 * and links to the immutable history for audit purposes.
 */
@Entity
@Table(name = "loan_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false, unique = true)
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_rate_mode", nullable = false, length = 30)
    private InterestRateMode interestRateMode;

    @Column(name = "interest_rate", nullable = false, precision = 10, scale = 6)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", nullable = false, length = 20)
    private InterestType interestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private LoanFrequency frequency;

    @Column(name = "tenure_months")
    private Integer tenureMonths;

    @Column(name = "tenure_units")
    private Integer tenureUnits;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_history_id")
    private LoanInterestConfigHistory activeHistory;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (timezone == null) {
            timezone = "Asia/Kolkata";
        }
        if (interestRateMode == null) {
            interestRateMode = InterestRateMode.ANNUAL_PERCENTAGE;
        }
        if (frequency == null) {
            frequency = LoanFrequency.MONTHLY;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the effective tenure based on frequency.
     * For MONTHLY loans, returns tenureMonths.
     * For WEEKLY loans, returns tenureUnits.
     */
    public Integer getEffectiveTenure() {
        return frequency == LoanFrequency.WEEKLY ? tenureUnits : tenureMonths;
    }
}
