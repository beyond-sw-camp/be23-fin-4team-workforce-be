package com._team._team.attendance.service;

import com._team._team.attendance.domain.OvertimePolicy;
import com._team._team.attendance.domain.OvertimeRequest;
import com._team._team.attendance.domain.enums.OvertimeApprovalStatus;
import com._team._team.attendance.domain.enums.OvertimeRequestType;
import com._team._team.attendance.dto.vo.LaborLawViolation;
import com._team._team.attendance.dto.reqDto.OvertimeRequestCreateReqDto;
import com._team._team.attendance.dto.resDto.OvertimeRequestResDto;
import com._team._team.attendance.repository.OvertimePolicyRepository;
import com._team._team.attendance.repository.OvertimeRequestRepository;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * 연장근로 신청 서비스
 * 신청 제출과 본인 철회, 결재 결과 반영, 72시간 만료 배치를 담당
 */
@Service
@Transactional
public class OvertimeRequestService {

    private final OvertimeRequestRepository overtimeRequestRepository;
    private final LaborLawValidator laborLawValidator;
    private final OvertimePolicyRepository overtimePolicyRepository;

    @Autowired
    public OvertimeRequestService(OvertimeRequestRepository overtimeRequestRepository,
                                  LaborLawValidator laborLawValidator, OvertimePolicyRepository overtimePolicyRepository) {
        this.overtimeRequestRepository = overtimeRequestRepository;
        this.laborLawValidator = laborLawValidator;
        this.overtimePolicyRepository = overtimePolicyRepository;
    }

    /**
     * 연장근로 신청 제출 , PRE(사전) 또는 POST(사후)
     * 법정 한도 사전 검증 후 PENDING 상태로 INSERT
     */
    public OvertimeRequestResDto submit(UUID companyId,
                                        UUID memberId,
                                        OvertimeRequestCreateReqDto reqDto) {
        // 타입별 필수 필드 검증
        validateByType(reqDto);

        int requestedMinutes = reqDto.getRequestType() == OvertimeRequestType.PRE
                ? reqDto.getRequestedMinutes()
                : reqDto.getActualMinutes();

        // 연장근로 정책 조회
        OvertimePolicy otPolicy = overtimePolicyRepository
                .findEffective(companyId, reqDto.getTargetDate())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "연장근로 정책이 설정되지 않았습니다."));

        // 연장근로 단위(분) 조회
        int unit = otPolicy.getOvertimeFloorMinutes();
        if (requestedMinutes % unit != 0) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    String.format("신청 시간은 %d분 단위여야 합니다.", unit));
        }

        // 법정 한도 사전 검증
        List<LaborLawViolation> violations = laborLawValidator.validateBeforeOvertimeSubmit(
                memberId, companyId, reqDto.getTargetDate(), requestedMinutes);

        if (!violations.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, violations.get(0).message());
        }

        OvertimeRequest request = OvertimeRequest.builder()
                .memberId(memberId)
                .companyId(companyId)
                .targetDate(reqDto.getTargetDate())
                .requestType(reqDto.getRequestType())
                .plannedStartTime(reqDto.getPlannedStartTime())
                .plannedEndTime(reqDto.getPlannedEndTime())
                .requestedMinutes(reqDto.getRequestedMinutes())
                .actualStartTime(reqDto.getActualStartTime())
                .actualEndTime(reqDto.getActualEndTime())
                .actualMinutes(reqDto.getActualMinutes())
                .reason(reqDto.getReason())
                .approvalStatus(OvertimeApprovalStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .build();

        OvertimeRequest saved = overtimeRequestRepository.save(request);
        return OvertimeRequestResDto.fromEntity(saved);
    }

    /**
     * 본인 철회 (PENDING 만)
     */
    public void cancel(UUID overtimeRequestId, UUID memberId) {
        OvertimeRequest request = overtimeRequestRepository.findById(overtimeRequestId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));

        if (!request.getMemberId().equals(memberId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "본인 요청만 취소할 수 있습니다.");
        }

        request.cancel();
    }

    /**
     * 결재 ID 연결
     * approval-service 에 결재 생성 후 프런트가 호출
     */
    public OvertimeRequestResDto linkApprovalRequest(UUID overtimeRequestId,
                                                     UUID memberId,
                                                     UUID approvalRequestId) {
        OvertimeRequest request = overtimeRequestRepository.findById(overtimeRequestId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));

        if (!request.getMemberId().equals(memberId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "본인 요청만 연결할 수 있습니다.");
        }

        request.linkApprovalRequest(approvalRequestId);
        return OvertimeRequestResDto.fromEntity(request);
    }

    /**
     * 결재 승인 반영 (OvertimeApprovalService 가 호출)
     */
    public void applyApproval(UUID approvalRequestId,
                              UUID approverId,
                              LocalDateTime decidedAt,
                              int approvedMinutes) {
        OvertimeRequest request = findByApprovalRequestId(approvalRequestId);
        request.approve(approverId, approvedMinutes, decidedAt);
    }

    /**
     * 결재 반려·취소 반영 (OvertimeApprovalService 가 호출)
     */
    public void applyRejection(UUID approvalRequestId,
                               UUID approverId,
                               LocalDateTime decidedAt,
                               String note) {
        OvertimeRequest request = findByApprovalRequestId(approvalRequestId);
        request.reject(approverId, decidedAt, note);
    }

    /**
     * 72시간 경과 사후 신청 자동 만료 (배치에서 호출)
     */
    public int expirePostOvertimeRequests(LocalDateTime now) {
        LocalDateTime cutoff = now.minusHours(72);

        List<OvertimeRequest> expired = overtimeRequestRepository
                .findAllByRequestTypeAndApprovalStatusAndSubmittedAtBefore(
                        OvertimeRequestType.POST,
                        OvertimeApprovalStatus.PENDING,
                        cutoff);

        expired.forEach(r -> r.expire(now));
        return expired.size();
    }

    /**
     * 내 신청 이력
     */
    @Transactional(readOnly = true)
    public Page<OvertimeRequestResDto> findMyHistory(UUID memberId, Pageable pageable) {
        return overtimeRequestRepository
                .findAllByMemberIdOrderBySubmittedAtDesc(memberId, pageable)
                .map(OvertimeRequestResDto::fromEntity);
    }

    /**
     * 내 특정 일자 신청 목록
     */
    @Transactional(readOnly = true)
    public List<OvertimeRequestResDto> findMyByDate(UUID memberId, LocalDate date) {
        return overtimeRequestRepository
                .findAllByMemberIdAndTargetDate(memberId, date)
                .stream()
                .map(OvertimeRequestResDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 단건 조회
     */
    @Transactional(readOnly = true)
    public OvertimeRequestResDto findById(UUID overtimeRequestId, UUID companyId) {
        OvertimeRequest request = overtimeRequestRepository
                .findByOvertimeRequestIdAndCompanyId(overtimeRequestId, companyId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "신청 내역을 찾을 수 없습니다."));
        return OvertimeRequestResDto.fromEntity(request);
    }

    // 타입별 필수 필드 검증
    private void validateByType(OvertimeRequestCreateReqDto reqDto) {
        if (reqDto.getRequestType() == OvertimeRequestType.PRE) {
            if (reqDto.getPlannedStartTime() == null
                    || reqDto.getPlannedEndTime() == null
                    || reqDto.getRequestedMinutes() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "사전 신청은 예정 시각과 신청 시간은 필수입니다.");
            }
        } else {
            if (reqDto.getActualStartTime() == null
                    || reqDto.getActualEndTime() == null
                    || reqDto.getActualMinutes() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "사후 신청은 실제 시각과 실제 시간은 필수입니다.");
            }
        }
    }

    private OvertimeRequest findByApprovalRequestId(UUID approvalRequestId) {
        return overtimeRequestRepository
                .findByApprovalRequestId(approvalRequestId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "해당 결재에 연결된 신청을 찾을 수 없습니다."));
    }
}
