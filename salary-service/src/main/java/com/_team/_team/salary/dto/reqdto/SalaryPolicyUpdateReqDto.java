package com._team._team.salary.dto.reqdto;

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

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
public class SalaryPolicyUpdateReqDto {

    @NotNull(message = "정책명은 필수입니다.")
    private String policyName;

    @NotNull(message = "지급 유형은 필수입니다.")
    private PayType payType;

    @NotNull(message = "지급일은 필수입니다.")
    @Min(value = 1, message = "지급일은 1이상이어야합니다.")
    @Max(value = 31, message = "지급일은 31이하여야합니다.")
    private Integer payDay;

    @Min(value = 1, message = "초과근무 인정 단위는 1분 이상이어야 합니다.")
    private Integer overtimeRoundingMinutes;

    @NotNull(message = "정산 시작 기준은 필수입니다.")
    private PeriodStartType periodStartType;

    @NotNull(message = "정산 종료 기준은 필수입니다.")
    private PeriodEndType periodEndType;

    @NotNull(message = "적용 시작일은 필수입니다.")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
}