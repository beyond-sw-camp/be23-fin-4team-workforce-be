package com._team._team.attendance.domain;

import com._team._team.attendance.domain.enums.PromotionStage;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 연차사용촉진제 알림 이력
 */
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Getter
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"memberBalanceId", "stage"})
})
public class LeavePromotionLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID promotionLogId;

    @Column(nullable = false)
    private UUID memberBalanceId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PromotionStage stage;

    @Column(nullable = false)
    private LocalDate sentOn;
}