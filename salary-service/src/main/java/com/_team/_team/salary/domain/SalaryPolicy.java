package com._team._team.salary.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.salary.domain.enums.PayDayShiftRule;
import com._team._team.salary.dto.reqdto.SalaryPolicyUpdateReqDto;
import com._team._team.salary.domain.enums.WageSystemType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryPolicy extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID salaryPolicyId;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String policyName;

    @Column(nullable = false)
    private Integer payDay;

    // 지급일(payDay)이 주말/공휴일인 경우 지급 날짜
    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    @Builder.Default
    private PayDayShiftRule payDayShiftRule = PayDayShiftRule.BEFORE;

    /**
     * 호봉제 사용 여부 (N 이면 연봉협상제)
     * Y: 호봉표 기반 기본급 자동 결정 (step 필수)
     * N: baseSalary 직접 입력 (기본값)
     */
    @Column(length = 1, nullable = false)
    @Builder.Default
    private String usePayGradeYn = "N";

    /**
     * 임금제 유형 (COMPREHENSIVE: 포괄 / NON_COMPREHENSIVE: 비포괄)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WageSystemType wageSystemType = WageSystemType.NON_COMPREHENSIVE;

    /**
     * 포괄임금제에서 기본급에 포함된 고정 오버타임시간(분)
     * - COMPREHENSIVE: 예) 1200 (월 20시간)
     * - NON_COMPREHENSIVE: 항상 0
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer fixedOvertimeMinutes = 0;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    /** 삭제 여부 */
    @Column(nullable = false, length = 1)
    @Builder.Default
    private String delYn = "N";

    // 과거 정책인지 확인 (정산 완료)
    public boolean isPast() {
        return effectiveTo != null && effectiveTo.isBefore(LocalDate.now());
    }

    // 현재 적용 중인 정책인지 확인
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        boolean started = !effectiveFrom.isAfter(today);
        boolean notEnded = effectiveTo == null || !effectiveTo.isBefore(today);
        return started && notEnded;
    }

    // 급여 정책 수정
    public void update(SalaryPolicyUpdateReqDto reqDto) {
        this.policyName = reqDto.getPolicyName();
        this.payDay = reqDto.getPayDay();
        this.effectiveFrom = reqDto.getEffectiveFrom();
        this.effectiveTo = reqDto.getEffectiveTo();
        this.usePayGradeYn = reqDto.getUsePayGradeYn();
        this.wageSystemType = reqDto.getWageSystemType();
        this.fixedOvertimeMinutes = reqDto.getWageSystemType() == WageSystemType.NON_COMPREHENSIVE
                ? 0
                : (reqDto.getFixedOvertimeMinutes() == null ? 0 : reqDto.getFixedOvertimeMinutes());
        if (reqDto.getPayDayShiftRule() != null) {
            this.payDayShiftRule = reqDto.getPayDayShiftRule();
        }
    }

    // 소프트 삭제
    public void softDelete() {
        this.delYn = "Y";
    }
}
