package com._team._team.attendance.dto.reqDto;

import com._team._team.attendance.domain.enums.AccrualBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LeavePolicyUpdateReqDto {

    /** 연차 촉진제도 사용 여부 */
    private String isPromotionYn;

    /** 1차 촉진 통보 기한 (일) */
    private Integer promotion1stBeforeDays;

    /** 2차 촉진 통보 기한 (일) */
    private Integer promotion2ndBeforeDays;

    /** 연차 이월 허용 여부 */
    private String isCarryoverYn;

    /** 이월 가능 일수 */
    private Integer carryoverDays;

    /** 이월 시 직원 동의 필요 여부 */
    private String isCarryoverConsentYn;

    /** 미사용 연차 수당 지급 여부 */
    private String isPayoutYn;

    /** 연차 발생 기준 (FISCAL: 회계연도 기준, HIRE_DATE: 입사일 기준) */
    private AccrualBase accrualBase;

    private Double defaultAnnualDays;
}
