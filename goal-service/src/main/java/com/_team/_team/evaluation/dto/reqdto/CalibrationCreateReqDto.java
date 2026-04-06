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

    @NotNull(message = "평가 ID는 필수입니다.")
    private UUID evaluationId;

    @NotNull(message = "조정자 ID는 필수입니다.")
    private UUID adjustedBy;

    private String beforeGrade;
    private String afterGrade;
    private BigDecimal beforeScore;
    private BigDecimal afterScore;

    @NotBlank(message = "조정 사유는 필수입니다.")
    private String reason;
}
