package com._team._team.dtos.reqdto;

import com._team._team.domain.enums.ApproverType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApprovalPolicyLineCreateDto {
    @NotNull
    private UUID documentId;

    @NotNull
    private Integer stepOrder;

    private Integer parallelGroupId; // null이면 단독, 동일값이면 병렬

    @NotNull
    private ApproverType approverType;

    // approverType에 따라 셋 중 하나만 필수
    private UUID specificMemberPositionId;
    private UUID jobGradeId;
    private UUID jobTitleId;
}
