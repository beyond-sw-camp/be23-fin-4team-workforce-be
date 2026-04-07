package com._team._team.approval.service;

import com._team._team.approval.domain.Approval;
import com._team._team.approval.domain.ApprovalRequest;
import com._team._team.approval.domain.ApprovalViewer;
import com._team._team.approval.domain.enums.LineStatus;
import com._team._team.approval.domain.enums.RequestStatus;
import com._team._team.approval.dto.reqdto.ApprovalActionReqDto;
import com._team._team.approval.dto.resdto.ApprovalRequestResDto;
import com._team._team.approval.repository.ApprovalRepository;
import com._team._team.approval.repository.ApprovalViewerRepository;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final ApprovalViewerRepository approvalViewerRepository;

    @Autowired
    public ApprovalService(ApprovalRepository approvalRepository, ApprovalViewerRepository approvalViewerRepository) {
        this.approvalRepository = approvalRepository;
        this.approvalViewerRepository = approvalViewerRepository;
    }

    // 결재 대기함 (내가 결재할 문서 목록)
    @Transactional(readOnly = true)
    public List<ApprovalRequestResDto> findPending(UUID memberPositionId) {
        List<Approval> pendingApprovals =
                approvalRepository.findPendingByMemberPositionId(memberPositionId, LineStatus.PENDING);

        return pendingApprovals.stream()
                .map(approval -> {
                    UUID requestId = approval.getRequest().getRequestId();
                    List<Approval> allApprovals =
                            approvalRepository.findByRequestIdWithRequest(requestId);
                    List<ApprovalViewer> viewers =
                            approvalViewerRepository.findByRequestId(requestId);
                    return ApprovalRequestResDto.fromEntity(
                            approval.getRequest(), allApprovals, viewers);
                })
                .toList();
    }

    // 결재 완료함 (내가 결재한 문서 목록)
    @Transactional(readOnly = true)
    public List<ApprovalRequestResDto> findActed(UUID memberPositionId) {
        List<Approval> actedApprovals =
                approvalRepository.findActedByMemberPositionId(
                        memberPositionId, List.of(LineStatus.APPROVED, LineStatus.REJECTED));

        return actedApprovals.stream()
                .map(approval -> {
                    UUID requestId = approval.getRequest().getRequestId();
                    List<Approval> allApprovals =
                            approvalRepository.findByRequestIdWithRequest(requestId);
                    List<ApprovalViewer> viewers =
                            approvalViewerRepository.findByRequestId(requestId);
                    return ApprovalRequestResDto.fromEntity(
                            approval.getRequest(), allApprovals, viewers);
                })
                .toList();
    }

//    승인 처리
    public ApprovalRequestResDto approve(UUID memberPositionId, UUID approvalId, ApprovalActionReqDto reqDto){
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 라인을 찾을 수 없습니다."));

        if (!approval.getApproverMemberPositionId().equals(memberPositionId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "본인의 결재건만 처리할 수 있습니다.");
        }

        if (approval.getApprovalStatus() != LineStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "결재 대기(PENDING) 상태에서만 승인할 수 있습니다.");
        }

//        현재 단계 승인 처리
        approval.approve(reqDto.getComment());

        ApprovalRequest request = approval.getRequest();
        UUID requestId = request.getRequestId();

        Integer maxStep = approvalRepository.findMaxStepOrderByRequestId(requestId).orElse(0);

        if (approval.getStepOrder() < maxStep) {
            Approval nextApproval = approvalRepository
                    .findByRequestAndStepOrder(request, approval.getStepOrder() + 1)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                            "다음 결재 단계를 찾을 수 없습니다."));
            nextApproval.activate();

            if (request.getRequestStatus() == RequestStatus.WAIT) {
                request.updateStatus(RequestStatus.PENDING);
            }
        } else {
            request.updateStatus(RequestStatus.APPROVED);
        }

        List<Approval> allApprovals =
                approvalRepository.findByRequestIdWithRequest(requestId);
        List<ApprovalViewer> viewers =
                approvalViewerRepository.findByRequestId(requestId);

        return ApprovalRequestResDto.fromEntity(request, allApprovals, viewers);
    }


    public ApprovalRequestResDto reject(UUID memberPositionId, UUID approvalId,
                                        ApprovalActionReqDto reqDto) {

        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 라인을 찾을 수 없습니다."));

        if (!approval.getApproverMemberPositionId().equals(memberPositionId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "본인의 결재건만 처리할 수 있습니다.");
        }

        if (approval.getApprovalStatus() != LineStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "결재 대기(PENDING) 상태에서만 반려할 수 있습니다.");
        }

        if (reqDto.getComment() == null || reqDto.getComment().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "반려 사유는 필수입니다.");
        }

        // 현재 결재라인 반려
        approval.reject(reqDto.getComment());

        // 나머지 WAITING 라인 → REJECTED 처리
        ApprovalRequest request = approval.getRequest();
        UUID requestId = request.getRequestId();

        List<Approval> allApprovals = approvalRepository.findByRequestIdWithRequest(requestId);
        for (Approval a : allApprovals) {
            if (a.getApprovalStatus() == LineStatus.WAITING) {
                a.reject(null);
            }
        }

        // 결재요청 상태 → REJECTED
        request.updateStatus(RequestStatus.REJECTED);

        List<ApprovalViewer> viewers = approvalViewerRepository.findByRequestId(requestId);
        return ApprovalRequestResDto.fromEntity(request, allApprovals, viewers);
    }




}
