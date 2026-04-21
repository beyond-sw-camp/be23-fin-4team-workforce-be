package com._team._team.attendance.domain;

import com._team._team.attendance.domain.enums.BalanceType;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 회사별 휴가 종류 정의
 * 법정 기본 8종은 회사 생성 시 자동 시드, 커스텀 휴가 추가 허용
 */
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Getter
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_company_leave_type_code",
                        columnNames = {"companyId", "code"})
        },
        indexes = {
                @Index(name = "idx_company_leave_type_company",
                        columnList = "companyId, delYn")
        }
)
public class CompanyLeaveType extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID companyLeaveTypeId;

    @Column(nullable = false)
    private UUID companyId;

    // 회사 내 고유 코드 (ANNUAL, HALF_AM, 또는 커스텀 "REFRESH" 등)
    @Column(nullable = false, length = 50)
    private String code;

    // 표시명 (연차, 반차(오전), 리프레시 휴가 등)
    @Column(nullable = false, length = 100)
    private String name;

    // 차감 대상 balance, null 이면 차감 안 함 (경조, 예비군 등)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BalanceType balanceType;

    // 1회 사용 시 차감 일수 (연차=1.0, 반차=0.5)
    @Column(nullable = false)
    private Double daysPerUse;

    // 시스템 기본 제공 여부, true 면 삭제 불가
    @Column(nullable = false)
    @Builder.Default
    private Boolean isSystemDefault = false;

    // 유급 여부
    @Column(nullable = false, length = 1)
    @Builder.Default
    private String isPaidYn = "Y";

    // 연간 최대 사용 일수, null 이면 제한 없음
    @Column
    private Double maxDaysPerYear;

    // 증빙 서류 필수 여부 (병가, 경조 등)
    @Column(nullable = false, length = 1)
    @Builder.Default
    private String requireEvidenceYn = "N";

    // 표시 순서
    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false, length = 1)
    @Builder.Default
    private String delYn = "N";

    /**
     * 사용자 수정 가능 필드만 반영, 시스템 기본은 name/order 만 허용
     */
    public void update(String name, Integer displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
    }

    /**
     * 커스텀 항목 전체 수정
     */
    public void updateCustom(String name, BalanceType balanceType,
                             Double daysPerUse, String isPaidYn,
                             Double maxDaysPerYear, String requireEvidenceYn,
                             Integer displayOrder) {
        if (Boolean.TRUE.equals(this.isSystemDefault)) {
            throw new IllegalStateException("시스템 기본 휴가는 이름과 순서만 수정 가능합니다.");
        }
        this.name = name;
        this.balanceType = balanceType;
        this.daysPerUse = daysPerUse;
        this.isPaidYn = isPaidYn;
        this.maxDaysPerYear = maxDaysPerYear;
        this.requireEvidenceYn = requireEvidenceYn;
        this.displayOrder = displayOrder;
    }

    public void softDelete() {
        if (Boolean.TRUE.equals(this.isSystemDefault)) {
            throw new IllegalStateException("시스템 기본 휴가는 삭제할 수 없습니다.");
        }
        this.delYn = "Y";
    }

    /**
     * 차감 대상 여부, 급여/잔고 처리 분기용
     */
    public boolean deductsBalance() {
        return this.balanceType != null;
    }
}