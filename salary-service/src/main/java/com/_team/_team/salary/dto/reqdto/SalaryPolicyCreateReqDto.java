package com._team._team.salary.dto.reqdto;

import com._team._team.salary.domain.SalaryPolicy;
import com._team._team.salary.domain.enums.PayType;
import com._team._team.salary.domain.enums.WageSystemType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
public class SalaryPolicyCreateReqDto {

    @NotNull(message = "정책명은 필수 입니다.")
    private String policyName;

    @NotNull(message = "지급 유형은 필수 입니다.")
    private PayType payType;

    @NotNull(message = "지급일은 필수 입니다.")
    @Min(value=1, message = "지급일은 1이상이어야합니다.")
    @Max(value=31, message = "지급일은 31이하여야합니다.")
    private Integer payDay;

    @NotNull(message = "임금제 유형은 필수입니다.")
    private WageSystemType wageSystemType;

    /**
     * 포괄임금제일 때 포함된 고정 오버타임시간(분)
     * - COMPREHENSIVE이면 1 이상 필수
     * - NON_COMPREHENSIVE이면 0
     */
    @Min(value = 0, message = "고정 오버타임시간(분)은 0 이상이어야 합니다.")
    private Integer fixedOvertimeMinutes;

    @NotNull(message = "적용 시작일은 필수입니다.")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    public SalaryPolicy toEntity(UUID companyId) {
        return SalaryPolicy.builder()
                .companyId(companyId)
                .policyName(policyName)
                .payType(payType)
                .payDay(payDay)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .wageSystemType(wageSystemType)
                .fixedOvertimeMinutes(
                        wageSystemType == WageSystemType.NON_COMPREHENSIVE
                                ? 0
                                : (fixedOvertimeMinutes == null ? 0 : fixedOvertimeMinutes)
                )
                .build();
    }
}
