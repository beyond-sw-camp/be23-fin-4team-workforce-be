package com._team._team.goal.dto.resdto;

import com._team._team.goal.domain.enums.GoalHealthStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GoalProgressUpdateResDto {
    private UUID updateId;
    private UUID goalId;
    private BigDecimal value;
    private GoalHealthStatus status;
    private String note;
    private UUID createdBy;
    private LocalDateTime createdAt;
}
