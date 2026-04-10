package com._team._team.goal.dto.resdto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GoalApprovalBundleDetailResDto {
    private UUID requestId;
    /** pending | approved | rejected */
    private String status;
    private String rejectionReason;
    private List<GoalResDto> goals;
    private UUID approverId;
    private String decision;
    private LocalDateTime decidedAt;
    private String comment;
    private String completionSummary;
    private String completionEvidenceFiles;
    private List<GoalApprovalWatcherResDto> watchers;
}
