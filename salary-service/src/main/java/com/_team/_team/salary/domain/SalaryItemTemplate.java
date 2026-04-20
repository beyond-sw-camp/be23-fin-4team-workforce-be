package com._team._team.salary.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.salary.domain.enums.ItemType;
import com._team._team.salary.domain.enums.TaxCategory;
import com._team._team.salary.dto.reqdto.SalaryItemTemplateUpdateReqDto;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 급여 항목 템플릿 엔티티
 * - 회사별로 사용할 급여 구성 항목을 정의하는 마스터 테이블 (기본급, 식대, 소득세 등)
 * - 지급(EARNING) / 공제(DEDUCTION)로 구분
 * - 실제 정산 시 PayrollItem에서 이 템플릿을 참조하여 금액을 계산
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryItemTemplate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID salaryItemTemplateId;

    /** 소속 회사 ID (Gateway 헤더에서 전달받음) */
    @Column(nullable = false)
    private UUID companyId;

    /** 항목명 (예: 기본급, 식대, 교통비, 소득세, 국민연금) */
    @Column(nullable = false)
    private String itemName;

    /** 항목 유형 - EARNING(지급) / DEDUCTION(공제) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType itemType;

    // 세법 카테고리, 카탈로그 기반이면 자동 복사, 커스텀이면 관리자가 선택
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaxCategory taxCategory;

    /** 표시 순서 */
    @Column(nullable = false)
    private Integer displayOrder;

    /** 과세 여부 - Y이면 소득세 계산에 포함 */
    @Column(nullable = false, length = 1)
    private String isTaxableYn;

    /** 삭제 여부 */
    @Column(length = 1)
    @Builder.Default
    private String delYn = "N";

    /**
     * 급여 항목 템플릿 정보 수정
     */
    public void update(SalaryItemTemplateUpdateReqDto reqDto) {
        this.itemName = reqDto.getItemName();
        this.itemType = reqDto.getItemType();
        this.isTaxableYn = reqDto.getIsTaxableYn();
        this.displayOrder = reqDto.getDisplayOrder();
    }

    /** 소프트 삭제 */
    public void softDelete() {
        this.delYn = "Y";
    }
}
