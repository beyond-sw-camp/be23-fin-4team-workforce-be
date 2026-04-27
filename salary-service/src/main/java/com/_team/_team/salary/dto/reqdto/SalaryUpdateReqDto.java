package com._team._team.salary.dto.reqdto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@Builder
@Data
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

    /** 부양가족수 간이세액표 룩업용 미입력 시 1 본인 만 */
    @Min(value = 0, message = "부양가족수는 0 이상이어야 합니다.")
    @Max(value = 11, message = "부양가족수는 11 이하여야 합니다.")
    private Integer dependentCount;
}
