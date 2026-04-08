package com._team._team.attendance.dto.reqDto;

import com._team._team.attendance.domain.AttendanceLog;
import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.domain.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 출퇴근/휴게 이벤트 생성 요청 DTO
 * - CLOCK_IN, CLOCK_OUT, BREAK_START, BREAK_END 공통 사용
 * - EventType은 서비스 메서드에서 직접 지정
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class AttendanceLogCreateReqDto {

    /** 이벤트 발생 시각 (null이면 서버 현재 시간 사용) */
    private LocalDateTime eventTime;

    /** GPS 위도 (nullable — 모바일 출퇴근 시 전달) */
    private Double latitude;

    /** GPS 경도 (nullable — 모바일 출퇴근 시 전달) */
    private Double longitude;

    /** 기기 식별자 (nullable — 앱/기기별 구분용) */
    private String deviceId;

    public AttendanceLog toEntity(DailyAttendance daily, EventType eventType, LocalDateTime eventTime) {
        return AttendanceLog.builder()
                .dailyAttendance(daily)
                .memberId(daily.getMemberId())
                .eventType(eventType)
                .eventTime(eventTime)
                .latitude(this.latitude)
                .longitude(this.longitude)
                .deviceId(this.deviceId)
                .isCorrectedYn("N")
                .build();
    }
}
