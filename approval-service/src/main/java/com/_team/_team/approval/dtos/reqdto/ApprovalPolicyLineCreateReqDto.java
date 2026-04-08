package com._team._team.approval.dtos.reqdto;

import com._team._team.approval.domain.enums.ApproverType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApprovalPolicyLineCreateReqDto {

    @NotNull(message = "문서 ID는 필수입니다.")
    private UUID documentId;

    @NotNull(message = "결재라인 목록은 필수입니다.")
    @Valid
    private List<PolicyLineItem> policyLines;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class PolicyLineItem {

        @NotNull(message = "결재 순서는 필수입니다.")
        private Integer stepOrder;

        @NotNull(message = "결재자 유형은 필수입니다.")
        private ApproverType approverType;

        private UUID specificMemberPositionId; // SPECIFIC일 때

        private UUID jobGradeId; // JOB_GRADE일 때

        private UUID jobTitleId; // JOB_TITLE일 때
    }
}
