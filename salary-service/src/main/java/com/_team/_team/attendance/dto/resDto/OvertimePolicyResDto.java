package com._team._team.attendance.dto.resDto;

import com._team._team.attendance.domain.OvertimePolicy;
import com._team._team.attendance.domain.enums.ApprovalMode;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class OvertimePolicyResDto {

    private UUID overtimePolicyId;
    private Integer overtimeRoundingMinutes;
    private ApprovalMode approvalMode;
    private Integer postApprovalDeadlineHours;
    private Integer weeklyOvertimeLimitMinutes;
    private Integer weeklyTotalLimitMinutes;
    private Integer dailyOvertimeLimitMinutes;
    private Integer monthlyOvertimeLimitMinutes;
    private LocalTime nightStartTime;
    private LocalTime nightEndTime;
    private Boolean holidayWorkRequiresApproval;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    public static OvertimePolicyResDto fromEntity(OvertimePolicy p) {
        return OvertimePolicyResDto.builder()
                .overtimePolicyId(p.getOvertimePolicyId())
                .overtimeRoundingMinutes(p.getOvertimeRoundingMinutes())
                .approvalMode(p.getApprovalMode())
                .postApprovalDeadlineHours(p.getPostApprovalDeadlineHours())
                .weeklyOvertimeLimitMinutes(p.getWeeklyOvertimeLimitMinutes())
                .weeklyTotalLimitMinutes(p.getWeeklyTotalLimitMinutes())
                .dailyOvertimeLimitMinutes(p.getDailyOvertimeLimitMinutes())
                .monthlyOvertimeLimitMinutes(p.getMonthlyOvertimeLimitMinutes())
                .nightStartTime(p.getNightStartTime())
                .nightEndTime(p.getNightEndTime())
                .holidayWorkRequiresApproval(p.getHolidayWorkRequiresApproval())
                .effectiveFrom(p.getEffectiveFrom())
                .effectiveTo(p.getEffectiveTo())
                .build();
    }
}
