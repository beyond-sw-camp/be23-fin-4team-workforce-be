package com._team._team.salary.dto.resdto;

import com._team._team.salary.domain.SalaryPolicy;
import com._team._team.salary.domain.enums.PayDayShiftRule;
import com._team._team.salary.domain.enums.PayType;
import com._team._team.salary.domain.enums.WageSystemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SalaryPolicyResDto {
    private UUID salaryPolicyId;
    private UUID companyId;
    private String policyName;
    private PayType payType;
    private Integer payDay;
    private PayDayShiftRule payDayShiftRule;
    private String usePayGradeYn;
    private WageSystemType wageSystemType;
    private Integer fixedOvertimeMinutes;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SalaryPolicyResDto fromEntity(SalaryPolicy salaryPolicy){
        return SalaryPolicyResDto.builder()
                .salaryPolicyId(salaryPolicy.getSalaryPolicyId())
                .companyId(salaryPolicy.getCompanyId())
                .policyName(salaryPolicy.getPolicyName())
                .payType(salaryPolicy.getPayType())
                .payDay(salaryPolicy.getPayDay())
                .payDayShiftRule(salaryPolicy.getPayDayShiftRule())
                .usePayGradeYn(salaryPolicy.getUsePayGradeYn())
                .wageSystemType(salaryPolicy.getWageSystemType())
                .fixedOvertimeMinutes(salaryPolicy.getFixedOvertimeMinutes())
                .effectiveFrom(salaryPolicy.getEffectiveFrom())
                .effectiveTo(salaryPolicy.getEffectiveTo())
                .createdAt(salaryPolicy.getCreatedAt())
                .updatedAt(salaryPolicy.getUpdatedAt())
                .build();
    }
}
