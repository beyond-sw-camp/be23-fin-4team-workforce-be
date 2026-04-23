package com._team._team.approval.service;

import com._team._team.event.CalendarApprovalEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CalendarApprovalEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public CalendarApprovalEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publish(CalendarApprovalEvent event) {
        eventPublisher.publishEvent(event);
        log.info("[Calendar] event published to Spring. requestId={}, title={}",
                event.getRequestId(), event.getTitle());
    }

//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    public CalendarApprovalEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
//        this.kafkaTemplate = kafkaTemplate;
//    }
//
//    public void publish(CalendarApprovalEvent event) {
//        try {
//            kafkaTemplate.send(
//                    CalendarApprovalEvent.TOPIC,
//                    event.getRequestId().toString(),
//                    event
//            );
//            log.info("[Calendar] event published. requestId={}, title={}",
//                    event.getRequestId(), event.getTitle());
//        } catch (Exception e) {
//            log.error("[Calendar] event publish failed. requestId={}",
//                    event.getRequestId(), e);
//        }
//    }
}
