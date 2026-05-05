package com._team._team.attendance.dto.resDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * 포괄임금제 직원 월별 OT 누적 vs 고정 한도 현황
 * - 관리자 [초과 근무 현황] 화면 행 단위
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class ComprehensiveOvertimeStatusResDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID memberId;
    private String name;
    private String organizationName;
    /** 이번 달 누적 승인 OT 분 */
    private Integer approvedMinutes;
    /** 포괄임금 고정 한도 분 */
    private Integer fixedLimit;
    /** 사용률 % (approvedMinutes / fixedLimit * 100) */
    private Double usagePercent;
    /** 한도 초과 분 (없으면 0) */
    private Integer exceedMinutes;
}
