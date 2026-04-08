package com._team._team.attendance.dto.resDto;

import com._team._team.attendance.domain.WorkSchedule;
import com._team._team.attendance.domain.enums.WorkType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class WorkScheduleResDto {

    private UUID workScheduleId;
    private UUID companyId;
    private UUID memberId;
    private String scheduleName;
    private WorkType workType;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer workMinutes;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    public static WorkScheduleResDto fromEntity(WorkSchedule workSchedule) {
        return WorkScheduleResDto.builder()
                .workScheduleId(workSchedule.getWorkScheduleId())
                .companyId(workSchedule.getCompanyId())
                .memberId(workSchedule.getMemberId())
                .scheduleName(workSchedule.getScheduleName())
                .workType(workSchedule.getWorkType())
                .startTime(workSchedule.getStartTime())
                .endTime(workSchedule.getEndTime())
                .workMinutes(workSchedule.getWorkMinutes())
                .effectiveFrom(workSchedule.getEffectiveFrom())
                .effectiveTo(workSchedule.getEffectiveTo())
                .build();
    }
}
