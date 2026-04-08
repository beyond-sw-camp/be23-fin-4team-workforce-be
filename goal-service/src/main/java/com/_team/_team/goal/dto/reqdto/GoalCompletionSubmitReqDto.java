package com._team._team.goal.dto.reqdto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class GoalCompletionSubmitReqDto {
    @NotNull
    private UUID approverId;

    private String summary;
    private String evidenceFiles;
}

