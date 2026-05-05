package com._team._team.attendance.dto.resDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * 직원 월별 OT 누적 vs 회사 월 한도 현황
 * 관리자 [초과 근무 현황] 화면 행 단위
 * 실측(DailyAttendance) + 승인(OvertimeRequest) 둘 다 노출
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class OvertimeUsageResDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID memberId;
    private String name;
    private String organizationName;
    /** 이번 달 누적 실측 OT 분 (DailyAttendance.overtimeMinutes 합) */
    private Integer actualOvertimeMinutes;
    /** 이번 달 누적 승인 OT 분 (OvertimeRequest APPROVED 합) */
    private Integer approvedMinutes;
    /** 회사 월 한도 분 (OvertimePolicy.monthlyOvertimeLimitMinutes) */
    private Integer fixedLimit;
    /** 사용률 % (actualOvertimeMinutes / fixedLimit * 100) */
    private Double usagePercent;
    /** 한도 초과 분 (없으면 0) */
    private Integer exceedMinutes;
}
