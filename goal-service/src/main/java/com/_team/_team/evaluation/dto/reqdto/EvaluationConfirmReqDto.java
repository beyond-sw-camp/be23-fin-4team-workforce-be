package com._team._team.evaluation.dto.reqdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationConfirmReqDto {

    @NotNull(message = "최종 점수는 필수입니다.")
    private BigDecimal finalScore;

    @NotBlank(message = "등급은 필수입니다.")
    private String grade;
}