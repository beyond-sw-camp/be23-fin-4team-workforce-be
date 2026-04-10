package com._team._team.goal.dto.reqdto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class GoalApprovalRequestCreateReqDto {
    @NotNull
    private UUID approverId;
}
