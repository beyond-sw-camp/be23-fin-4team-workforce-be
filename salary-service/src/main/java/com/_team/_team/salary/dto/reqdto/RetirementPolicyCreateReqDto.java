package com._team._team.salary.dto.reqdto;

import com._team._team.salary.domain.enums.RetirementType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class RetirementPolicyCreateReqDto {

    @NotNull(message = "퇴직급여 제도는 필수입니다.")
    private RetirementType retirementType;

    @NotNull(message = "적용 시작일은 필수입니다.")
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private String memo;
}