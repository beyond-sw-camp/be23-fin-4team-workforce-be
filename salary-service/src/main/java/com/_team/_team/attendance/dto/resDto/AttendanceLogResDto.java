package com._team._team.attendance.dto.resDto;

import com._team._team.attendance.domain.AttendanceLog;
import com._team._team.attendance.domain.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class AttendanceLogResDto {

    private UUID attendanceLogId;
    private EventType eventType;
    private LocalDateTime eventTime;
    private Double latitude;
    private Double longitude;
    private String deviceId;
    private String isCorrectedYn;

    public static AttendanceLogResDto fromEntity(AttendanceLog log) {
        return AttendanceLogResDto.builder()
                .attendanceLogId(log.getAttendanceLogId())
                .eventType(log.getEventType())
                .eventTime(log.getEventTime())
                .latitude(log.getLatitude())
                .longitude(log.getLongitude())
                .deviceId(log.getDeviceId())
                .isCorrectedYn(log.getIsCorrectedYn())
                .build();
    }
}
