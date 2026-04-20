package com._team._team.attendance.dto.resDto;

import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.domain.enums.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;


/**
 * 일별 근태 요약 응답 DTO
 * - 월간 근태 조회 시 이 DTO 리스트로 응답 (attendance_log JOIN 없음)
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DailyAttendanceResDto {

    private UUID dailyAttendanceId;
    private UUID memberId;
    private LocalDate attendanceDate;
    private AttendanceStatus status;
    private UUID workScheduleId;
    private LocalDateTime firstClockIn;
    private LocalDateTime lastClockOut;
    private Integer workedMinutes;
    private Integer overtimeMinutes;


    public static DailyAttendanceResDto fromEntity(DailyAttendance daily) {
        return DailyAttendanceResDto.builder()
                .dailyAttendanceId(daily.getDailyAttendanceId())
                .memberId(daily.getMemberId())
                .attendanceDate(daily.getAttendanceDate())
                .status(daily.getStatus())
                .workScheduleId(daily.getWorkScheduleId())
                .firstClockIn(daily.getFirstClockIn())
                .lastClockOut(daily.getLastClockOut())
                .workedMinutes(daily.getWorkedMinutes())
                .overtimeMinutes(daily.getOvertimeMinutes())
                .build();
    }
}
