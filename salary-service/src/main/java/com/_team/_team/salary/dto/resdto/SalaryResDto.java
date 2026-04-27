package com._team._team.salary.dto.resdto;

import com._team._team.salary.domain.Salary;
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
public class SalaryResDto {

    private UUID salaryId;
    private UUID memberId;
    private UUID companyId;
    private UUID salaryPolicyId;
    private Long baseSalary;
    private Integer step;
    private String jobGradeName;
    private String jobTitleName;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer dependentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SalaryResDto fromEntity(Salary salary){
        return SalaryResDto.builder()
                .salaryId(salary.getSalaryId())
                .memberId(salary.getMemberId())
                .companyId(salary.getCompanyId())
                .salaryPolicyId(salary.getSalaryPolicyId())
                .baseSalary(salary.getBaseSalary())
                .step(salary.getStep())
                .jobGradeName(salary.getJobGradeName())
                .jobTitleName(salary.getJobTitleName())
                .effectiveFrom(salary.getEffectiveFrom())
                .effectiveTo(salary.getEffectiveTo())
                .dependentCount(salary.getDependentCount())
                .createdAt(salary.getCreatedAt())
                .updatedAt(salary.getUpdatedAt())
                .build();
    }

}
