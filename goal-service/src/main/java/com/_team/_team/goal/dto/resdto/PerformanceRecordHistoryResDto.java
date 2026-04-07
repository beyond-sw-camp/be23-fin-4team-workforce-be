package com._team._team.goal.dto.resdto;

import com._team._team.goal.domain.PerformanceRecordHistory;
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
public class PerformanceRecordHistoryResDto {

    private UUID performanceRecordHistoryId;
    private UUID performanceRecordId;
    private BigDecimal beforeValue;
    private BigDecimal afterValue;
    private String beforeDesc;
    private String afterDesc;
    private String changeReason;
    private UUID changedBy;
    private LocalDateTime changedAt;

    public static PerformanceRecordHistoryResDto from(PerformanceRecordHistory history) {
        return PerformanceRecordHistoryResDto.builder()
                .performanceRecordHistoryId(history.getPerformanceRecordHistoryId())
                .performanceRecordId(history.getPerformanceRecord() != null
                        ? history.getPerformanceRecord().getPerformanceRecordId()
                        : null)
                .beforeValue(history.getBeforeValue())
                .afterValue(history.getAfterValue())
                .beforeDesc(history.getBeforeDesc())
                .afterDesc(history.getAfterDesc())
                .changeReason(history.getChangeReason())
                .changedBy(history.getChangedBy())
                .changedAt(history.getChangedAt())
                .build();
    }
}
