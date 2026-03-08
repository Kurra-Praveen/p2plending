package com.llms.entity;

import com.llms.enums.ChargeApplicationTiming;
import com.llms.enums.ChargeCalculationType;
import com.llms.enums.ChargeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Template definition for charges that can be applied to loans.
 * Defines charge types, calculation methods, and application timing.
 */
@Entity
@Table(name = "charge_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 30)
    private ChargeType chargeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", nullable = false, length = 30)
    private ChargeCalculationType calculationType;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "percentage", precision = 5, scale = 4)
    private BigDecimal percentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_timing", nullable = false, length = 30)
    private ChargeApplicationTiming applicationTiming;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "is_mandatory")
    private Boolean isMandatory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (isMandatory == null) {
            isMandatory = false;
        }
        if (calculationType == null) {
            calculationType = ChargeCalculationType.FIXED;
        }
        if (applicationTiming == null) {
            applicationTiming = ChargeApplicationTiming.DISBURSEMENT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculates the charge amount based on the calculation type.
     * @param principalAmount The loan principal amount in paise
     * @return The calculated charge amount in paise
     */
    public long calculateAmount(long principalAmount) {
        return switch (calculationType) {
            case FIXED -> amount != null ? amount : 0L;
            case PERCENTAGE_OF_PRINCIPAL, PERCENTAGE_OF_DISBURSEMENT -> {
                if (percentage == null) {
                    yield 0L;
                }
                BigDecimal principal = BigDecimal.valueOf(principalAmount);
                BigDecimal chargeAmount = principal.multiply(percentage).divide(BigDecimal.valueOf(100));
                yield chargeAmount.longValue();
            }
        };
    }
}
