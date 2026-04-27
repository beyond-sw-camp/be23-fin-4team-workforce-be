package com._team._team.attendance.service;

import com._team._team.attendance.domain.CompanyLeaveType;
import com._team._team.attendance.domain.LeavePromotionLog;
import com._team._team.attendance.domain.LeaveRequest;
import com._team._team.attendance.domain.MemberBalance;
import com._team._team.attendance.domain.enums.BalanceType;
import com._team._team.attendance.domain.enums.PromotionLogStatus;
import com._team._team.attendance.domain.enums.PromotionStage;
import com._team._team.attendance.dto.reqDto.LeavePromotionDesignateReqDto;
import com._team._team.attendance.dto.reqDto.LeavePromotionRespondReqDto;
import com._team._team.attendance.dto.reqDto.MemberBalanceUseReqDto;
import com._team._team.attendance.dto.resDto.LeavePromotionMyResDto;
import com._team._team.attendance.dto.resDto.LeavePromotionNoResponseResDto;
import com._team._team.attendance.repository.CompanyLeaveTypeRepository;
import com._team._team.attendance.repository.LeavePromotionLogRepository;
import com._team._team.attendance.repository.LeaveRequestRepository;
import com._team._team.attendance.repository.MemberBalanceRepository;
import com._team._team.dto.BusinessException;
import com._team._team.dto.NotificationMessage;
import com._team._team.notification.NotificationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

// 연차 사용 촉진제 회신 처리 서비스, 직원 회신은 계획 기록만 잔여 차감 없음
@Slf4j
@Service
@Transactional
public class LeavePromotionResponseService {

    // 2차 통보 후 며칠 지나야 회사가 강제 지정 가능 한지
    private static final int DESIGNATION_GRACE_DAYS = 10;

    private static final String DEFAULT_LEAVE_TYPE_CODE = "ANNUAL";

    private final LeavePromotionLogRepository promotionLogRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final MemberBalanceRepository memberBalanceRepository;
    private final CompanyLeaveTypeRepository companyLeaveTypeRepository;
    private final MemberBalanceService memberBalanceService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Autowired
    public LeavePromotionResponseService(LeavePromotionLogRepository promotionLogRepository,
                                         LeaveRequestRepository leaveRequestRepository,
                                         MemberBalanceRepository memberBalanceRepository,
                                         CompanyLeaveTypeRepository companyLeaveTypeRepository,
                                         MemberBalanceService memberBalanceService,
                                         ApplicationEventPublisher eventPublisher) {
        this.promotionLogRepository = promotionLogRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.memberBalanceRepository = memberBalanceRepository;
        this.companyLeaveTypeRepository = companyLeaveTypeRepository;
        this.memberBalanceService = memberBalanceService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    // 직원 본인이 받은 촉진 통보 목록 응답 필요한 것 우선
    @Transactional(readOnly = true)
    public List<LeavePromotionMyResDto> findMyPromotions(UUID memberId) {
        List<LeavePromotionLog> logs = promotionLogRepository
                .findByMemberIdOrderBySentOnDesc(memberId);

        List<LeavePromotionMyResDto> result = new ArrayList<>();
        for (LeavePromotionLog log : logs) {
            MemberBalance bal = memberBalanceRepository
                    .findById(log.getMemberBalanceId()).orElse(null);
            LocalDate exp = bal != null ? bal.getExpirationDate() : null;
            Double remaining = bal != null ? bal.getRemaining() : null;
            result.add(LeavePromotionMyResDto.from(log, exp, remaining));
        }
        // SENT 우선 정렬 (응답 필요한 것 위로)
        result.sort(Comparator.comparing(d -> d.getStatus() != PromotionLogStatus.SENT));
        return result;
    }

    // 직원 회신 처리
    // 핵심 LeaveRequest 자동 생성 안 함 잔여 차감 없음 회사 면책 기록만
    public void respondAsMember(UUID memberId, UUID promotionLogId,
                                LeavePromotionRespondReqDto reqDto) {
        LeavePromotionLog log = promotionLogRepository.findById(promotionLogId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "촉진 통보를 찾을 수 없습니다"));

        if (!log.getMemberId().equals(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "본인의 통보만 회신 가능합니다");
        }
        if (log.getStatus() != PromotionLogStatus.SENT) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "이미 회신했거나 회사가 강제 지정한 통보입니다");
        }

        String json = serializeDates(reqDto.getPlannedDates());
        log.acknowledge(json);
    }

    // 회사 무응답자 리스트 (2차 통보 후 10일 경과만 노출)
    @Transactional(readOnly = true)
    public List<LeavePromotionNoResponseResDto> findNoResponse(UUID companyId,
                                                               LocalDate today) {
        List<LeavePromotionLog> logs = promotionLogRepository
                .findByCompanyIdAndStageAndStatus(
                        companyId, PromotionStage.SECOND, PromotionLogStatus.SENT);

        List<LeavePromotionNoResponseResDto> result = new ArrayList<>();
        for (LeavePromotionLog log : logs) {
            long daysSinceSent = ChronoUnit.DAYS.between(log.getSentOn(), today);
            if (daysSinceSent < DESIGNATION_GRACE_DAYS) continue;

            MemberBalance bal = memberBalanceRepository
                    .findById(log.getMemberBalanceId()).orElse(null);
            if (bal == null) continue;

            result.add(LeavePromotionNoResponseResDto.from(
                    log, bal.getExpirationDate(), bal.getRemaining(), daysSinceSent));
        }
        return result;
    }

    // 회사 강제 지정 노무수령 거부
    public void designateByAdmin(UUID adminId, UUID companyId, UUID promotionLogId,
                                 LeavePromotionDesignateReqDto reqDto, LocalDate today) {
        LeavePromotionLog log = promotionLogRepository.findById(promotionLogId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "촉진 통보를 찾을 수 없습니다"));

        if (!log.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "다른 회사의 통보는 처리할 수 없습니다");
        }
        if (log.getStage() != PromotionStage.SECOND) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "2차 통보 건만 강제 지정 가능합니다");
        }
        if (log.getStatus() != PromotionLogStatus.SENT) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "이미 회신되었거나 강제 지정된 통보입니다");
        }

        long daysSinceSent = ChronoUnit.DAYS.between(log.getSentOn(), today);
        if (daysSinceSent < DESIGNATION_GRACE_DAYS) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "2차 통보 후 10일 경과 후에만 강제 지정 가능합니다");
        }

        UUID companyLeaveTypeId = companyLeaveTypeRepository
                .findByCompanyIdAndCodeAndDelYn(companyId, DEFAULT_LEAVE_TYPE_CODE, "N")
                .map(CompanyLeaveType::getCompanyLeaveTypeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "기본 연차 종류를 찾을 수 없습니다"));

        // 날짜별 LeaveRequest 생성 (휴일 주말 제외 정책은 추후 도입 가능)
        for (LocalDate date : reqDto.getDates()) {
            LeaveRequest req = LeaveRequest.createDesignated(
                    log.getMemberId(),
                    companyId,
                    companyLeaveTypeId,
                    date, date,
                    1.0,
                    reqDto.getReason(),
                    adminId);
            leaveRequestRepository.save(req);
        }

        // 잔여 차감
        for (LocalDate date : reqDto.getDates()) {
            MemberBalanceUseReqDto useReq = MemberBalanceUseReqDto.builder()
                    .balanceType(BalanceType.ANNUAL)
                    .days(1.0)
                    .leaveDate(date)
                    .build();
            memberBalanceService.useBalance(companyId, log.getMemberId(), useReq);
        }

        // 로그 업데이트
        log.designate(serializeDates(reqDto.getDates()), reqDto.getReason());

        // 직원 알림 발송
        publishDesignationNotice(log.getMemberId(), reqDto.getDates(), log.getMemberBalanceId());
    }

    private void publishDesignationNotice(UUID memberId, List<LocalDate> dates,
                                          UUID memberBalanceId) {
        String content = "회사가 다음 날짜를 연차로 지정했습니다 (노무 수령 거부) "
                + dates;
        eventPublisher.publishEvent(NotificationMessage.builder()
                .receiverId(memberId)
                .senderId(null)
                .notificationType(NotificationType.LEAVE_DESIGNATION)
                .content(content)
                .targetId(memberBalanceId)
                .targetType("MEMBER_BALANCE")
                .build());
    }

    private String serializeDates(List<LocalDate> dates) {
        try {
            return objectMapper.writeValueAsString(dates);
        } catch (JsonProcessingException e) {
            log.error("dates serialize fail", e);
            return "[]";
        }
    }
}