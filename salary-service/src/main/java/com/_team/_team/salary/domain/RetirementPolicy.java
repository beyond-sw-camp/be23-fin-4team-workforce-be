package com._team._team.salary.domain;


import com._team._team.domain.BaseTimeEntity;
import com._team._team.salary.domain.enums.RetirementType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

// 회사별 퇴직급여 제도 정책
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Getter
@Table(
        indexes = {
                @Index(name = "idx_retirement_policy_company_active",
                        columnList = "companyId, effectiveFrom")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_retirement_policy_company_effective_from",
                        columnNames = {"companyId", "effectiveFrom"}
                )
        }
)
public class RetirementPolicy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID retirementPolicyId;

    @Column(nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RetirementType retirementType;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    // 정책 등록 시 비고 메모 변경 사유 등
    @Column(length = 500)
    private String memo;

    @Column(nullable = false, length = 1)
    @Builder.Default
    private String delYn = "N";

    // 정책 마감 새 정책 등록 시 이전 활성 행 자동 종료용
    public void closeAt(LocalDate closingDate) {
        this.effectiveTo = closingDate;
    }

    // 정책 수정 effectiveFrom 은 변경 불가 effectiveTo 메모 retirementType 만 가능
    public void update(RetirementType retirementType, LocalDate effectiveTo, String memo) {
        this.retirementType = retirementType;
        this.effectiveTo = effectiveTo;
        this.memo = memo;
    }

    public void softDelete() {
        this.delYn = "Y";
    }

    // 특정 날짜 시점 활성 여부 판단
    public boolean isActiveAt(LocalDate date) {
        if ("Y".equals(delYn)) return false;
        if (effectiveFrom.isAfter(date)) return false;
        return effectiveTo == null || !effectiveTo.isBefore(date);
    }
}
