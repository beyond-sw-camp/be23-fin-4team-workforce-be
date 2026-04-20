package com._team._team.attendance.dto.reqDto;

import com._team._team.attendance.domain.enums.WorkType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class WorkScheduleUpdateReqDto {

    private String scheduleName;
    private WorkType workType;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer workMinutes;
    private Integer breakMinutes;
    private Integer selectionDeadlineDay;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
}