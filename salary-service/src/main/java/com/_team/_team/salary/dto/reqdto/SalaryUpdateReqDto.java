package com._team._team.salary.dto.reqdto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SalaryUpdateReqDto {

    @NotNull(message = "급여 정책 ID는 필수 입니다.")
    private UUID salaryPolicyId;

    @NotNull(message = "기본급은 필수 입니다.")
    private Long baseSalary;

    @Positive
    private Integer step;

    private String jobGradeName;

    private String jobTitleName;

    @NotNull(message = "적용 시작일은 필수입니다.")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}
