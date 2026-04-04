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

    @NotNull
    private InputType inputType;

    private BigDecimal actualValue;

    private String description;

    private BigDecimal selfScore;

    @NotNull
    private UUID submittedBy;
}
