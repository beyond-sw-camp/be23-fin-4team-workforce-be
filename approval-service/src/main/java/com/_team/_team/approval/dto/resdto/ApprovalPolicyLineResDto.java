package com._team._team.approval.dto.resdto;

import com._team._team.approval.domain.ApprovalPolicyLine;
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
    private UUID jobGradeId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ApprovalPolicyLineResDto fromEntity(ApprovalPolicyLine entity) {
        return ApprovalPolicyLineResDto.builder()
                .policyLineId(entity.getPolicyLineId())
                .documentId(entity.getApprovalDocument().getDocumentId())
                .jobGradeId(entity.getJobGradeId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
