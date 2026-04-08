package com._team._team.attendance.dto.reqDto;

import com._team._team.attendance.domain.LeavePolicy;
import com._team._team.attendance.domain.enums.AccrualBase;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LeavePolicyCreateReqDto {

    /** 촉진제도 사용 여부 */
    private String isPromotionYn;

    /** 1차 촉진 알림 기한(일) */
    private Integer promotion1stBeforeDays;

    /** 2차 촉진 알림 기한(일) */
    private Integer promotion2ndBeforeDays;

    /** 이월 허용 여부 */
    private String isCarryoverYn;

    /** 최대 이월 일수 */
    private Integer carryoverDays;

    /** 이월 동의 필요 여부 */
    private String isCarryoverConsentYn;

    /** 미사용 수당 지급 여부 */
    private String isPayoutYn;

    @NotNull(message = "연차 발생 기준은 필수 입니다.")
    private AccrualBase accrualBase;

    public LeavePolicy toEntity(UUID companyId) {
        return LeavePolicy.builder()
                .companyId(companyId)
                .isPromotionYn(this.isPromotionYn != null ? this.isPromotionYn : "N")
                .promotion1stBeforeDays(this.promotion1stBeforeDays)
                .promotion2ndBeforeDays(this.promotion2ndBeforeDays)
                .isCarryoverYn(this.isCarryoverYn != null ? this.isCarryoverYn : "N")
                .carryoverDays(this.carryoverDays)
                .isCarryoverConsentYn(this.isCarryoverConsentYn != null ? this.isCarryoverConsentYn : "N")
                .isPayoutYn(this.isPayoutYn != null ? this.isPayoutYn : "N")
                .accrualBase(this.accrualBase)
                .build();
    }
}
