package com._team._team.approval.dto.resdto;

import com._team._team.approval.domain.Approval;
import com._team._team.approval.domain.ApprovalRequest;
import com._team._team.approval.domain.ApprovalViewer;
import com._team._team.approval.domain.enums.RequestStatus;
import com._team._team.approval.domain.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApprovalRequestResDto {
    private UUID requestId;
    private UUID documentId;
    private String documentName;
    private UUID memberId;
    private RequestType requestType;
    private String contentJson;
    private RequestStatus requestStatus;
    private String cancelReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String documentNumber;                      // 공문 번호 (OFFICIAL만)
    private List<OfficialRecipientResDto> recipients;   // 수신 부서 목록 (OFFICIAL만)

    // === 요청자 스냅샷 (작성 시점 정보) ===
    private String requesterName;
    private UUID requesterOrganizationId;
    private String requesterOrganizationName;

    private List<ApprovalLineResDto> approvalLines;
    private List<ApprovalViewerResDto> viewers;

    public static ApprovalRequestResDto fromEntity(ApprovalRequest entity,
                                                   List<Approval> approvals,
                                                   List<ApprovalViewer> viewers,
                                                   List<OfficialRecipientResDto> recipients) {

        return ApprovalRequestResDto.builder()
                .requestId(entity.getRequestId())
                .documentId(entity.getApprovalDocument().getDocumentId())
                .documentName(entity.getApprovalDocument().getDocumentName())
                .memberId(entity.getMemberId())
                .requestType(entity.getRequestType())
                .contentJson(entity.getContentJson())
                .requestStatus(entity.getRequestStatus())
                .cancelReason(entity.getCancelReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .requesterName(entity.getRequesterName())
                .requesterOrganizationId(entity.getRequesterOrganizationId())
                .requesterOrganizationName(entity.getRequesterOrganizationName())
                .approvalLines(approvals.stream()
                        .map(ApprovalLineResDto::fromEntity)
                        .collect(Collectors.toList()))
                .viewers(viewers.stream()
                        .map(ApprovalViewerResDto::fromEntity)
                        .collect(Collectors.toList()))
                .documentNumber(entity.getDocumentNumber())
                .recipients(recipients)
                .build();
    }
}
