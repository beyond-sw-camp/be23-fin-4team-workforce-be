package com._team._team.attendance.kafka;

import com._team._team.attendance.service.LeaveApprovalService;
import com._team._team.event.LeaveApprovalEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

/**
 * 휴가 결재 이벤트 컨슈머
 * approval-service가 쏜 leave-approval 토픽을 구독.
 * producer 쪽이 type header를 안 붙이므로 VALUE_DEFAULT_TYPE으로 타입 고정해서 역직렬화.
 */
@Slf4j
@Component
public class LeaveApprovalEventConsumer {

    private final LeaveApprovalService leaveApprovalService;

    @Autowired
    public LeaveApprovalEventConsumer(LeaveApprovalService leaveApprovalService) {
        this.leaveApprovalService = leaveApprovalService;
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
        log.info("[Leave] event received. requestId={}, action={}, days={}",
                event.getRequestId(), event.getAction(), event.getDays());

        try {
            switch (event.getAction()) {
                case USE -> leaveApprovalService.applyUse(event);
                case RESTORE -> leaveApprovalService.applyRestore(event);
            }
        } catch (Exception e) {
            log.error("[Leave] consume failed. requestId={}", event.getRequestId(), e);
            throw e;
        }
    }
}
