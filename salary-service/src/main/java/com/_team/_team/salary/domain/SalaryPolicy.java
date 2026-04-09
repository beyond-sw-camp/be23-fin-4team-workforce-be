package com._team._team.salary.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.salary.domain.enums.PayType;
import com._team._team.salary.domain.enums.PeriodEndType;
import com._team._team.salary.domain.enums.PeriodStartType;
import com._team._team.salary.dto.reqdto.SalaryPolicyUpdateReqDto;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PayType payType;

    @Column(nullable = false)
    private Integer payDay;

    /** 초과근무 인정 단위(분) - null이면 1분 단위(절사 없음) */
    private Integer overtimeRoundingMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodStartType periodStartType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodEndType periodEndType;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    /** 삭제 여부 */
    @Column(length = 1)
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
        this.payType = reqDto.getPayType();
        this.payDay = reqDto.getPayDay();
        this.periodStartType = reqDto.getPeriodStartType();
        this.periodEndType = reqDto.getPeriodEndType();
        this.effectiveFrom = reqDto.getEffectiveFrom();
        this.effectiveTo = reqDto.getEffectiveTo();
        this.overtimeRoundingMinutes = reqDto.getOvertimeRoundingMinutes();
    }

    // 소프트 삭제
    public void softDelete() {
        this.delYn = "Y";
    }
}
