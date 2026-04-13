package com._team._team.goal.dto.reqdto;

import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class KpiTemplateGenerateReqDto {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private UUID parentGoalId;
    private List<KpiGenerateOwnerMappingReqDto> ownerMapping;
    @Valid
    private GoalApprovalRequestCreateReqDto approval;
}
