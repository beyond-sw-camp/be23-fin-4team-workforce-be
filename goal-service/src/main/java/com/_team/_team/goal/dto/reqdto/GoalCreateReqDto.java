package com._team._team.goal.dto.reqdto;

import com._team._team.goal.domain.enums.GoalOwnerType;
import com._team._team.goal.domain.enums.GoalVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalCreateReqDto {
    @NotNull
    private UUID companyId;
    private UUID parentGoalId;
    private UUID cycleId;
    @NotNull
    private GoalOwnerType ownerType;
    @NotNull
    private UUID ownerId;
    @NotBlank
    private String title;
    private String description;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
    @NotNull
    private BigDecimal baseline;
    @NotNull
    private BigDecimal targetValue;
    @Builder.Default
    private Boolean isAutoUpdated = false;
    @Builder.Default
    private GoalVisibility visibility = GoalVisibility.PRIVATE;
}
