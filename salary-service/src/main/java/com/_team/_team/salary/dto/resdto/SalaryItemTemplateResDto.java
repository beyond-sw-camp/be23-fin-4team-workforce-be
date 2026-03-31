package com._team._team.salary.dto.resdto;

import com._team._team.salary.domain.SalaryItemTemplate;
import com._team._team.salary.domain.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 급여 항목 템플릿 응답 DTO
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SalaryItemTemplateResDto {

    private UUID salaryItemTemplateId;
    private UUID companyId;
    private String itemName;
    private ItemType itemType;
    private Boolean isTaxable;
    private Boolean isFixed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SalaryItemTemplateResDto fromEntity(SalaryItemTemplate template) {
        return SalaryItemTemplateResDto.builder()
                .salaryItemTemplateId(template.getSalaryItemTemplateId())
                .companyId(template.getCompanyId())
                .itemName(template.getItemName())
                .itemType(template.getItemType())
                .isTaxable(template.getIsTaxable())
                .isFixed(template.getIsFixed())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
