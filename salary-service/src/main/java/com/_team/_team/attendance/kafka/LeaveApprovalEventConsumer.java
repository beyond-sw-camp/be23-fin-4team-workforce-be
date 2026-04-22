package com._team._team.attendance.kafka;

import com._team._team.attendance.domain.MemberBalance;
import com._team._team.attendance.domain.enums.BalanceType;
import com._team._team.attendance.repository.MemberBalanceRepository;
import com._team._team.attendance.service.BalancePriorityResolver;
import com._team._team.attendance.service.LeaveApprovalService;
import com._team._team.attendance.service.LeaveRequestService;
import com._team._team.event.LeaveApprovalEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 휴가 결재 이벤트 Consumer
 * USE: LeaveRequest 상태 APPROVED + MemberBalance 차감
 * REJECT: LeaveRequest 상태 REJECTED + 잔고 건드리지 않음
 */
@Slf4j
@Component
public class LeaveApprovalEventConsumer {

    private final LeaveApprovalService leaveApprovalService;
    private final LeaveRequestService leaveRequestService;
    private final BalancePriorityResolver balancePriorityResolver;
    private final ObjectMapper objectMapper;

    @Autowired
    public LeaveApprovalEventConsumer(LeaveApprovalService leaveApprovalService,
                                      LeaveRequestService leaveRequestService,
                                      BalancePriorityResolver balancePriorityResolver,
                                      ObjectMapper objectMapper) {
        this.leaveApprovalService = leaveApprovalService;
        this.leaveRequestService = leaveRequestService;
        this.balancePriorityResolver = balancePriorityResolver;
        this.objectMapper = objectMapper;
    }

    // 이벤트 수신 후 action에 따라 USE(차감) / RESTORE(복구) 분기
    @KafkaListener(
            topics = LeaveApprovalEvent.TOPIC,
            groupId = "salary-service",
            properties = {
                    JsonDeserializer.VALUE_DEFAULT_TYPE +
                            "=com._team._team.event.LeaveApprovalEvent"
            }
    )

    public void consume(LeaveApprovalEvent event) {
        log.info("[Leave] event received. requestId={}, action={}, needsDeduction={}, days={}",
                event.getRequestId(), event.getAction(),
                event.getNeedsDeduction(), event.getDays());

        try {
            switch (event.getAction()) {
                case USE -> handleApproval(event);
                case REJECT -> handleRejection(event);
            }
        } catch (Exception e) {
            log.error("[Leave] consume failed. requestId={}", event.getRequestId(), e);
            throw e;
        }
    }

    /**
     * 승인 처리, 분할 차감 지원
     */
    private void handleApproval(LeaveApprovalEvent event) {
        boolean needsDeduction = Boolean.TRUE.equals(event.getNeedsDeduction());

        Map<BalanceType, Double> deductions = null;
        BalanceType representativeType = null;
        String deductionsJson = null;

        if (needsDeduction) {
            deductions = balancePriorityResolver.resolveDeductions(
                    event.getCompanyId(), event.getMemberId(), event.getDays());

            if (deductions == null) {
                log.error("[Leave] 잔고 부족으로 차감 불가, requestId={}", event.getRequestId());
                // 정책: 그래도 승인 상태 유지 + 관리자 알림
                // 또는 예외 던져서 재시도
            } else {
                representativeType = findRepresentativeType(deductions);
                deductionsJson = toJson(deductions);
            }
        }

        // 1. LeaveRequest 상태 APPROVED + 차감 이력 기록
        leaveRequestService.applyApproval(
                event.getRequestId(),
                event.getApproverId(),
                event.getDecidedAt(),
                representativeType,
                deductionsJson);

        // 2. MemberBalance 차감
        if (deductions != null && !deductions.isEmpty()) {
            leaveApprovalService.applyUse(event, deductions);
        }
    }

    /**
     * 반려 처리
     */
    private void handleRejection(LeaveApprovalEvent event) {
        leaveRequestService.applyRejection(
                event.getRequestId(),
                event.getApproverId(),
                event.getDecidedAt(),
                event.getNote());
    }

    /**
     * 분할 차감 중 가장 많이 차감된 휴가
     */
    private BalanceType findRepresentativeType(Map<BalanceType, Double> deductions) {
        return deductions.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String toJson(Map<BalanceType, Double> deductions) {
        try {
            return objectMapper.writeValueAsString(deductions);
        } catch (Exception e) {
            log.warn("[Leave] deductions JSON 변환 실패", e);
            return null;
        }
    }
}