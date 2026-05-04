package com._team._team.personnel.consumer;

import com._team._team.event.PersonnelOrderApprovedEvent;
import com._team._team.personnel.service.PersonnelOrderApplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

/**
 * 인사발령 결재 승인 Kafka 수신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonnelOrderApprovalConsumer {

    private final PersonnelOrderApplyService applyService;

    @KafkaListener(
            topics = PersonnelOrderApprovedEvent.TOPIC,
            groupId = "member-service-personnel-order",
            properties = {
                    JsonDeserializer.VALUE_DEFAULT_TYPE +
                            "=com._team._team.event.PersonnelOrderApprovedEvent"
            }
    )
    public void consume(PersonnelOrderApprovedEvent event) {
        log.info("[PersonnelOrder] received. approvalDocumentId={} items={}",
                event.getApprovalDocumentId(),
                event.getItems() != null ? event.getItems().size() : 0);
        applyService.apply(event);
    }
}
