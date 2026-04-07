package com._team._team.goal.dto.resdto;

import com._team._team.goal.domain.PerformanceRecord;
import com._team._team.goal.domain.enums.InputType;
import com._team._team.goal.domain.enums.ReviewStatus;
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
public class PerformanceRecordResDto {

    private UUID performanceRecordId;
    private UUID goalId;
    private InputType inputType;
    private BigDecimal actualValue;
    private String description;
    private BigDecimal selfScore;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private UUID submittedBy;
    private ReviewStatus reviewStatus;
    private BigDecimal convertedScore;
    private String rejectReason;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PerformanceRecordResDto from(PerformanceRecord record) {
        return PerformanceRecordResDto.builder()
                .performanceRecordId(record.getPerformanceRecordId())
                .goalId(record.getGoal() != null ? record.getGoal().getGoalId() : null)
                .inputType(record.getInputType())
                .actualValue(record.getActualValue())
                .description(record.getDescription())
                .selfScore(record.getSelfScore())
                .fileUrl(record.getFileUrl())
                .fileName(record.getFileName())
                .fileSize(record.getFileSize())
                .submittedBy(record.getSubmittedBy())
                .reviewStatus(record.getReviewStatus())
                .convertedScore(record.getConvertedScore())
                .rejectReason(record.getRejectReason())
                .reviewedBy(record.getReviewedBy())
                .reviewedAt(record.getReviewedAt())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
