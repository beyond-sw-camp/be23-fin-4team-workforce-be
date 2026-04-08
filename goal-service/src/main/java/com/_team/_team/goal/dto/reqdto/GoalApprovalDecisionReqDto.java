package com._team._team.goal.dto.reqdto;

import lombok.Data;

@Data
public class GoalApprovalDecisionReqDto {
    private String comment;
    /** 반려 API 명세 필드 — 비어 있으면 {@link #comment} 사용 */
    private String reason;

    public String resolveNote() {
        if (reason != null && !reason.isBlank()) return reason.trim();
        return comment;
    }
}
