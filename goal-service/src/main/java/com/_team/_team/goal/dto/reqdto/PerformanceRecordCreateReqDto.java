package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.InputType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class PerformanceRecordCreateReqDto {

    @NotNull(message = "입력 유형은 필수입니다.")
    private InputType inputType;

    private BigDecimal actualValue;

    private String description;

    private BigDecimal selfScore;

    @NotNull(message = "제출자 ID는 필수입니다.")
    private UUID submittedBy;
}
