package com._team._team.service;

import com._team._team.domain.*;
import com._team._team.domain.enums.*;
import com._team._team.dtos.reqdto.ApprovalLineItemDto;
import com._team._team.dtos.reqdto.ApprovalRequestCreateDto;
import com._team._team.dtos.reqdto.ViewerItemDto;
import com._team._team.dtos.resdto.ApprovalLineDetailDto;
import com._team._team.dtos.resdto.ApprovalRequestResponseDto;
import com._team._team.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class RequestService {
    private final RequestRepository requestRepository;
    private final ApprovalRepository approvalRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final ApprovalViewerRepository approvalViewerRepository;
    private final ApprovalDocumentRepository documentRepository;
    private final ApprovalPolicyLineRepository policyLineRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public RequestService(RequestRepository requestRepository, ApprovalRepository approvalRepository, ApprovalLineRepository approvalLineRepository, ApprovalViewerRepository approvalViewerRepository, ApprovalDocumentRepository documentRepository, ApprovalPolicyLineRepository policyLineRepository, ObjectMapper objectMapper) {
        this.requestRepository = requestRepository;
        this.approvalRepository = approvalRepository;
        this.approvalLineRepository = approvalLineRepository;
        this.approvalViewerRepository = approvalViewerRepository;
        this.documentRepository = documentRepository;
        this.policyLineRepository = policyLineRepository;
        this.objectMapper = objectMapper;
    }

    public UUID createApprovalRequest(UUID memberId, ApprovalRequestCreateDto dto){

//        본인 결재자로 지정 방지 코드 추가(memberPositionId)

        // 1. 문서 양식 조회 및 검증
        ApprovalDocument document = documentRepository.findById(dto.getDocumentId()).orElseThrow(() -> new EntityNotFoundException("문서양식이 존재하지 않습니다."));

        if ("N".equals(document.getIsActiveYn())) {
            throw new IllegalStateException("비활성화된 문서 양식입니다.");
        }

        // 2. contentJson 직렬화
        String contentJson;
        try {
            contentJson = objectMapper.writeValueAsString(dto.getContentJson());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("contentJson 변환 실패", e);
        }

        // 3. Request 생성
        RequestStatus initialStatus =
                dto.isDraft() ? RequestStatus.DRAFT : RequestStatus.PENDING;

        Request request = Request.builder()
                .approvalDocument(document)
                .memberId(memberId)
                .requestType(dto.getRequestType())
                .contentJson(contentJson)
                .status(RequestStatus.PENDING)
                .startDateTime(dto.getStartDateTime())
                .endDateTime(dto.getEndDateTime())
                .build();
        requestRepository.save(request);

        // 4. 임시저장이면 여기서 끝 (Approval, ApprovalLine 생성 안 함)
        if (dto.isDraft()) {
            return request.getRequestId();
        }

        // 5. 제출인 경우 — 결재선 필수 검증
        if (dto.getApprovalLines() == null || dto.getApprovalLines().isEmpty()) {
            throw new IllegalArgumentException("제출 시 결재선은 최소 1명 이상 필요합니다.");
        }

        // 6. 자동승인 여부 확인
        if ("Y".equals(document.getAutoApproveYn())) {
            request.updateStatus(RequestStatus.APPROVED);
            return request.getRequestId();
        }

        // 7. Approval 생성
        Approval approval = Approval.builder()
                .request(request)
                .status(ApprovalStatus.PENDING)
                .currentStep(1)
                .build();
        approvalRepository.save(approval);

        // 8. ApprovalLine 생성
        List<ApprovalLineItemDto> lineItems = dto.getApprovalLines();
        for (ApprovalLineItemDto item : lineItems) {
            ApprovalPolicyLine policyLine = policyLineRepository.findById(item.getPolicyLineId())
                    .orElseThrow(() -> new EntityNotFoundException("결재 정책 라인이 존재하지 않습니다."));

            // 1단계는 PENDING, 나머지는 WAIT
            LineStatus lineStatus = (item.getStepOrder() == 1)
                    ? LineStatus.PENDING
                    : LineStatus.WAIT;

            ApprovalLine approvalLine = ApprovalLine.builder()
                    .approval(approval)
                    .approvalPolicyLine(policyLine)
                    .memberPositionId(item.getMemberPositionId())
                    .stepOrder(item.getStepOrder())
                    .lineStatus(lineStatus)
                    .build();
            approvalLineRepository.save(approvalLine);
        }

        // 9. ApprovalViewer 생성 (참조/공람자)
        if (dto.getViewers() != null && !dto.getViewers().isEmpty()) {
            for (ViewerItemDto viewer : dto.getViewers()) {
                ViewerType viewerType;
                try {
                    viewerType = ViewerType.valueOf(viewer.getViewerType());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "유효하지 않은 참조 유형입니다: " + viewer.getViewerType() + " (CC 또는 CIRCULATION만 허용)");
                }
                ApprovalViewer approvalViewer = ApprovalViewer.builder()
                        .approval(approval)
                        .memberId(viewer.getMemberId())
                        .viewerType(viewerType)
                        .status(ViewerReadStatus.UNREAD)
                        .build();
                approvalViewerRepository.save(approvalViewer);
            }
        }

        return request.getRequestId();
    }

    public UUID submitDraftRequest(UUID memberId, UUID requestId, ApprovalRequestCreateDto dto) {

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("결재 요청이 존재하지 않습니다."));

        // 본인 확인
        if (!request.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인의 임시저장 건만 제출할 수 있습니다.");
        }

        // DRAFT 상태 확인
        if (request.getStatus() != RequestStatus.DRAFT) {
            throw new IllegalStateException("임시저장 상태의 요청만 제출할 수 있습니다.");
        }

        // 결재선 필수 검증
        if (dto.getApprovalLines() == null || dto.getApprovalLines().isEmpty()) {
            throw new IllegalArgumentException("제출 시 결재선은 최소 1명 이상 필요합니다.");
        }

        // 내용 업데이트
        String contentJson;
        try {
            contentJson = objectMapper.writeValueAsString(dto.getContentJson());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("contentJson 변환 실패", e);
        }
        request.updateContentJson(contentJson);
        request.updateStartDateTime(dto.getStartDateTime());
        request.updateEndDateTime(dto.getEndDateTime());

        // 자동승인 확인
        ApprovalDocument document = request.getApprovalDocument();
        if ("Y".equals(document.getAutoApproveYn())) {
            request.updateStatus(RequestStatus.APPROVED);
            return request.getRequestId();
        }

        // 상태를 PENDING으로 변경
        request.updateStatus(RequestStatus.PENDING);

        // Approval + ApprovalLine + ApprovalViewer 생성
        Approval approval = Approval.builder()
                .request(request)
                .status(ApprovalStatus.PENDING)
                .currentStep(1)
                .build();
        approvalRepository.save(approval);

        for (ApprovalLineItemDto item : dto.getApprovalLines()) {
            ApprovalPolicyLine policyLine = policyLineRepository.findById(item.getPolicyLineId())
                    .orElseThrow(() -> new EntityNotFoundException("결재 정책 라인이 존재하지 않습니다."));

            LineStatus lineStatus = (item.getStepOrder() == 1)
                    ? LineStatus.PENDING
                    : LineStatus.WAIT;

            ApprovalLine approvalLine = ApprovalLine.builder()
                    .approval(approval)
                    .approvalPolicyLine(policyLine)
                    .memberPositionId(item.getMemberPositionId())
                    .stepOrder(item.getStepOrder())
                    .lineStatus(lineStatus)
                    .build();
            approvalLineRepository.save(approvalLine);
        }

        if (dto.getViewers() != null && !dto.getViewers().isEmpty()) {
            for (ViewerItemDto viewer : dto.getViewers()) {
                ViewerType viewerType;
                try {
                    viewerType = ViewerType.valueOf(viewer.getViewerType());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "유효하지 않은 참조 유형입니다: " + viewer.getViewerType() + " (CC 또는 CIRCULATION만 허용)");
                }

                ApprovalViewer approvalViewer = ApprovalViewer.builder()
                        .approval(approval)
                        .memberId(viewer.getMemberId())
                        .viewerType(viewerType)
                        .status(ViewerReadStatus.UNREAD)
                        .build();
                approvalViewerRepository.save(approvalViewer);
            }
        }

        return request.getRequestId();
    }

    public void updateDraftRequest(UUID memberId, UUID requestId, ApprovalRequestCreateDto dto) {

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("결재 요청이 존재하지 않습니다."));

        if (!request.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인의 임시저장 건만 수정할 수 있습니다.");
        }

        if (request.getStatus() != RequestStatus.DRAFT) {
            throw new IllegalStateException("임시저장 상태의 요청만 수정할 수 있습니다.");
        }

        String contentJson;
        try {
            contentJson = objectMapper.writeValueAsString(dto.getContentJson());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("contentJson 변환 실패", e);
        }

        request.updateContentJson(contentJson);
        request.updateStartDateTime(dto.getStartDateTime());
        request.updateEndDateTime(dto.getEndDateTime());
        // 상태는 DRAFT 유지 — 변경하지 않음
    }


    @Transactional(readOnly = true)
    public List<Request> getMyRequests(UUID memberId) {
        return requestRepository.findByMemberIdAndDelYnAndStatusNotOrderByCreatedAtDesc(memberId, "N", RequestStatus.DRAFT);
    }

//    임시저장 목록 조회
    @Transactional(readOnly = true)
    public List<Request> getMyDraftRequests(UUID memberId) {
        return requestRepository.findByMemberIdAndDelYnAndStatusOrderByUpdatedAtDesc(
                memberId, "N", RequestStatus.DRAFT);
    }

//    결재 요청 단건 상세 조회
    @Transactional(readOnly = true)
    public ApprovalRequestResponseDto getRequestDetail(UUID requestId) {

        Request request = requestRepository.findByIdWithDocument(requestId)
                .orElseThrow(() -> new EntityNotFoundException("결재 요청이 존재하지 않습니다."));

        ApprovalRequestResponseDto.ApprovalRequestResponseDtoBuilder builder =
                ApprovalRequestResponseDto.builder()
                        .requestId(request.getRequestId())
                        .documentId(request.getApprovalDocument().getDocumentId())
                        .documentName(request.getApprovalDocument().getDocumentName())
                        .requestType(request.getRequestType())
                        .requestStatus(request.getStatus())
                        .requesterId(request.getMemberId())
                        .startDateTime(request.getStartDateTime())
                        .endDateTime(request.getEndDateTime())
                        .createdAt(request.getCreatedAt());

        // DRAFT 상태면 Approval이 없으므로 Request 정보만 반환
        Approval approval = approvalRepository.findByRequest_RequestId(requestId)
                .orElse(null);

        if (approval != null) {
            builder.approvalId(approval.getApprovalId())
                    .approvalStatus(approval.getStatus())
                    .currentStep(approval.getCurrentStep());

            List<ApprovalLine> lines =
                    approvalLineRepository.findByApproval_ApprovalIdOrderByStepOrderAsc(approval.getApprovalId());

            List<ApprovalLineDetailDto> lineDetails = lines.stream()
                    .map(line -> ApprovalLineDetailDto.builder()
                            .approvalLineId(line.getApprovalLineId())
                            .stepOrder(line.getStepOrder())
                            .memberPositionId(line.getMemberPositionId())
                            .lineStatus(line.getLineStatus().name())
                            .comment(line.getComment())
                            .isSignedYn(line.getIsSignedYn())
                            .signedAt(line.getSignedAt())
                            .build())
                    .collect(Collectors.toList());

            builder.approvalLines(lineDetails);
        }

        return builder.build();
    }

//    1단계 결재자가 처리하지 않은 경우 결재 요청 취소
    public void cancelRequest(UUID memberId, UUID requestId, String cancelReason) {

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("결재 요청이 존재하지 않습니다."));

        if (!request.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인의 결재 요청만 회수할 수 있습니다.");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("대기 중인 결재만 회수할 수 있습니다.");
        }

        // 1단계 결재자가 이미 처리했는지 확인
        Approval approval = approvalRepository.findByRequest_RequestId(requestId)
                .orElseThrow(() -> new EntityNotFoundException("결재 정보가 존재하지 않습니다."));

        List<ApprovalLine> firstStepLines =
                approvalLineRepository.findByApproval_ApprovalIdAndStepOrder(
                        approval.getApprovalId(), 1);

        boolean alreadyProcessed = firstStepLines.stream()
                .anyMatch(line -> line.getLineStatus() == LineStatus.APPROVED
                        || line.getLineStatus() == LineStatus.REJECTED);

        if (alreadyProcessed) {
            throw new IllegalStateException("이미 결재가 진행된 건은 회수할 수 없습니다.");
        }

        // 회수 처리
        request.cancel(cancelReason);
        approval.reject();

        // 모든 결재선 상태 초기화
        List<ApprovalLine> allLines =
                approvalLineRepository.findByApproval_ApprovalIdOrderByStepOrderAsc(
                        approval.getApprovalId());
        for (ApprovalLine line : allLines) {
            line.reject("요청자 회수", null);
        }
    }

//    임시 저장 건 삭제
    public void deleteDraftRequest(UUID memberId, UUID requestId) {

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("결재 요청이 존재하지 않습니다."));

        if (!request.getMemberId().equals(memberId)) {
            throw new IllegalStateException("본인의 임시저장 건만 삭제할 수 있습니다.");
        }

        if (request.getStatus() != RequestStatus.DRAFT) {
            throw new IllegalStateException("임시저장 상태의 요청만 삭제할 수 있습니다.");
        }

        requestRepository.delete(request);
    }
}
