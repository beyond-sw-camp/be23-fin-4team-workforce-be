package com._team._team.attendance.dto.reqDto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 슬롯 선택 제출 요청 DTO
 * 1. 월 시작 전 첫 선택 (requestReason null 허용)
 * 2. 월 중 슬롯 변경 (requestReason 필수)
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class MemberScheduleSelectionReqDto {

    @NotNull
    private UUID memberId;

    /** "YYYY-MM" 형식, 예: "2026-05" */
    @NotBlank
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$",
            message = "형식은 YYYY-MM 이어야 합니다.")
    private String targetYearMonth;

    /** 선택한 슬롯 ID */
    @NotNull
    private UUID slotId;

    /**
     * 변경 사유
     * 월 중 변경 요청일 경우 필수
     * 최초 제출은 비워도 됨
     */
    @Size(max = 500)
    private String requestReason;
}
