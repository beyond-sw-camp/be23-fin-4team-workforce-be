package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.InputType;
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
public class PerformanceRecordCreateReqDto {

    @NotNull(message = "입력 유형은 필수입니다.")
    private InputType inputType;

    private BigDecimal actualValue;

    private String description;

    private BigDecimal selfScore;
}
