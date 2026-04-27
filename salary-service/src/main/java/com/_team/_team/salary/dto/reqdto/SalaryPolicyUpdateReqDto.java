package com._team._team.salary.dto.reqdto;

import com._team._team.salary.domain.enums.PayDayShiftRule;
import com._team._team.salary.domain.enums.WageSystemType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class SalaryPolicyUpdateReqDto {

    @NotNull(message = "정책명은 필수입니다.")
    private String policyName;

    @NotNull(message = "지급일은 필수입니다.")
    @Min(value = 1, message = "지급일은 1이상이어야합니다.")
    @Max(value = 31, message = "지급일은 31이하여야합니다.")
    private Integer payDay;

    @NotNull
    @Builder.Default
    private PayDayShiftRule payDayShiftRule = PayDayShiftRule.BEFORE;

    @NotNull(message = "호봉제 또는 연봉협상제 선택은 필수 입니다.")
    @Builder.Default
    private String usePayGradeYn = "N";

    @NotNull(message = "임금제 유형은 필수입니다.")
    private WageSystemType wageSystemType;

    @Min(value = 0, message = "고정 오버타임시간(분)은 0 이상이어야 합니다.")
    private Integer fixedOvertimeMinutes;

    @NotNull(message = "적용 시작일은 필수입니다.")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

}