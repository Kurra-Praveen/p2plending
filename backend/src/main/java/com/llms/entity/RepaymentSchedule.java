package com.llms.entity;

import com.llms.enums.EmiStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "repayment_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "emi_no", nullable = false)
    private Integer emiNo;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_due", nullable = false)
    private Long principalDue;

    @Column(name = "interest_due", nullable = false)
    private Long interestDue;

    @Column(name = "total_due", nullable = false)
    private Long totalDue;

    @Column(name = "principal_paid", nullable = false)
    @Builder.Default
    private Long principalPaid = 0L;

    @Column(name = "interest_paid", nullable = false)
    @Builder.Default
    private Long interestPaid = 0L;

    @Column(name = "penalty_paid", nullable = false)
    @Builder.Default
    private Long penaltyPaid = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EmiStatus status = EmiStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getPrincipalRemaining() {
        return principalDue - principalPaid;
    }

    public Long getInterestRemaining() {
        return interestDue - interestPaid;
    }

    public Long getTotalRemaining() {
        return getPrincipalRemaining() + getInterestRemaining();
    }

    public boolean isFullyPaid() {
        return getPrincipalRemaining() == 0 && getInterestRemaining() == 0;
    }
}
