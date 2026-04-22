package com._team._team.attendance.service;

import com._team._team.attendance.domain.CompanyHoliday;
import com._team._team.attendance.domain.CompanyLeaveType;
import com._team._team.attendance.domain.LeaveRequest;
import com._team._team.attendance.domain.enums.BalanceType;
import com._team._team.attendance.domain.enums.LeaveApprovalStatus;
import com._team._team.attendance.dto.reqDto.LeaveRequestSubmitReqDto;
import com._team._team.attendance.repository.CompanyHolidayRepository;
import com._team._team.attendance.repository.LeaveRequestRepository;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final CompanyHolidayRepository companyHolidayRepository;
    private final CompanyLeaveTypeService companyLeaveTypeService;
    private final BalancePriorityResolver balancePriorityResolver;

    @Autowired
    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               CompanyHolidayRepository companyHolidayRepository,
                               CompanyLeaveTypeService companyLeaveTypeService, BalancePriorityResolver balancePriorityResolver) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.companyHolidayRepository = companyHolidayRepository;
        this.companyLeaveTypeService = companyLeaveTypeService;
        this.balancePriorityResolver = balancePriorityResolver;
    }

    /**
     * 직원 본인 휴가 신청
     * 유효성 검증 순서, 날짜 -> 증빙 -> 일수계산(영업일) -> 연간한도 -> 잔고
     */
    public LeaveRequest submit(UUID memberId, UUID companyId,
                               LeaveRequestSubmitReqDto reqDto) {

        // 1. 휴가 종류 불러오기
        CompanyLeaveType leaveType = companyLeaveTypeService.findActiveOrThrow(
                companyId, reqDto.getCompanyLeaveTypeId());

        // 2. 날짜 유효성
        if (reqDto.getStartDate().isAfter(reqDto.getEndDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "시작일이 종료일보다 뒤일 수 없습니다.");
        }

        // 반차는 하루만 가능
        if (leaveType.getDaysPerUse() < 1.0
                && !reqDto.getStartDate().equals(reqDto.getEndDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    leaveType.getName() + "는 하루만 신청 가능합니다.");
        }

        // 3. 증빙 필수 체크
        if ("Y".equals(leaveType.getRequireEvidenceYn())
                && (reqDto.getEvidenceFileUrl() == null
                || reqDto.getEvidenceFileUrl().isBlank())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    leaveType.getName() + "는 증빙 서류가 필요합니다.");
        }

        // 4. 사용 일수 계산, 영업일 기준 (주말 + 공휴일 제외)
        Set<LocalDate> holidays = companyHolidayRepository
                .findByCompanyIdAndHolidayDateBetweenAndDelYn(
                        companyId, reqDto.getStartDate(), reqDto.getEndDate(), "N")
                .stream()
                .map(CompanyHoliday::getHolidayDate)
                .collect(Collectors.toSet());

        int businessDays = BusinessDayCalculator.countBusinessDays(
                reqDto.getStartDate(), reqDto.getEndDate(), holidays);

        if (businessDays == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "신청 기간에 영업일이 없습니다. 주말/공휴일만 포함됐습니다.");
        }

        double usageDays = businessDays * leaveType.getDaysPerUse();

        // 5. 연간 한도 체크, 회계연도 1월 1일 ~ 12월 31일 고정
        if (leaveType.getMaxDaysPerYear() != null) {
            LocalDate[] yearRange = resolveYearRange(reqDto.getStartDate());
            Double alreadyUsed = leaveRequestRepository.sumUsedDaysInPeriod(
                    memberId, companyId, leaveType.getCompanyLeaveTypeId(),
                    yearRange[0], yearRange[1]);

            if (alreadyUsed + usageDays > leaveType.getMaxDaysPerYear()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        leaveType.getName() + " 연간 한도 "
                                + leaveType.getMaxDaysPerYear() + "일을 초과합니다. "
                                + "(사용 " + alreadyUsed + "일 + 요청 " + usageDays + "일)");
            }
        }

        // 6. 잔고 사전 체크, 분할 가능 여부 확인
        if (leaveType.deductsBalance()) {
            Map<BalanceType, Double> deductions = balancePriorityResolver
                    .resolveDeductions(companyId, memberId, usageDays);

            if (deductions == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "사용 가능한 잔여가 부족합니다. 요청 " + usageDays + "일");
            }
            // 실제 차감은 승인 시 Consumer 가 다시 계산
        }

        // 7. 저장
        LeaveRequest newRequest = LeaveRequest.builder()
                .memberId(memberId)
                .companyId(companyId)
                .companyLeaveTypeId(leaveType.getCompanyLeaveTypeId())
                .startDate(reqDto.getStartDate())
                .endDate(reqDto.getEndDate())
                .usageDays(usageDays)
                .reason(reqDto.getReason())
                .evidenceFileUrl(reqDto.getEvidenceFileUrl())
                .approvalStatus(LeaveApprovalStatus.PENDING)
                .requestedBy(memberId)
                .requestedAt(LocalDateTime.now())
                .build();

        return leaveRequestRepository.save(newRequest);
    }

    /**
     * 결재 ID 연결
     */
    public LeaveRequest linkApprovalRequest(UUID leaveRequestId, UUID memberId,
                                            UUID approvalRequestId) {
        LeaveRequest leaveRequest = findOwnRequest(leaveRequestId, memberId);
        leaveRequest.linkApprovalRequest(approvalRequestId);
        return leaveRequest;
    }

    /**
     * 본인 철회
     */
    public void cancel(UUID leaveRequestId, UUID memberId) {
        LeaveRequest leaveRequest = findOwnRequest(leaveRequestId, memberId);
        leaveRequest.cancel();
    }

    // Consumer 호출

    /**
     * 결재 승인 반영
     */
    public void applyApproval(UUID approvalRequestId, UUID approverId,
                              LocalDateTime decidedAt,
                              BalanceType deductedBalanceType,
                              String deductionsJson) {
        LeaveRequest leaveRequest = leaveRequestRepository
                .findByApprovalRequestId(approvalRequestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "해당 결재에 연결된 휴가 신청을 찾을 수 없습니다."));

        leaveRequest.approve(approverId, decidedAt, deductedBalanceType, deductionsJson);
    }

    /**
     * 결재 반려/취소 반영, Kafka Consumer 가 호출
     */
    public void applyRejection(UUID approvalRequestId, UUID approverId,
                               LocalDateTime decidedAt, String note) {
        LeaveRequest leaveRequest = leaveRequestRepository
                .findByApprovalRequestId(approvalRequestId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "해당 결재에 연결된 휴가 신청을 찾을 수 없습니다."));

        leaveRequest.reject(approverId, decidedAt, note);
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

    // 단건 조회, 본인 또는 관리자 (회사 검증 포함)
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
    private LeaveRequest findOwnRequest(UUID leaveRequestId, UUID memberId) {
        LeaveRequest leaveRequest = leaveRequestRepository
                .findByLeaveRequestIdAndMemberId(leaveRequestId, memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "휴가 신청 내역을 찾을 수 없습니다."));

        if (!"N".equals(leaveRequest.getDelYn())) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "이미 삭제된 휴가 신청입니다.");
        }
        return leaveRequest;
    }

    /**
     * 연간 한도 체크용 범위 계산, 캘린더 연도 1월 1일 ~ 12월 31일 고정
     */
    private LocalDate[] resolveYearRange(LocalDate requestDate) {
        LocalDate yearStart = LocalDate.of(requestDate.getYear(), 1, 1);
        LocalDate yearEnd = LocalDate.of(requestDate.getYear(), 12, 31);
        return new LocalDate[]{yearStart, yearEnd};
    }
}