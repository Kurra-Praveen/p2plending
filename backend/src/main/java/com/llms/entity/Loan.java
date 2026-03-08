package com.llms.entity;

import com.llms.enums.InterestType;
import com.llms.enums.LoanFrequency;
import com.llms.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @Column(name = "principal_amount", nullable = false)
    private Long principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", nullable = false)
    private InterestType interestType;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "tenure_units")
    private Integer tenureUnits;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", length = 20)
    @Builder.Default
    private LoanFrequency frequency = LoanFrequency.MONTHLY;

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Asia/Kolkata";

    @Column(name = "emi_amount", nullable = false)
    private Long emiAmount;

    @Column(name = "total_interest", nullable = false)
    private Long totalInterest;

    @Column(name = "total_payable", nullable = false)
    private Long totalPayable;

    @Column(name = "outstanding_principal", nullable = false)
    private Long outstandingPrincipal;

    @Column(name = "outstanding_interest", nullable = false)
    private Long outstandingInterest;

    @Column(name = "outstanding_penalty", nullable = false)
    @Builder.Default
    private Long outstandingPenalty = 0L;

    @Column(name = "outstanding_charges")
    @Builder.Default
    private Long outstandingCharges = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LoanStatus status = LoanStatus.CREATED;

    @Column(name = "disbursed_at")
    private LocalDateTime disbursedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("emiNo ASC")
    @Builder.Default
    private List<RepaymentSchedule> repaymentSchedules = new ArrayList<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Penalty> penalties = new ArrayList<>();

    @OneToOne(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private LoanConfiguration configuration;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL)
    @Builder.Default
    private List<LoanCharge> charges = new ArrayList<>();

    public Long getTotalOutstanding() {
        long charges = outstandingCharges != null ? outstandingCharges : 0L;
        return outstandingPrincipal + outstandingInterest + outstandingPenalty + charges;
    }

    /**
     * Returns the effective tenure based on frequency.
     * For WEEKLY loans, returns tenureUnits.
     * For MONTHLY loans, returns tenureMonths.
     */
    public Integer getEffectiveTenure() {
        return frequency == LoanFrequency.WEEKLY ? tenureUnits : tenureMonths;
    }
}
