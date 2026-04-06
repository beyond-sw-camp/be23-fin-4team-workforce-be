package com._team._team.goal.dto.reqdto;

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
public class PerformanceRecordReviewReqDto {

    @NotNull(message = "승인 확정 여부는 필수입니다.")
    private Boolean confirmed;

    private BigDecimal convertedScore;

    private String rejectReason;
}
