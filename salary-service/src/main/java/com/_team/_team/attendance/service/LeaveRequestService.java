package com._team._team.attendance.service;

import com._team._team.attendance.domain.CompanyLeaveType;
import com._team._team.attendance.domain.LeaveRequest;
import com._team._team.attendance.domain.MemberBalance;
import com._team._team.attendance.domain.enums.BalanceType;
import com._team._team.attendance.domain.enums.LeaveApprovalStatus;
import com._team._team.attendance.dto.reqDto.LeaveRequestSubmitReqDto;
import com._team._team.attendance.repository.LeaveRequestRepository;
import com._team._team.attendance.repository.MemberBalanceRepository;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final MemberBalanceRepository memberBalanceRepository;
    private final CompanyLeaveTypeService companyLeaveTypeService;

    @Autowired
    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               MemberBalanceRepository memberBalanceRepository,
                               CompanyLeaveTypeService companyLeaveTypeService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.memberBalanceRepository = memberBalanceRepository;
        this.companyLeaveTypeService = companyLeaveTypeService;
    }
    /**
     * 직원 본인 휴가 신청
     * 유효성 검증 순서, 날짜 -> 증빙 -> 일수계산 -> 연간한도 -> 잔고
     */
    public LeaveRequest submit(UUID memberId, UUID companyId,
                               LeaveRequestSubmitReqDto reqDto) {

        // 1. 휴가 종류 확보
        CompanyLeaveType type = companyLeaveTypeService.findActiveOrThrow(
                companyId, reqDto.getCompanyLeaveTypeId());

        // 2. 날짜 유효성
        if (reqDto.getStartDate().isAfter(reqDto.getEndDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "시작일이 종료일보다 뒤일 수 없습니다.");
        }

        // 반차는 하루만 가능
        if (type.getDaysPerUse() < 1.0
                && !reqDto.getStartDate().equals(reqDto.getEndDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    type.getName() + "는 하루만 신청 가능합니다.");
        }

        // 3. 증빙 필수 체크
        if ("Y".equals(type.getRequireEvidenceYn())
                && (reqDto.getEvidenceFileUrl() == null
                || reqDto.getEvidenceFileUrl().isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    type.getName() + "는 증빙 서류가 필요합니다.");
        }

        // 4. 사용 일수 계산
        long daysBetween = ChronoUnit.DAYS.between(
                reqDto.getStartDate(), reqDto.getEndDate()) + 1;
        double usageDays = daysBetween * type.getDaysPerUse();

        // 5. 연간 한도 체크
        if (type.getMaxDaysPerYear() != null) {
            int year = reqDto.getStartDate().getYear();
            Double alreadyUsed = leaveRequestRepository.sumUsedDaysInYear(
                    memberId, companyId, type.getCompanyLeaveTypeId(), year);
            if (alreadyUsed + usageDays > type.getMaxDaysPerYear()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        type.getName() + " 연간 한도 "
                                + type.getMaxDaysPerYear() + "일을 초과합니다.");
            }
        }

        // 6. 잔고 사전 체크, 차감 대상 휴가만
        if (type.deductsBalance()) {
            MemberBalance balance = memberBalanceRepository
                    .findByCompanyIdAndMemberIdAndBalanceTypeAndDelYn(
                            companyId, memberId, type.getBalanceType(), "N")
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.BAD_REQUEST,
                            type.getBalanceType() + " 잔여가 없습니다. 먼저 부여받아야 합니다."));

            if (balance.getRemaining() < usageDays) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        String.format("잔여 부족, 남은 %s %.1f일 < 요청 %.1f일",
                                type.getBalanceType(),
                                balance.getRemaining(), usageDays));
            }
        }

        // 7. 저장
        LeaveRequest entity = LeaveRequest.builder()
                .memberId(memberId)
                .companyId(companyId)
                .companyLeaveTypeId(type.getCompanyLeaveTypeId())
                .startDate(reqDto.getStartDate())
                .endDate(reqDto.getEndDate())
                .usageDays(usageDays)
                .reason(reqDto.getReason())
                .evidenceFileUrl(reqDto.getEvidenceFileUrl())
                .approvalStatus(LeaveApprovalStatus.PENDING)
                .requestedBy(memberId)
                .requestedAt(LocalDateTime.now())
                .build();

        return leaveRequestRepository.save(entity);
    }

    /**
     * 결재 ID 연결, 프론트에서 approval-service 결재 생성 후 호출
     */
    public LeaveRequest linkApprovalRequest(UUID leaveRequestId, UUID memberId,
                                            UUID approvalRequestId) {
        LeaveRequest entity = findOwn(leaveRequestId, memberId);
        entity.linkApprovalRequest(approvalRequestId);
        return entity;
    }

    /**
     * 본인 철회
     */
    public void cancel(UUID leaveRequestId, UUID memberId) {
        LeaveRequest entity = findOwn(leaveRequestId, memberId);
        entity.cancel();
    }

    /**
     * 결재 승인 반영, Kafka Consumer 가 호출
     * deductedBalanceType 은 실제 차감된 대표 풀 (차감 없는 휴가면 null)
     */
    public void applyApproval(UUID approvalRequestId, UUID approverId,
                              LocalDateTime decidedAt,
                              BalanceType deductedBalanceType) {
        LeaveRequest entity = leaveRequestRepository
                .findByApprovalRequestId(approvalRequestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "해당 결재에 연결된 휴가 신청을 찾을 수 없습니다."));

        entity.approve(approverId, decidedAt, deductedBalanceType);
    }

    /**
     * 결재 반려/취소 반영, Kafka Consumer 가 호출
     */
    public void applyRejection(UUID approvalRequestId, UUID approverId,
                               LocalDateTime decidedAt, String note) {
        LeaveRequest entity = leaveRequestRepository
                .findByApprovalRequestId(approvalRequestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "해당 결재에 연결된 휴가 신청을 찾을 수 없습니다."));

        entity.reject(approverId, decidedAt, note);
    }

    // 직원 본인 이력, 최근 순
    @Transactional(readOnly = true)
    public Page<LeaveRequest> findMyHistory(UUID memberId, UUID companyId,
                                            Pageable pageable) {
        return leaveRequestRepository
                .findAllByMemberIdAndCompanyIdAndDelYnOrderByRequestedAtDesc(
                        memberId, companyId, "N", pageable);
    }

    // 특정일 본인 신청 목록 (같은 날 여러 건 가능)
    @Transactional(readOnly = true)
    public List<LeaveRequest> findMyByDate(UUID memberId, LocalDate date) {
        return leaveRequestRepository
                .findAllByMemberIdAndStartDateAndDelYn(memberId, date, "N");
    }

    // 단건 조회, 본인 또는 관리자
    @Transactional(readOnly = true)
    public LeaveRequest findById(UUID leaveRequestId, UUID companyId) {
        return leaveRequestRepository
                .findByLeaveRequestIdAndCompanyIdAndDelYn(leaveRequestId, companyId, "N")
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "휴가 신청을 찾을 수 없습니다."));
    }

    // 관리자 상태별 목록, 오래된 순
    @Transactional(readOnly = true)
    public List<LeaveRequest> findByCompanyAndStatus(UUID companyId,
                                                     LeaveApprovalStatus status) {
        return leaveRequestRepository
                .findAllByCompanyIdAndApprovalStatusAndDelYnOrderByRequestedAtAsc(
                        companyId, status, "N");
    }

    // 근태 마감 배치용, 특정일에 적용되는 승인된 휴가 조회
    @Transactional(readOnly = true)
    public Optional<LeaveRequest> findActiveOnDate(UUID companyId, UUID memberId,
                                                   LocalDate date) {
        return leaveRequestRepository.findActiveOnDate(companyId, memberId, date);
    }

    // 본인 소유 검증
    private LeaveRequest findOwn(UUID leaveRequestId, UUID memberId) {
        LeaveRequest entity = leaveRequestRepository
                .findByLeaveRequestIdAndMemberId(leaveRequestId, memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "휴가 신청내역을 찾을 수 없습니다."));

        if (!"N".equals(entity.getDelYn())) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "이미 삭제된 휴가 신청건 입니다.");
        }
        return entity;
    }
}
