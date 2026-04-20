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
import com._team._team.approval.feignclients.MemberServiceClient;
import com._team._team.approval.feignclients.dto.SignatureResDto;
import com._team._team.approval.repository.AbsenceProxyRepository;
import com._team._team.approval.repository.ApprovalRepository;
import com._team._team.approval.repository.ApprovalRequestRepository;
import com._team._team.approval.repository.ApprovalViewerRepository;
import com._team._team.approval.repository.OfficialRecipientRepository;
import com._team._team.dto.BusinessException;
import com._team._team.event.LeaveApprovalEvent;
import com._team._team.event.OvertimeApprovalEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final ApprovalViewerRepository approvalViewerRepository;
    private final AbsenceProxyRepository absenceProxyRepository;
    private final ApprovalNotificationService approvalNotificationService;
    private final OvertimeApprovalEventPublisher overtimeApprovalEventPublisher;
    private final LeaveApprovalEventPublisher leaveApprovalEventPublisher;
    private final ObjectMapper objectMapper;
    private final OfficialNumberService officialNumberService;
    private final OfficialRecipientRepository officialRecipientRepository;
    private final MemberServiceClient memberServiceClient;

    @Autowired
    public ApprovalService(ApprovalRepository approvalRepository,
                           ApprovalViewerRepository approvalViewerRepository,
                           AbsenceProxyRepository absenceProxyRepository,
                           ApprovalNotificationService approvalNotificationService,
                           OvertimeApprovalEventPublisher overtimeApprovalEventPublisher,
                           LeaveApprovalEventPublisher leaveApprovalEventPublisher,
                           OfficialNumberService officialNumberService,
                           OfficialRecipientRepository officialRecipientRepository,
                           ObjectMapper objectMapper, MemberServiceClient memberServiceClient) {
        this.approvalRepository = approvalRepository;
        this.approvalViewerRepository = approvalViewerRepository;
        this.absenceProxyRepository = absenceProxyRepository;
        this.approvalNotificationService = approvalNotificationService;
        this.officialNumberService = officialNumberService;
        this.officialRecipientRepository = officialRecipientRepository;
        this.overtimeApprovalEventPublisher = overtimeApprovalEventPublisher;
        this.leaveApprovalEventPublisher = leaveApprovalEventPublisher;
        this.objectMapper = objectMapper;
        this.memberServiceClient = memberServiceClient;
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

        // 서명 이미지 복사
        try {
            SignatureResDto signatureRes = memberServiceClient.getSignatureUrl(actualMemberId);
            if (signatureRes != null && signatureRes.getSignatureUrl() != null) {
                approval.sign(signatureRes.getSignatureUrl());
            }
        } catch (Exception e) {
            // 서명 조회 실패해도 승인은 진행
        }

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

            // 공문
            if (request.getRequestType() == RequestType.OFFICIAL) {
                approvalNotificationService.publishOfficialApprovedEvent(request);
            }

            // 최종 승인 시 이벤트 발행
            publishOvertimeEventIfApplicable(request, OvertimeApprovalEvent.Action.APPROVE);
            publishLeaveEventIfApplicable(request, LeaveApprovalEvent.Action.USE);
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

        // 반려 시 이벤트 발행
        publishOvertimeEventIfApplicable(request, OvertimeApprovalEvent.Action.REJECT);
        // 연차는 승인 전에는 차감되지 않았으므로 RESTORE 필요 없음 → 반려 시 발행 생략

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

    // 결재 예정함 (아직 내 차례가 아닌 문서)
    @Transactional(readOnly = true)
    public List<ApprovalRequestResDto> findWaiting(UUID memberPositionId) {

        List<Approval> waitingApprovals =
                approvalRepository.findWaitingByMemberPositionId(memberPositionId, LineStatus.WAITING);

        return waitingApprovals.stream()
                .map(approval -> toResDtoWithRecipients(approval.getRequest()))
                .toList();
    }

    // 결재함 전체 (PENDING + WAITING 통합)
    @Transactional(readOnly = true)
    public List<ApprovalRequestResDto> findAllInbox(UUID companyId, UUID memberId, UUID memberPositionId) {

        // 1) PENDING (내 차례 + 대결)
        List<ApprovalRequestResDto> pendingList = findPending(companyId, memberId, memberPositionId);

        // 2) WAITING (예정)
        List<ApprovalRequestResDto> waitingList = findWaiting(memberPositionId);

        // 3) 합산 (중복 제거 — requestId 기준, PENDING 우선)
        Map<UUID, ApprovalRequestResDto> mergedMap = new LinkedHashMap<>();
        for (ApprovalRequestResDto dto : pendingList) {
            mergedMap.putIfAbsent(dto.getRequestId(), dto);
        }
        for (ApprovalRequestResDto dto : waitingList) {
            mergedMap.putIfAbsent(dto.getRequestId(), dto);
        }

        return new ArrayList<>(mergedMap.values());
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


    // 아래는 승인/반려 직후 Kafka로 이벤트 쏘는 헬퍼들

    // 연장근무신청 문서일 때만 overtime 이벤트 발행
    // contentJson에서 workType(연장/야간/휴일) 뽑아서 같이 실어 보낸다.
    private void publishOvertimeEventIfApplicable(ApprovalRequest request, OvertimeApprovalEvent.Action action) {
        String docName = request.getApprovalDocument().getDocumentName();
        if (!"연장근무신청".equals(docName)) {
            return;
        }

        String workType = extractWorkType(request.getContentJson());

        overtimeApprovalEventPublisher.publish(
                OvertimeApprovalEvent.builder()
                        .companyId(request.getApprovalDocument().getCompanyId())
                        .memberId(request.getMemberId())
                        .requestId(request.getRequestId())
                        .workType(workType)
                        .startAt(request.getStartDateTime())
                        .endAt(request.getEndDateTime())
                        .action(action)
                        .build()
        );
    }

    // contentJson에서 workType 한 필드만 뽑기
    // JSON 파싱 실패하거나 필드 없으면 그냥 null 리턴 (이벤트는 나가되 workType만 비게 됨).
    private String extractWorkType(String contentJson) {
        if (contentJson == null) return null;
        try {

            JsonNode node = objectMapper.readTree(contentJson);
            JsonNode wt = node.get("workType");
            return wt != null ? wt.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // 연차신청서 문서일 때만 leave 이벤트 발행
    // 연차=1일씩 누적, 반차=0.5일. BalanceType은 일단 ANNUAL 고정 (CARRYOVER 우선소진은 salary 쪽에서 처리)
    private void publishLeaveEventIfApplicable(ApprovalRequest request, LeaveApprovalEvent.Action action) {
        String docName = request.getApprovalDocument().getDocumentName();
        if (!"연차신청서".equals(docName)) {
            return;
        }

        JsonNode node = readContent(request.getContentJson());
        if (node == null) {
            return;
        }

        String vacationType = textOrNull(node, "vacationType");
        LocalDate startDate = dateOrNull(node, "startDate");
        LocalDate endDate   = dateOrNull(node, "endDate");
        if (vacationType == null || startDate == null) {
            return;
        }

        double days = calculateLeaveDays(vacationType, startDate, endDate);
        if (days <= 0) return;

        leaveApprovalEventPublisher.publish(
                LeaveApprovalEvent.builder()
                        .companyId(request.getApprovalDocument().getCompanyId())
                        .memberId(request.getMemberId())
                        .requestId(request.getRequestId())
                        .balanceType("ANNUAL")
                        .days(days)
                        .leaveDate(startDate)
                        .action(action)
                        .build()
        );
    }

    // 휴가 일수 계산 - 반차는 무조건 0.5, 연차는 시작/종료일 포함 일수
    // endDate 비어있으면 당일치기로 보고 startDate만으로 1일 처리
    private double calculateLeaveDays(String vacationType, LocalDate startDate, LocalDate endDate) {
        if (vacationType.startsWith("반차")) return 0.5;
        LocalDate end = (endDate != null) ? endDate : startDate;
        return end.toEpochDay() - startDate.toEpochDay() + 1.0;
    }

    // contentJson 통째로 파싱 - 실패하면 null 돌려서 호출한 쪽이 발행 스킵.
    private JsonNode readContent(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    // 해당 필드가 없거나 명시적 null이면 null 반환하는 얇은 래퍼
    private String textOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText() : null;
    }

    // ISO yyyy-MM-dd 문자열을 LocalDate로 파싱 - 빈 문자열/포맷 이상이면 null
    private LocalDate dateOrNull(JsonNode node, String field) {
        String text = textOrNull(node, field);
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text);
        } catch (Exception e) {
            return null;
        }
    }
}
