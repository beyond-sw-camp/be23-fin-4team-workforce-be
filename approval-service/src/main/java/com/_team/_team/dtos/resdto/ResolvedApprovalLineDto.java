package com._team._team.dtos.resdto;

import com._team._team.domain.enums.ApproverType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * 결재 정책 라인을 실제 결재자로 변환한 결과 DTO
 * 프론트에서 "결재선 미리보기"로 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResolvedApprovalLineDto {

    private UUID policyLineId;          // 원본 정책 라인 ID
    private Integer stepOrder;          // 결재 순서
    private Integer parallelGroupId;    // 병렬 그룹 (null이면 단독)
    private ApproverType approverType;  // 결재자 유형
    private List<ApproverCandidateDto> candidates; // 해당 단계의 결재자 후보 목록


}
