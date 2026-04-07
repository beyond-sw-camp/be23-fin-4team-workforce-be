package com._team._team.evaluation.dto.resdto;

import com._team._team.evaluation.domain.CalibrationLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalibrationLogResDto {

    private UUID calibrationId;
    private UUID evaluationId;
    private UUID adjustedBy;
    private String beforeGrade;
    private String afterGrade;
    private BigDecimal beforeScore;
    private BigDecimal afterScore;
    private String reason;
    private LocalDateTime adjustedAt;

    public static CalibrationLogResDto from(CalibrationLog log) {
        return CalibrationLogResDto.builder()
                .calibrationId(log.getCalibrationId())
                .evaluationId(log.getEvaluation().getEvaluationId())
                .adjustedBy(log.getAdjustedBy())
                .beforeGrade(log.getBeforeGrade())
                .afterGrade(log.getAfterGrade())
                .beforeScore(log.getBeforeScore())
                .afterScore(log.getAfterScore())
                .reason(log.getReason())
                .adjustedAt(log.getAdjustedAt())
                .build();
    }
}
