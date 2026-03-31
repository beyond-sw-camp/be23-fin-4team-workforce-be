package com._team._team.salary.dto.reqdto;

import com._team._team.salary.domain.enums.ItemType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 급여 항목 템플릿 수정 요청 DTO
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
public class SalaryItemTemplateUpdateReqDto {

    @NotNull(message = "항목명은 필수입니다.")
    private String itemName;

    @NotNull(message = "항목 유형은 필수입니다.")
    private ItemType itemType;

    @NotNull(message = "표시 순서는 필수입니다.")
    private Integer displayOrder;

    @NotNull(message = "과세 여부는 필수입니다.")
    private String isTaxableYn;

}
