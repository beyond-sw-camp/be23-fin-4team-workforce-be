package com._team._team.approval.dtos.resdto;

import com._team._team.approval.domain.ApprovalPolicyLine;
import com._team._team.approval.domain.enums.ApproverType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApprovalPolicyLineResDto {

    private UUID policyLineId;
    private UUID documentId;
    private Integer stepOrder;
    private ApproverType approverType;
    private UUID specificMemberPositionId;
    private UUID jobGradeId;
    private UUID jobTitleId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ApprovalPolicyLineResDto fromEntity(ApprovalPolicyLine entity) {
        return ApprovalPolicyLineResDto.builder()
                .policyLineId(entity.getPolicyLineId())
                .documentId(entity.getApprovalDocument().getDocumentId())
                .stepOrder(entity.getStepOrder())
                .approverType(entity.getApproverType())
                .specificMemberPositionId(entity.getSpecificMemberPositionId())
                .jobGradeId(entity.getJobGradeId())
                .jobTitleId(entity.getJobTitleId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
