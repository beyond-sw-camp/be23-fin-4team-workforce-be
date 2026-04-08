package com._team._team.goal.dto.resdto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GoalApprovalBundleSummaryResDto {
    private UUID requestId;
    private String status;
    private int goalCount;
    private LocalDateTime requestedAt;
    private String completionSummary;
    private String completionEvidenceFiles;
}
