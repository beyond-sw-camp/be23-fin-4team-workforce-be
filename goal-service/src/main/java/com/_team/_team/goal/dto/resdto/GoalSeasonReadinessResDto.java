package com._team._team.goal.dto.resdto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GoalSeasonReadinessResDto {
    private UUID seasonId;
    private boolean ready;
    private int targetMemberCount;
    private int activeGoalCount;
    private int blockerCount;
    private int warningCount;
    private List<MemberIssue> missingGoals;
    private List<MemberIssue> weightIssues;
    private List<MemberIssue> pendingBundles;
    private List<MemberIssue> missingProgressUpdates;
    private List<MemberIssue> missingLeads;

    @Data
    @Builder
    public static class MemberIssue {
        private UUID memberId;
        private String reason;
        private Integer weightSum;
        private Integer goalCount;
    }
}
