package com._team._team.goal.domain;

import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GoalCycle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID cycleId;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    public void validateDateRange() {
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("사이클 종료일은 시작일보다 이후여야 합니다.");
        }
    }
}
