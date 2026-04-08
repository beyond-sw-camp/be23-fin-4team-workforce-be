package com._team._team.attendance.domain;

import com._team._team.attendance.domain.enums.WorkType;
import com._team._team.attendance.dto.reqDto.WorkScheduleUpdateReqDto;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 근무 스케줄
 * - memberId가 null이면 회사 전체 기본 스케줄
 * - memberId가 있으면 개인별 스케줄 (우선 적용)
 * - effectiveFrom/To로 기간별 스케줄 이력 관리
 * - workMinutes: overtime 계산의 기준값
 */

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class WorkSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID workScheduleId;

    @Column(nullable = false)
    private UUID companyId;

    private UUID memberId;

    @Column(nullable = false, length = 100)
    private String scheduleName;

    /** 근무 유형 (FIXED: 고정 / FLEXIBLE: 유연 / SHIFT: 교대) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkType workType;

    /** 규정 출근 시각 */
    @Column(nullable = false)
    private LocalTime startTime;

    /** 규정 퇴근 시각 */
    @Column(nullable = false)
    private LocalTime endTime;

    /** 하루 규정 근무시간(분) - overtime 계산 기준 */
    @Column(nullable = false)
    private Integer workMinutes;

    /** 스케줄 시작일 */
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    /** 스케줄 종료일 (null이면 현재 적용중) */
    private LocalDate effectiveTo;

    @Column(length = 1, nullable = false)
    @Builder.Default
    private String delYn = "N";

    public void update(WorkScheduleUpdateReqDto dto){
        if (dto.getScheduleName() != null) this.scheduleName = dto.getScheduleName();
        if (dto.getWorkType() != null) this.workType = dto.getWorkType();
        if (dto.getStartTime() != null) this.startTime = dto.getStartTime();
        if (dto.getEndTime() != null) this.endTime = dto.getEndTime();
        if (dto.getWorkMinutes() != null) this.workMinutes = dto.getWorkMinutes();
        if (dto.getEffectiveFrom() != null) this.effectiveFrom = dto.getEffectiveFrom();
        if (dto.getEffectiveTo() != null) this.effectiveTo = dto.getEffectiveTo();
    }

    public void delete(){
        this.delYn = "Y";
    }
}
