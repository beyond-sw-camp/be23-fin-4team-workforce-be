package com._team._team.attendance.kafka;

import com._team._team.attendance.service.AttendanceCorrectionService;
import com._team._team.event.AttendanceCorrectionApprovalEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

/**
 * 근태정정 결재 이벤트 Consumer
 */
@Slf4j
@Component
public class AttendanceCorrectionApprovalEventConsumer {

    private final AttendanceCorrectionService attendanceCorrectionService;

    @Autowired
    public AttendanceCorrectionApprovalEventConsumer(AttendanceCorrectionService attendanceCorrectionService) {
        this.attendanceCorrectionService = attendanceCorrectionService;
    }

    @KafkaListener(
            topics = AttendanceCorrectionApprovalEvent.TOPIC,
            groupId = "salary-service",
            properties = {
                    JsonDeserializer.VALUE_DEFAULT_TYPE +
                            "=com._team._team.event.AttendanceCorrectionApprovalEvent"
            }
    )
    public void consume(AttendanceCorrectionApprovalEvent event) {
        log.info("[AttendanceCorrection] event received. requestId={}, action={}, memberId={}, date={}",
                event.getRequestId(), event.getAction(),
                event.getMemberId(), event.getAttendanceDate());

        try {
            switch (event.getAction()) {
                case APPROVE -> attendanceCorrectionService.applyApprovedCorrection(
                        event.getCompanyId(),
                        event.getMemberId(),
                        event.getApproverId(),
                        event.getAttendanceDate(),
                        event.getRequestedClockIn(),
                        event.getRequestedClockOut(),
                        event.getReason());
                case REJECT, CANCEL -> log.info(
                        "[AttendanceCorrection] {} 처리 - 일일근태 변경 없음, requestId={}",
                        event.getAction(), event.getRequestId());
            }
        } catch (Exception e) {
            log.error("[AttendanceCorrection] consume failed. requestId={}", event.getRequestId(), e);
            throw e;
        }
    }
}
