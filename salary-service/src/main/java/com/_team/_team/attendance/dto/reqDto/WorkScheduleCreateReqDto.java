package com._team._team.attendance.dto.reqDto;

import com._team._team.attendance.domain.WorkSchedule;
import com._team._team.attendance.domain.enums.WorkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class WorkScheduleCreateReqDto {

    /** 개인 스케줄이면 memberId, 회사 기본이면 null */
    private UUID memberId;

    @NotBlank(message = "스케줄 이름은 필수입니다.")
    private String scheduleName;

    @NotNull(message = "근무 유형은 필수입니다.")
    private WorkType workType;

    @NotNull(message = "출근 시각은 필수입니다.")
    private LocalTime startTime;

    @NotNull(message = "퇴근 시각은 필수입니다.")
    private LocalTime endTime;

    @NotNull(message = "근무시간(분)은 필수입니다.")
    private Integer workMinutes;

    private Integer breakMinutes;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate effectiveFrom;

    /** null이면 종료일 없음 (현재 적용중) */
    private LocalDate effectiveTo;

    public WorkSchedule toEntity(UUID companyId){
        return WorkSchedule.builder()
                .companyId(companyId)
                .memberId(this.memberId)
                .scheduleName(scheduleName)
                .workType(this.workType)
                .startTime(this.startTime)
                .endTime(this.endTime)
                .workMinutes(this.workMinutes)
                .breakMinutes(this.breakMinutes)
                .effectiveFrom(this.effectiveFrom)
                .effectiveTo(this.effectiveTo)
                .build();
    }
}
