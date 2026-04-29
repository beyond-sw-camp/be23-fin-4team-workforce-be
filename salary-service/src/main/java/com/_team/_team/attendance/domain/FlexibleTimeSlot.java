package com._team._team.attendance.domain;

import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * 시차출퇴근제 선택 스케줄
 *
 *  회사가 제공하는 여러 출퇴근 시간대 중
 *  직원은 월별로 이 스케줄 중 하나를 선택해서 근무
 *
 * WorkSchedule.workType = FLEXIBLE 인 스케줄에만 해당
 * 개인 선택 결과는 MemberScheduleSelection 테이블에 기록
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class FlexibleTimeSlot extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID slotId;

    /**
     * workType=FLEXIBLE인 스케줄에만 해당
     */
    @Column(nullable = false)
    private UUID workScheduleId;

    @Column(nullable = false)
    private UUID companyId;

    /**
     * 시스템 식별용 코드
     * 예: "EARLY", "STANDARD", "LATE"
     */
    @Column(nullable = false, length = 30)
    private String slotCode;

    /**
     * UI 노출용 라벨
     * 예: "조기 출근 (7시-4시)"
     */
    @Column(nullable = false, length = 50)
    private String slotLabel;

    /** 이 스케줄 선택 시 정시 출근 시각 */
    @Column(nullable = false)
    private LocalTime startTime;

    /** 이 스케줄 선택 시 정시 퇴근 시각 */
    @Column(nullable = false)
    private LocalTime endTime;

    /**
     * 이 스케줄 순 근무시간(분)
     * overtime 계산 기준
     */
    @Column(nullable = false)
    private Integer workMinutes;

    /**
     * 이 스케줄 휴게시간(분)
     * 스케줄 다르게 설정 (조기 출근은 30분, 표준은 60분 등)
     */
    @Column(nullable = false)
    private Integer breakMinutes;

    /**
     * 기본 스케줄 여부
     * true면 신규 입사자에게 자동 할당되고, 월별 미선택자 자동 설정
     * 한 스케줄 내에서 오직 하나만 true 여야 한다 (애플리케이션 레벨 검증)
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(nullable = false, length = 1)
    @Builder.Default
    private String activeYn = "Y";

    // 스케줄 정보 수정
    public void update(String slotLabel, LocalTime startTime, LocalTime endTime,
                       Integer workMinutes, Integer breakMinutes) {
        if (slotLabel != null) this.slotLabel = slotLabel;
        if (startTime != null) this.startTime = startTime;
        if (endTime != null) this.endTime = endTime;
        if (workMinutes != null) this.workMinutes = workMinutes;
        if (breakMinutes != null) this.breakMinutes = breakMinutes;
    }

    // 기본 스케줄 지정
    public void setAsDefault() {
        this.isDefault = true;
    }

    // 기본 스케줄 지정 해제
    public void unsetDefault() {
        this.isDefault = false;
    }

    // 폐지 (소프트 딜리트)
    public void deactivate() {
        this.activeYn = "N";
    }
}