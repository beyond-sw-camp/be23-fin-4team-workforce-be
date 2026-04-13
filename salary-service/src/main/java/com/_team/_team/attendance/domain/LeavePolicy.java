package com._team._team.attendance.domain;

import com._team._team.attendance.domain.enums.AccrualBase;
import com._team._team.attendance.dto.reqDto.LeavePolicyUpdateReqDto;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 연차 정책 (회사별 1건)
 * - 촉진제도: 연차 소진 독려 알림 기한 설정 (근로기준법 제61조)
 * - 이월: 미사용 연차 다음 해 이월 여부/일수
 * - 미사용 수당: 소멸 연차에 대한 수당 지급 여부 (salary-service 연동)
 * - accrualBase: 연차 발생 기준 (회계연도 vs 입사일)
 */

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Getter
public class LeavePolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID policyId;

    @Column(nullable = false)
    private UUID companyId;

    /** 촉진제도 사용 여부 (근로기준법 제61조 연차 사용 촉진) */
    @Column(length = 1, nullable = false)
    @Builder.Default
    private String isPromotionYn = "N";

    /** 1차 촉진 알림 — 만료일 N일 전 (ex: 180일 전) */
    private Integer promotion1stBeforeDays;

    /** 2차 촉진 알림 — 만료일 N일 전 (ex: 90일 전) */
    private Integer promotion2ndBeforeDays;

    /** 이월 허용 여부 */
    @Column(length = 1, nullable = false)
    @Builder.Default
    private String isCarryoverYn = "N";

    /** 최대 이월 일수 (isCarryoverYn = 'Y'일 때만 의미) */
    private Integer carryoverDays;

    /** 이월 시 본인 동의 필요 여부 */
    @Column(length = 1)
    @Builder.Default
    private String isCarryoverConsentYn = "N";

    /** 미사용 연차 수당 지급 여부 (salary-service에서 참조) */
    @Column(length = 1, nullable = false)
    @Builder.Default
    private String isPayoutYn = "N";

    /**
     * 기본 부여 일수 (회사 커스텀)
     * FISCAL 모드: 1/1 일괄 부여 시 이 값만큼 부여 , HIRE_DATE 모드: 입사 기념일에 이 값만큼 부여
     * ===== 근로기준법 : 1년 이상 재직자 기준 법정 최소 15일.
     * (근속 연수별 가산은 추후 확장 — 일단 단일 값으로 시작) =====
     */
    @Column(nullable = false)
    @Builder.Default
    private Double defaultAnnualDays = 15.0;

    /** 연차 발생 기준 (FISCAL: 회계연도 1/1 / HIRE_DATE: 입사일 기준) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccrualBase accrualBase;

    @Column(length = 1, nullable = false)
    @Builder.Default
    private String delYn = "N";

    public void update(LeavePolicyUpdateReqDto reqDto) {
        if (reqDto.getIsPromotionYn() != null) {
            this.isPromotionYn = reqDto.getIsPromotionYn();
        }
        if (reqDto.getPromotion1stBeforeDays() != null) {
            this.promotion1stBeforeDays = reqDto.getPromotion1stBeforeDays();
        }
        if (reqDto.getPromotion2ndBeforeDays() != null) {
            this.promotion2ndBeforeDays = reqDto.getPromotion2ndBeforeDays();
        }
        if (reqDto.getIsCarryoverYn() != null) {
            this.isCarryoverYn = reqDto.getIsCarryoverYn();
        }
        if (reqDto.getCarryoverDays() != null) {
            this.carryoverDays = reqDto.getCarryoverDays();
        }
        if (reqDto.getIsCarryoverConsentYn() != null) {
            this.isCarryoverConsentYn = reqDto.getIsCarryoverConsentYn();
        }
        if (reqDto.getIsPayoutYn() != null) {
            this.isPayoutYn = reqDto.getIsPayoutYn();
        }
        if (reqDto.getDefaultAnnualDays() != null) {
            this.defaultAnnualDays = reqDto.getDefaultAnnualDays();
        }
        if (reqDto.getAccrualBase() != null) {
            this.accrualBase = reqDto.getAccrualBase();
        }
    }

    public void delete() {
        this.delYn = "Y";
    }
}
