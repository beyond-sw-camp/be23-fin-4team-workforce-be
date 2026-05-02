package com._team._team.attendance.domain;

import com._team._team.attendance.domain.enums.PromotionLogStatus;
import com._team._team.attendance.domain.enums.PromotionStage;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    /** 직원이 사용계획 회신한 시각, null 이면 미응답 */
    @Column
    private LocalDateTime acknowledgedAt;

    /**
     * 직원이 알림을 처음 열람한 시각
     */
    @Column
    private LocalDateTime viewedAt;

    /** 직원이 입력한 사용 계획 날짜 — 참고용. JSON 배열 ["2027-05-12","2027-05-13"] */
    @Column(columnDefinition = "TEXT")
    private String plannedDates;

    /** 회사가 강제 지정한 연차일, JSON 배열 */
    @Column(columnDefinition = "TEXT")
    private String designatedDates;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PromotionLogStatus status = PromotionLogStatus.SENT;

    /** 강제 지정 시 사유 */
    @Column(length = 500)
    private String designationReason;

    /**
     * 알림 열람 처리, 1회 기록 후 변경하지 않음
     */
    public void markViewed() {
        if (this.viewedAt == null) {
            this.viewedAt = LocalDateTime.now();
        }
    }

    /**
     * 직원 회신 처리.
     * - acknowledgedAt 기록있ㄷ면 회사 촉진 의무 면책 완료
     * - plannedDates 는 "참고용 계획", 실제 그날 안써도 됨
     */
    public void acknowledge(String plannedDatesJson) {
        if (this.status != PromotionLogStatus.SENT) {
            throw new IllegalStateException("이미 응답되었거나 강제 지정된 통보입니다.");
        }
        this.status = PromotionLogStatus.ACKNOWLEDGED;
        this.acknowledgedAt = LocalDateTime.now();
        this.plannedDates = plannedDatesJson;
    }

    /**
     * 회사 강제 지정 (노무수령 거부)
     * - 무응답자 대상으로만 호출
     */
    public void designate(String datesJson, String reason) {
        if (this.status == PromotionLogStatus.DESIGNATED) {
            throw new IllegalStateException("이미 강제 지정된 통보입니다.");
        }
        this.status = PromotionLogStatus.DESIGNATED;
        this.designatedDates = datesJson;
        this.designationReason = reason;
    }
}