package com._team._team.salary.dto.reqdto;

import com._team._team.salary.domain.SalaryPolicy;
import com._team._team.salary.domain.enums.PayType;
import com._team._team.salary.domain.enums.PeriodEndType;
import com._team._team.salary.domain.enums.PeriodStartType;
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

    @NotNull(message = "정산 시작 기준은 필수입니다.")
    private PeriodStartType periodStartType;

    @NotNull(message = "정산 종료 기준은 필수입니다.")
    private PeriodEndType periodEndType;

    @NotNull(message = "적용 시작일은 필수입니다.")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    public SalaryPolicy toEntity(UUID companyId) {
        return SalaryPolicy.builder()
                .companyId(companyId)
                .policyName(policyName)
                .payType(payType)
                .payDay(payDay)
                .periodStartType(periodStartType)
                .periodEndType(periodEndType)
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .build();
    }
}
