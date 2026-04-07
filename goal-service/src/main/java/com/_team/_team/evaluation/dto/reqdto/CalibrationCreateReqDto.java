package com._team._team.evaluation.dto.reqdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class CalibrationCreateReqDto {

    @NotNull
    private UUID evaluationId;

    @NotNull
    private UUID adjustedBy;

    private String beforeGrade;
    private String afterGrade;
    private BigDecimal beforeScore;
    private BigDecimal afterScore;

    @NotBlank
    private String reason;
}
