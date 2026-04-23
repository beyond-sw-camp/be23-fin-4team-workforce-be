package com._team._team.salary.dto.reqdto;

import com._team._team.salary.domain.SalaryItemTemplate;
import com._team._team.salary.domain.enums.ItemType;
import com._team._team.salary.domain.enums.TaxCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 급여 항목 템플릿 생성 요청 DTO
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
public class SalaryItemTemplateCreateReqDto {

    @NotNull(message = "항목명은 필수입니다.")
    private String itemName;

    @NotNull(message = "항목 유형은 필수입니다.")
    private ItemType itemType;

    @NotNull(message = "표시 순서는 필수입니다.")
    private Integer displayOrder;

    @NotNull(message = "과세 여부는 필수입니다.")
    private String isTaxableYn;

    public SalaryItemTemplate toEntity(UUID companyId) {
        return SalaryItemTemplate.builder()
                .companyId(companyId)
                .itemName(itemName)
                .itemType(itemType)
                .displayOrder(displayOrder)
                .isTaxableYn(isTaxableYn)
                // 커스텀 등록 시 자동 매핑
                .taxCategory("Y".equals(isTaxableYn)
                        ? TaxCategory.TAXABLE
                        : TaxCategory.ETC_NON_TAXABLE)
                .build();
    }
}
