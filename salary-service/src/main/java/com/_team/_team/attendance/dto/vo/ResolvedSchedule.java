package com._team._team.attendance.dto.vo;

import com._team._team.attendance.domain.enums.WorkType;

import java.time.LocalTime;
import java.util.UUID;

/**
 * 스케줄 해석 결과 (값 객체, DB 저장 x)
 * 한 직원의 특정 일자에 적용되는 최종 근무 시간 정보를 담음
 */
public record ResolvedSchedule(
        UUID workScheduleId,
        UUID flexibleSlotId,     // FIXED면 null
        WorkType workType,
        LocalTime startTime,
        LocalTime endTime,
        Integer workMinutes,
        Integer breakMinutes
) {

    // FIXED 스케줄에서 조립
    public static ResolvedSchedule fromFixed(
            UUID workScheduleId,
            LocalTime startTime,
            LocalTime endTime,
            Integer workMinutes,
            Integer breakMinutes) {
        return new ResolvedSchedule(
                workScheduleId,
                null,
                WorkType.FIXED,
                startTime,
                endTime,
                workMinutes,
                breakMinutes
        );
    }

    // FLEXIBLE 스케줄 + 선택된 슬롯으로 조립
    public static ResolvedSchedule fromFlexible(
            UUID workScheduleId,
            UUID flexibleSlotId,
            LocalTime startTime,
            LocalTime endTime,
            Integer workMinutes,
            Integer breakMinutes) {
        return new ResolvedSchedule(
                workScheduleId,
                flexibleSlotId,
                WorkType.FLEXIBLE,
                startTime,
                endTime,
                workMinutes,
                breakMinutes
        );
    }
}