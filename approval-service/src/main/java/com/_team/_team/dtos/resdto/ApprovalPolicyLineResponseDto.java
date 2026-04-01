package com._team._team.dtos.resdto;

import com._team._team.domain.ApprovalPolicyLine;
import com._team._team.domain.enums.ApproverType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalPolicyLineResponseDto {

    private UUID policyLineId;
    private UUID documentId;
    private Integer stepOrder;
    private Integer parallelGroupId;
    private ApproverType approverType;
    private UUID specificMemberPositionId;
    private UUID jobGradeId;
    private UUID jobTitleId;

    public static ApprovalPolicyLineResponseDto fromEntity(ApprovalPolicyLine policyLine) {
        return ApprovalPolicyLineResponseDto.builder()
                .policyLineId(policyLine.getPolicyLineId())
                .documentId(policyLine.getApprovalDocument().getDocumentId())
                .stepOrder(policyLine.getStepOrder())
                .parallelGroupId(policyLine.getParallelGroupId())
                .approverType(policyLine.getApproverType())
                .specificMemberPositionId(policyLine.getSpecificMemberPositionId())
                .jobGradeId(policyLine.getJobGradeId())
                .jobTitleId(policyLine.getJobTitleId())
                .build();
    }
}
