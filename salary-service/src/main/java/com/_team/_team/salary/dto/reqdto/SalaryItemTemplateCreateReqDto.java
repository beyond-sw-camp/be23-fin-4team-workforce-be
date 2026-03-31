package com._team._team.salary.dto.reqdto;

import com._team._team.salary.domain.SalaryItemTemplate;
import com._team._team.salary.domain.enums.ItemType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 급여 항목 템플릿 생성 요청 DTO
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SalaryItemTemplateCreateReqDto {

    @NotNull(message = "항목명은 필수입니다.")
    private String itemName;

    @NotNull(message = "항목 유형은 필수입니다.")
    private ItemType itemType;

    @NotNull(message = "과세 여부는 필수입니다.")
    private Boolean isTaxable;

    private Boolean isFixed;

    public SalaryItemTemplate toEntity(UUID companyId) {
        return SalaryItemTemplate.builder()
                .companyId(companyId)
                .itemName(itemName)
                .itemType(itemType)
                .isTaxable(isTaxable)
                .isFixed(isFixed != null ? isFixed : true)
                .build();
    }
}