package com._team._team.member.publisher;

import com._team._team.event.MemberHiredEvent;
import com._team._team.member.event.MemberHiredInternalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

// 입사 커밋 AFTER_COMMIT
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberHiredEventListener {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onHired(MemberHiredInternalEvent e) {
        MemberHiredEvent payload = MemberHiredEvent.builder()
                .memberId(e.getMemberId())
                .companyId(e.getCompanyId())
                .jobGradeId(e.getJobGradeId())
                .jobGradeName(e.getJobGradeName())
                .jobTitleName(e.getJobTitleName())
                .hireDate(e.getHireDate())
                .baseSalary(e.getBaseSalary())
                .eventTime(Instant.now())
                .build();
        try {
            kafkaTemplate.send(MemberHiredEvent.TOPIC, e.getMemberId().toString(), payload);
            log.info("[MEMBER-HIRED] 발행 memberId={} hireDate={}", e.getMemberId(), e.getHireDate());
        } catch (Exception ex) {
            log.error("[MEMBER-HIRED] 발행 실패 memberId={} err={}", e.getMemberId(), ex.getMessage());
        }
    }
}
