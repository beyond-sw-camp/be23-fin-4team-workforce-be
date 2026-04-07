package com._team._team.evaluation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "calibration_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CalibrationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "calibration_id")
    private UUID calibrationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Evaluation evaluation;

    @Column(name = "adjusted_by", nullable = false)
    private UUID adjustedBy;

    @Column(name = "before_grade", length = 5)
    private String beforeGrade;

    @Column(name = "after_grade", length = 5)
    private String afterGrade;

    @Column(name = "before_score", precision = 7, scale = 2)
    private BigDecimal beforeScore;

    @Column(name = "after_score", precision = 7, scale = 2)
    private BigDecimal afterScore;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @CreationTimestamp
    @Column(name = "adjusted_at", nullable = false)
    private LocalDateTime adjustedAt;
}
