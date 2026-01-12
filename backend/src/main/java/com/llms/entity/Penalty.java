package com.llms.entity;

import com.llms.enums.PenaltyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "penalties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private RepaymentSchedule schedule;

    @Column(name = "emi_no", nullable = false)
    private Integer emiNo;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "days_overdue", nullable = false)
    private Integer daysOverdue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PenaltyStatus status = PenaltyStatus.UNPAID;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getUnpaidAmount() {
        if (status == PenaltyStatus.PAID || status == PenaltyStatus.WAIVED) {
            return 0L;
        }
        return amount;
    }
}
