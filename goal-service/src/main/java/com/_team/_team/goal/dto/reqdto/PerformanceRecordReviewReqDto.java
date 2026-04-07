package com._team._team.goal.dto.reqdto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class PerformanceRecordReviewReqDto {

    @NotNull
    private boolean confirmed;

    private BigDecimal convertedScore;

    private String rejectReason;

    @NotNull
    private UUID reviewedBy;
}
