package com._team._team.goal.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "performance_record_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PerformanceRecordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "performance_record_history_id", columnDefinition = "BINARY(16)")
    private UUID performanceRecordHistoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_record_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private PerformanceRecord performanceRecord;

    @Column(name = "before_value", precision = 18, scale = 4)
    private BigDecimal beforeValue;

    @Column(name = "after_value", precision = 18, scale = 4)
    private BigDecimal afterValue;

    @Column(name = "before_desc", columnDefinition = "TEXT")
    private String beforeDesc;

    @Column(name = "after_desc", columnDefinition = "TEXT")
    private String afterDesc;

    @Column(name = "change_reason", columnDefinition = "TEXT", nullable = false)
    private String changeReason;

    @Column(name = "changed_by", columnDefinition = "BINARY(16)", nullable = false)
    private UUID changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
}
