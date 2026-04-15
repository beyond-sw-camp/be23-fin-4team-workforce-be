package com._team._team.salary.kafka;

import com._team._team.kafka.event.MemberHiredEvent;
import com._team._team.salary.Service.SalaryAutoCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

// 신규 입사 Kafka 수신 -> Salary 자동 생성
@Slf4j
@Component
public class MemberHiredEventConsumer {

    private final SalaryAutoCreateService salaryAutoCreateService;

    @Autowired
    public MemberHiredEventConsumer(SalaryAutoCreateService salaryAutoCreateService) {
        this.salaryAutoCreateService = salaryAutoCreateService;
    }

    @KafkaListener(
            topics = MemberHiredEvent.TOPIC,
            groupId = "salary-service",
            properties = {
                    JsonDeserializer.VALUE_DEFAULT_TYPE +
                            "=com._team._team.kafka.event.MemberHiredEvent"
            }
    )
    public void consume(MemberHiredEvent event) {
        log.info("[MEMBER-HIRED] 수신 memberId={} hireDate={}",
                event.getMemberId(), event.getHireDate());
        try {
            salaryAutoCreateService.bootstrapSalary(event);
        } catch (Exception e) {
            log.error("[MEMBER-HIRED] 처리 실패 memberId={} err={}",
                    event.getMemberId(), e.getMessage(), e);
            throw e;
        }
    }
}
