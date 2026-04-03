package com._team._team.dtos.resdto;

import com._team._team.domain.enums.ApprovalStatus;
import com._team._team.domain.enums.RequestStatus;
import com._team._team.domain.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequestResponseDto {
    // Request 정보
    private UUID requestId;
    private UUID documentId;
    private String documentName;
    private RequestType requestType;
    private RequestStatus requestStatus;
    private UUID requesterId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private LocalDateTime createdAt;

    // Approval 정보
    private UUID approvalId;
    private ApprovalStatus approvalStatus;
    private Integer currentStep;

    // 결재선 목록
    private List<ApprovalLineDetailDto> approvalLines;


}

