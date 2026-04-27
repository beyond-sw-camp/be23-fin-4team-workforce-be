package com._team._team.salary.dto.resdto;


import com._team._team.salary.domain.RetirementPolicy;
import com._team._team.salary.domain.enums.RetirementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class RetirementPolicyResDto {
    private UUID retirementPolicyId;
    private UUID companyId;
    private RetirementType retirementType;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String memo;
    // 오늘 기준 활성 여부
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RetirementPolicyResDto fromEntity(RetirementPolicy p) {
        return RetirementPolicyResDto.builder()
                .retirementPolicyId(p.getRetirementPolicyId())
                .companyId(p.getCompanyId())
                .retirementType(p.getRetirementType())
                .effectiveFrom(p.getEffectiveFrom())
                .effectiveTo(p.getEffectiveTo())
                .memo(p.getMemo())
                .active(p.isActiveAt(LocalDate.now()))
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}