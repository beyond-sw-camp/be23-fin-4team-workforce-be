package com._team._team.approval.service;

import com._team._team.approval.domain.AbsenceProxy;
import com._team._team.approval.domain.Approval;
import com._team._team.approval.domain.ApprovalRequest;
import com._team._team.approval.domain.ApprovalViewer;
import com._team._team.approval.domain.enums.LineStatus;
import com._team._team.approval.domain.enums.RequestStatus;
import com._team._team.approval.domain.enums.RequestType;
import com._team._team.approval.dto.reqdto.ApprovalActionReqDto;
import com._team._team.approval.dto.resdto.ApprovalRequestResDto;
import com._team._team.approval.dto.resdto.OfficialRecipientResDto;
import com._team._team.approval.repository.AbsenceProxyRepository;
import com._team._team.approval.repository.ApprovalRepository;
import com._team._team.approval.repository.ApprovalViewerRepository;
import com._team._team.approval.repository.OfficialRecipientRepository;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final ApprovalViewerRepository approvalViewerRepository;
    private final AbsenceProxyRepository absenceProxyRepository;
    private final ApprovalNotificationService approvalNotificationService;
    private final OfficialNumberService officialNumberService;
    private final OfficialRecipientRepository officialRecipientRepository;

    @Autowired
    public ApprovalService(ApprovalRepository approvalRepository, ApprovalViewerRepository approvalViewerRepository, AbsenceProxyRepository absenceProxyRepository, ApprovalNotificationService approvalNotificationService, OfficialNumberService officialNumberService, OfficialRecipientRepository officialRecipientRepository) {
        this.approvalRepository = approvalRepository;
        this.approvalViewerRepository = approvalViewerRepository;
        this.absenceProxyRepository = absenceProxyRepository;
        this.approvalNotificationService = approvalNotificationService;
        this.officialNumberService = officialNumberService;
        this.officialRecipientRepository = officialRecipientRepository;
    }


    // 결재 완료함 (내가 결재한 문서 목록)
    @Transactional(readOnly = true)
    public List<ApprovalRequestResDto> findActed(UUID memberId, UUID memberPositionId) {

        List<LineStatus> actedStatuses = List.of(LineStatus.APPROVED, LineStatus.REJECTED);

        // 1) 내가 직접 처리한 건 (기존)
        List<Approval> myActed =
                approvalRepository.findActedByMemberPositionId(memberPositionId, actedStatuses);

        // 2) 내가 대결자로서 처리한 건
        List<Approval> proxyActed =
                approvalRepository.findActedByActualApproverMemberId(memberId, actedStatuses);

        // 3) 합산 (중복 제거 — requestId 기준, actedAt 최신 순 유지)
        Map<UUID, Approval> mergedMap = new LinkedHashMap<>();
        for (Approval a : myActed) {
            mergedMap.putIfAbsent(a.getRequest().getRequestId(), a);
        }
        for (Approval a : proxyActed) {
            mergedMap.putIfAbsent(a.getRequest().getRequestId(), a);
        }

        return mergedMap.values().stream()
                .map(approval -> toResDtoWithRecipients(approval.getRequest()))
                .toList();
    }

//    승인 처리
    public ApprovalRequestResDto approve(UUID companyId, UUID memberId, UUID memberPositionId, UUID approvalId, ApprovalActionReqDto reqDto){
        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 라인을 찾을 수 없습니다."));

        if (approval.getApprovalStatus() != LineStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "결재 대기(PENDING) 상태에서만 승인할 수 있습니다.");
        }

        // 권한 확인: 본인 결재 또는 대결
        UUID actualMemberId;
        UUID actualMemberPositionId;

        if (approval.getApproverMemberPositionId().equals(memberPositionId)) {
            // 본인 결재
            actualMemberId = memberId;
            actualMemberPositionId = memberPositionId;
        } else {
            // 대결자인지 확인
            AbsenceProxy proxy = absenceProxyRepository
                    .findActiveProxy(companyId, approval.getApproverMemberId(), LocalDateTime.now())
                    .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN,
                            "본인의 결재건만 처리할 수 있습니다."));

            if (!proxy.getSubstituteId().equals(memberId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "본인의 결재건만 처리할 수 있습니다.");
            }

            actualMemberId = memberId;
            actualMemberPositionId = memberPositionId;
        }



//        현재 단계 승인 처리
        approval.approve(reqDto.getComment(), actualMemberId, actualMemberPositionId);

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

            //        다음 결재자에게 알림
            approvalNotificationService.notifyNextApprover(request, nextApproval, actualMemberId);

        } else {
            request.updateStatus(RequestStatus.APPROVED);

            // 공문이면 최종 승인 시점에 번호 발번
            if (request.getRequestType() == RequestType.OFFICIAL) {
                String docNumber = officialNumberService.generate(request.getRequestId());
                request.updateDocumentNumber(docNumber);
                approvalNotificationService.publishOfficialApprovedEvent(request);
            }
        }

        List<Approval> allApprovals =
                approvalRepository.findByRequestIdWithRequest(requestId);
        List<ApprovalViewer> viewers =
                approvalViewerRepository.findByRequestId(requestId);

        //        최종 승인 완료 시 요청자/참조자/공람자에게 알림
        if (request.getRequestStatus() == RequestStatus.APPROVED) {
            approvalNotificationService.notifyApproved(request, viewers, actualMemberId);
        }

        List<OfficialRecipientResDto> recipientDtos = null;
        if (request.getRequestType() == RequestType.OFFICIAL) {
            recipientDtos = officialRecipientRepository
                    .findByApprovalRequest_RequestId(requestId)
                    .stream()
                    .map(OfficialRecipientResDto::fromEntity)
                    .toList();
        }
        return ApprovalRequestResDto.fromEntity(request, allApprovals, viewers, recipientDtos);

    }


    public ApprovalRequestResDto reject(UUID companyId, UUID memberId, UUID memberPositionId, UUID approvalId,
                                        ApprovalActionReqDto reqDto) {

        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 라인을 찾을 수 없습니다."));

        if (approval.getApprovalStatus() != LineStatus.PENDING) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "결재 대기(PENDING) 상태에서만 반려할 수 있습니다.");
        }

        if (reqDto.getComment() == null || reqDto.getComment().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "반려 사유는 필수입니다.");
        }

        // 권한 확인: 본인 결재 또는 대결
        UUID actualMemberId;
        UUID actualMemberPositionId;

        if (approval.getApproverMemberPositionId().equals(memberPositionId)) {
            actualMemberId = memberId;
            actualMemberPositionId = memberPositionId;
        } else {
            AbsenceProxy proxy = absenceProxyRepository
                    .findActiveProxy(companyId, approval.getApproverMemberId(), LocalDateTime.now())
                    .orElseThrow(() -> new BusinessException(HttpStatus.FORBIDDEN,
                            "본인의 결재건만 처리할 수 있습니다."));

            if (!proxy.getSubstituteId().equals(memberId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "본인의 결재건만 처리할 수 있습니다.");
            }

            actualMemberId = memberId;
            actualMemberPositionId = memberPositionId;
        }

        // 현재 결재라인 반려
        approval.reject(reqDto.getComment(), actualMemberId, actualMemberPositionId);

        // 나머지 WAITING 라인 → REJECTED 처리
        ApprovalRequest request = approval.getRequest();
        UUID requestId = request.getRequestId();

        List<Approval> allApprovals = approvalRepository.findByRequestIdWithRequest(requestId);
        for (Approval a : allApprovals) {
            if (a.getApprovalStatus() == LineStatus.WAITING) {
                a.reject(null, null, null);
            }
        }

        // 결재요청 상태 → REJECTED
        request.updateStatus(RequestStatus.REJECTED);

        List<ApprovalViewer> viewers = approvalViewerRepository.findByRequestId(requestId);

        //        요청자/참조자에게 반려 알림
        approvalNotificationService.notifyRejected(
                request, viewers, actualMemberId, reqDto.getComment());

        List<OfficialRecipientResDto> recipientDtos = null;
        if (request.getRequestType() == RequestType.OFFICIAL){
            recipientDtos = officialRecipientRepository
                    .findByApprovalRequest_RequestId(requestId)
                    .stream()
                    .map(OfficialRecipientResDto::fromEntity)
                    .toList();
        }
        return ApprovalRequestResDto.fromEntity(request, allApprovals, viewers, recipientDtos);
    }

    // 결재 대기함 (내 건 + 대결 건)
    @Transactional(readOnly = true)
    public List<ApprovalRequestResDto> findPending(UUID companyId, UUID memberId, UUID memberPositionId) {

        // 1) 내가 직접 결재할 건
        List<Approval> myPending =
                approvalRepository.findPendingByMemberPositionId(memberPositionId, LineStatus.PENDING);

        // 2) 내가 대결자로 지정된 부재자들의 PENDING 건
        List<AbsenceProxy> activeProxies =
                absenceProxyRepository.findBySubstituteIdAndIsActiveYn(memberId, "Y");

        List<Approval> proxyPending = activeProxies.stream()
                .filter(proxy -> {
                    // 현재 시점에 유효한 위임만
                    LocalDateTime now = LocalDateTime.now();
                    return !now.isBefore(proxy.getStartDate()) && !now.isAfter(proxy.getEndDate())
                            && proxy.getCompanyId().equals(companyId);
                })
                .flatMap(proxy ->
                        approvalRepository.findPendingByApproverMemberId(
                                proxy.getMemberId(), LineStatus.PENDING).stream()
                )
                .toList();

        // 3) 합산 (중복 제거 — requestId 기준)
        Map<UUID, Approval> mergedMap = new LinkedHashMap<>();
        for (Approval a : myPending) {
            mergedMap.putIfAbsent(a.getRequest().getRequestId(), a);
        }
        for (Approval a : proxyPending) {
            mergedMap.putIfAbsent(a.getRequest().getRequestId(), a);
        }

        return mergedMap.values().stream()
                .map(approval -> toResDtoWithRecipients(approval.getRequest()))
                .toList();
    }

    private ApprovalRequestResDto toResDtoWithRecipients(ApprovalRequest request) {
        UUID requestId = request.getRequestId();
        List<Approval> allApprovals =
                approvalRepository.findByRequestIdWithRequest(requestId);
        List<ApprovalViewer> viewers =
                approvalViewerRepository.findByRequestId(requestId);

        List<OfficialRecipientResDto> recipientDtos = null;
        if (request.getRequestType() == RequestType.OFFICIAL) {
            recipientDtos = officialRecipientRepository
                    .findByApprovalRequest_RequestId(requestId)
                    .stream()
                    .map(OfficialRecipientResDto::fromEntity)
                    .toList();
        }
        return ApprovalRequestResDto.fromEntity(request, allApprovals, viewers, recipientDtos);
    }

}
