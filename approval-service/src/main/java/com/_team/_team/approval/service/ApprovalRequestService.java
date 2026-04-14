package com._team._team.approval.service;

import com._team._team.approval.domain.Approval;
import com._team._team.approval.domain.ApprovalDocument;
import com._team._team.approval.domain.ApprovalRequest;
import com._team._team.approval.domain.ApprovalViewer;
import com._team._team.approval.domain.enums.LineStatus;
import com._team._team.approval.domain.enums.RequestStatus;
import com._team._team.approval.domain.enums.ViewerReadStatus;
import com._team._team.approval.dto.reqdto.ApprovalRequestCreateReqDto;
import com._team._team.approval.dto.resdto.ApprovalRequestResDto;
import com._team._team.approval.feignclients.MemberServiceClient;
import com._team._team.approval.feignclients.dto.MemberPositionResDto;
import com._team._team.approval.repository.ApprovalDocumentRepository;
import com._team._team.approval.repository.ApprovalRepository;
import com._team._team.approval.repository.ApprovalRequestRepository;
import com._team._team.approval.repository.ApprovalViewerRepository;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ApprovalRequestService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApprovalRepository approvalRepository;
    private final ApprovalViewerRepository approvalViewerRepository;
    private final AttachmentService attachmentService;
    private final MemberServiceClient memberServiceClient;

    @Autowired
    public ApprovalRequestService(ApprovalRequestRepository approvalRequestRepository, ApprovalDocumentRepository approvalDocumentRepository, ApprovalRepository approvalRepository, ApprovalViewerRepository approvalViewerRepository, AttachmentService attachmentService, MemberServiceClient memberServiceClient) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.approvalDocumentRepository = approvalDocumentRepository;
        this.approvalRepository = approvalRepository;
        this.approvalViewerRepository = approvalViewerRepository;
        this.attachmentService = attachmentService;
        this.memberServiceClient = memberServiceClient;
    }

//    결재요청 생성(결재라인 + 참조/공람자 포함)
    public ApprovalRequestResDto create(UUID companyId, UUID memberId, UUID memberPositionId, ApprovalRequestCreateReqDto reqDto){

//        양식 조회 + 권한 체크
        ApprovalDocument document = approvalDocumentRepository.findById(reqDto.getDocumentId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

//        요청 상태 검증 (DRAFT 또는 WAIT만 허용)
        if (reqDto.getRequestStatus() != RequestStatus.DRAFT && reqDto.getRequestStatus() != RequestStatus.WAIT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "요청 상태는 DRAFT 또는 WAIT만 가능합니다.");
        }

        // WAIT(제출)일 때 결재자 최소 1명 검증 (자동승인 제외)
        if (reqDto.getRequestStatus() == RequestStatus.WAIT && "N".equals(document.getAutoApproveYn())) {
            if (reqDto.getApprovalLines() == null || reqDto.getApprovalLines().isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "결재 제출 시 결재자는 최소 1명 이상이어야 합니다.");
            }
        }

        // 결재자 중복 검증
        if (reqDto.getApprovalLines() != null && !reqDto.getApprovalLines().isEmpty()) {
            List<UUID> memberPositionIds = reqDto.getApprovalLines().stream()
                    .map(ApprovalRequestCreateReqDto.ApprovalLineItem::getApproverMemberPositionId)
                    .toList();

            if (memberPositionIds.size() != memberPositionIds.stream().distinct().count()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "동일한 결재자를 중복 지정할 수 없습니다.");
            }
        }

        // 참조/공람자 중복 검증
        if (reqDto.getViewers() != null && !reqDto.getViewers().isEmpty()) {
            long distinctCount = reqDto.getViewers().stream()
                    .map(v -> v.getViewerMemberId().toString() + "_" + v.getViewerType().name())
                    .distinct()
                    .count();

            if (distinctCount != reqDto.getViewers().size()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "동일한 참조/공람자를 중복 지정할 수 없습니다.");
            }
        }

        // stepOrder 연속성 검증
        if (reqDto.getApprovalLines() != null && !reqDto.getApprovalLines().isEmpty()) {
            List<Integer> orders = reqDto.getApprovalLines().stream()
                    .map(ApprovalRequestCreateReqDto.ApprovalLineItem::getStepOrder)
                    .sorted()
                    .toList();

            for (int i = 0; i < orders.size(); i++) {
                if (orders.get(i) != i + 1) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST,
                            "결재 순서(stepOrder)는 1부터 연속된 값이어야 합니다.");
                }
            }
        }

        // === 요청자 스냅샷 조회 ===
        // member-service에 요청자 정보 조회 (Feign 실패 시 트랜잭션 롤백)
        MemberPositionResDto requester = memberServiceClient.getMemberPosition(memberPositionId);
        if (requester == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "요청자 정보를 찾을 수 없습니다.");
        }

        // ApprovalRequest 생성
        ApprovalRequest request = ApprovalRequest.builder()
                .approvalDocument(document)
                .memberId(memberId)
                .requestType(document.getRequestType())
                .contentJson(reqDto.getContentJson())
                .requestStatus(reqDto.getRequestStatus())
                .requesterName(requester.getMemberName())
                .requesterOrganizationId(requester.getOrganizationId())
                .requesterOrganizationName(requester.getOrganizationName())
                .build();

        ApprovalRequest savedRequest = approvalRequestRepository.save(request);

//        Approval(결재라인) 생성
        List<Approval> savedApprovals = new ArrayList<>();
        if (reqDto.getApprovalLines() != null && !reqDto.getApprovalLines().isEmpty()){
            List<Approval> approvals = reqDto.getApprovalLines().stream()
                    .map(line -> Approval.builder()
                            .request(savedRequest)
                            .approverMemberId(line.getApproverMemberId())
                            .approverMemberPositionId(line.getApproverMemberPositionId())
                            .stepOrder(line.getStepOrder())
                            .approvalStatus(reqDto.getRequestStatus() == RequestStatus.WAIT && line.getStepOrder() == 1
                                    ? LineStatus.PENDING
                                    : LineStatus.WAITING)
                            .build())
                    .toList();

            savedApprovals = approvalRepository.saveAll(approvals);
        }

        // ApprovalViewer(참조/공람자) 생성
        List<ApprovalViewer> savedViewers = new ArrayList<>();
        if (reqDto.getViewers() != null && !reqDto.getViewers().isEmpty()) {
            List<ApprovalViewer> viewers = reqDto.getViewers().stream()
                    .map(viewer -> ApprovalViewer.builder()
                            .approvalRequest(savedRequest)
                            .viewerMemberId(viewer.getViewerMemberId())
                            .viewerMemberPositionId(viewer.getViewerMemberPositionId())
                            .viewerType(viewer.getViewerType())
                            .viewerReadStatus(ViewerReadStatus.UNREAD)
                            .build())
                    .toList();

            savedViewers = approvalViewerRepository.saveAll(viewers);
        }

        return ApprovalRequestResDto.fromEntity(savedRequest, savedApprovals, savedViewers);

    }

    // 결재요청 상세 조회 (결재라인 + 참조/공람자 포함)
    @Transactional(readOnly = true)
    public ApprovalRequestResDto findById(UUID companyId, UUID requestId) {
        // 결재요청 조회 (양식 정보 fetch join)
        ApprovalRequest request = approvalRequestRepository.findByIdWithDocument(requestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 요청을 찾을 수 없습니다."));

        if (!request.getApprovalDocument().getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        // 결재라인 + 참조/공람자 조회
        List<Approval> approvals = approvalRepository.findByRequestIdWithRequest(requestId);
        List<ApprovalViewer> viewers = approvalViewerRepository.findByRequestId(requestId);

        return ApprovalRequestResDto.fromEntity(request, approvals, viewers);
    }

    // 내가 올린 결재 목록
    @Transactional(readOnly = true)
    public List<ApprovalRequestResDto> findByMemberId(UUID memberId) {
        List<ApprovalRequest> requests =
                approvalRequestRepository.findByMemberIdAndDelYn(memberId, "N");

        return requests.stream()
                .map(request -> {
                    List<Approval> approvals =
                            approvalRepository.findByRequestIdWithRequest(request.getRequestId());
                    List<ApprovalViewer> viewers =
                            approvalViewerRepository.findByRequestId(request.getRequestId());
                    return ApprovalRequestResDto.fromEntity(request, approvals, viewers);
                })
                .toList();
    }

    // 내가 올린 결재 목록 (상태별 필터)
    @Transactional(readOnly = true)
    public List<ApprovalRequestResDto> findByMemberIdAndStatus(UUID memberId,
                                                               RequestStatus status) {
        List<ApprovalRequest> requests =
                approvalRequestRepository.findByMemberIdAndRequestStatusAndDelYn(
                        memberId, status, "N");

        return requests.stream()
                .map(request -> {
                    List<Approval> approvals =
                            approvalRepository.findByRequestIdWithRequest(request.getRequestId());
                    List<ApprovalViewer> viewers =
                            approvalViewerRepository.findByRequestId(request.getRequestId());
                    return ApprovalRequestResDto.fromEntity(request, approvals, viewers);
                })
                .toList();
    }

//    임시저장 수정 (DRAFT 상태인 경우)
    public ApprovalRequestResDto update(UUID companyId, UUID memberId, UUID requestId, ApprovalRequestCreateReqDto reqDto){
//        결재요청 조회 및 검증
        ApprovalRequest request = approvalRequestRepository.findByIdWithDocument(requestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 요청을 찾을 수 없습니다."));

        ApprovalDocument document = request.getApprovalDocument();

        if (!request.getApprovalDocument().getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        if (!request.getMemberId().equals(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "본인의 결재 요청만 수정할 수 있습니다.");
        }

        if (request.getRequestStatus() != RequestStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "임시저장(DRAFT) 상태에서만 수정 가능합니다.");
        }

        // 상태 검증 (DRAFT 또는 WAIT만 허용)
        if (reqDto.getRequestStatus() != RequestStatus.DRAFT
                && reqDto.getRequestStatus() != RequestStatus.WAIT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "요청 상태는 DRAFT 또는 WAIT만 가능합니다.");
        }

        // WAIT(제출)일 때 결재자 최소 1명 검증
        if (reqDto.getRequestStatus() == RequestStatus.WAIT && !"Y".equals(document.getAutoApproveYn())) {
            if (reqDto.getApprovalLines() == null || reqDto.getApprovalLines().isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "결재 제출 시 결재자는 최소 1명 이상이어야 합니다.");
            }
        }

        // 결재자 중복 검증
        if (reqDto.getApprovalLines() != null && !reqDto.getApprovalLines().isEmpty()) {
            List<UUID> memberPositionIds = reqDto.getApprovalLines().stream()
                    .map(ApprovalRequestCreateReqDto.ApprovalLineItem::getApproverMemberPositionId)
                    .toList();

            if (memberPositionIds.size() != memberPositionIds.stream().distinct().count()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "동일한 결재자를 중복 지정할 수 없습니다.");
            }
        }

        // 참조/공람자 중복 검증
        if (reqDto.getViewers() != null && !reqDto.getViewers().isEmpty()) {
            long distinctCount = reqDto.getViewers().stream()
                    .map(v -> v.getViewerMemberId().toString() + "_" + v.getViewerType().name())
                    .distinct()
                    .count();

            if (distinctCount != reqDto.getViewers().size()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "동일한 참조/공람자를 중복 지정할 수 없습니다.");
            }
        }

        // stepOrder 연속성 검증
        if (reqDto.getApprovalLines() != null && !reqDto.getApprovalLines().isEmpty()) {
            List<Integer> orders = reqDto.getApprovalLines().stream()
                    .map(ApprovalRequestCreateReqDto.ApprovalLineItem::getStepOrder)
                    .sorted()
                    .toList();

            for (int i = 0; i < orders.size(); i++) {
                if (orders.get(i) != i + 1) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST,
                            "결재 순서(stepOrder)는 1부터 연속된 값이어야 합니다.");
                }
            }
        }

        // 결재요청 내용 수정
        request.updateContentJson(reqDto.getContentJson());
        request.updateStatus(reqDto.getRequestStatus());

        // 기존 결재라인 물리 삭제 후 새로 저장
        List<Approval> existingApprovals =
                approvalRepository.findByRequestIdWithRequest(requestId);
        approvalRepository.deleteAll(existingApprovals);

        List<Approval> savedApprovals = new ArrayList<>();
        if (reqDto.getApprovalLines() != null && !reqDto.getApprovalLines().isEmpty()) {
            List<Approval> newApprovals = reqDto.getApprovalLines().stream()
                    .map(line -> Approval.builder()
                            .request(request)
                            .approverMemberId(line.getApproverMemberId())
                            .approverMemberPositionId(line.getApproverMemberPositionId())
                            .stepOrder(line.getStepOrder())
                            .approvalStatus(reqDto.getRequestStatus() == RequestStatus.WAIT && line.getStepOrder() == 1
                                    ? LineStatus.PENDING
                                    : LineStatus.WAITING)
                            .build())
                    .toList();

            savedApprovals = approvalRepository.saveAll(newApprovals);
        }

        // 기존 참조/공람자 물리 삭제 후 새로 저장
        List<ApprovalViewer> existingViewers = approvalViewerRepository.findByRequestId(requestId);
        approvalViewerRepository.deleteAll(existingViewers);

        // 기존 첨부파일 전체 삭제 (S3 + DB)
        attachmentService.deleteAllByRequestId(requestId);

        List<ApprovalViewer> savedViewers = new ArrayList<>();
        if (reqDto.getViewers() != null && !reqDto.getViewers().isEmpty()) {
            List<ApprovalViewer> newViewers = reqDto.getViewers().stream()
                    .map(viewer -> ApprovalViewer.builder()
                            .approvalRequest(request)
                            .viewerMemberId(viewer.getViewerMemberId())
                            .viewerMemberPositionId(viewer.getViewerMemberPositionId())
                            .viewerType(viewer.getViewerType())
                            .viewerReadStatus(ViewerReadStatus.UNREAD)
                            .build())
                    .toList();

            savedViewers = approvalViewerRepository.saveAll(newViewers);
        }

        return ApprovalRequestResDto.fromEntity(request, savedApprovals, savedViewers);

    }

//    결재 취소
    public ApprovalRequestResDto cancel(UUID companyId, UUID memberId, UUID requestId, String cancelReason){

//        결재요청 조회 + 검증
        ApprovalRequest request = approvalRequestRepository.findByIdWithDocument(requestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 요청을 찾을 수 없습니다."));

        if (!request.getApprovalDocument().getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        if (!request.getMemberId().equals(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "본인의 결재 요청만 취소할 수 있습니다.");
        }

        // DRAFT, WAIT만 취소 가능
        if (request.getRequestStatus() != RequestStatus.DRAFT
                && request.getRequestStatus() != RequestStatus.WAIT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "임시저장(DRAFT) 또는 결재대기(WAIT) 상태에서만 취소 가능합니다.");
        }

        // 취소사유 필수 검증
        if (cancelReason == null || cancelReason.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "취소 사유는 필수입니다.");
        }

        // 결재요청 취소 처리
        request.cancel(cancelReason);

        // 결재라인도 전부 CANCELED 처리
        List<Approval> approvals =
                approvalRepository.findByRequestIdWithRequest(requestId);
        approvals.forEach(Approval::cancel);

        List<ApprovalViewer> viewers =
                approvalViewerRepository.findByRequestId(requestId);

        return ApprovalRequestResDto.fromEntity(request, approvals, viewers);

    }

    // 부서 문서함 조회 (본인 부서 + 하위 조직의 결재 요청, 작성 시점 부서 기준)
    @Transactional(readOnly = true)
    public List<ApprovalRequestResDto> findDepartmentRequests(
            UUID companyId, UUID memberPositionId, UUID organizationId) {

        // 1. 요청자 본인 정보 조회
        MemberPositionResDto requester = memberServiceClient.getMemberPosition(memberPositionId);
        if (requester == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다.");
        }

        // 2. 본인 접근 가능 범위 (본인 부서 + 하위 조직 전체)
        List<UUID> accessibleOrgIds = memberServiceClient
                .getOrganizationDescendantIds(requester.getOrganizationId());

        // 3. 요청된 organizationId가 본인 접근 범위 안에 있는지 검증
        if (!accessibleOrgIds.contains(organizationId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "해당 부서의 문서함에 접근할 권한이 없습니다.");
        }

        // 4. 조회 대상 부서의 descendants (자기 자신 포함)
        List<UUID> targetOrgIds = memberServiceClient
                .getOrganizationDescendantIds(organizationId);

        // 5. 부서 문서함 조회 (승인완료/반려만)
        List<ApprovalRequest> requests = approvalRequestRepository
                .findDepartmentRequests(
                        companyId,
                        targetOrgIds,
                        List.of(RequestStatus.APPROVED, RequestStatus.REJECTED));

        return requests.stream()
                .map(request -> {
                    List<Approval> approvals =
                            approvalRepository.findByRequestIdWithRequest(request.getRequestId());
                    List<ApprovalViewer> viewers =
                            approvalViewerRepository.findByRequestId(request.getRequestId());
                    return ApprovalRequestResDto.fromEntity(request, approvals, viewers);
                })
                .toList();
    }
}
