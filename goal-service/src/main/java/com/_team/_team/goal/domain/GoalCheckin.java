package com._team._team.goal.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.goal.domain.enums.GoalProgressStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GoalCheckin extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID goalCheckinId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Goal goal;

    @Column(nullable = false)
    private UUID memberId;

    @Column(precision = 18, scale = 4)
    private BigDecimal actualValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalProgressStatus progressStatus;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
