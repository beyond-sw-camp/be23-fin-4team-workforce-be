package com._team._team.goal.service;

import com._team._team.dto.NotificationMessage;
import com._team._team.notification.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * goal-service 의 알림 발행 진입점.
 *
 * approval-service 의 {@code ApprovalNotificationService} 와 동일한 역할:
 * 각 도메인 서비스가 직접 {@link ApplicationEventPublisher} 를 호출하지 않고 본 서비스를 거치게 해
 * {@link NotificationMessage} 생성·검증·로깅을 한 곳에 모은다.
 *
 * 발행된 이벤트는 {@code common.NotificationEventListener}
 * ({@code @TransactionalEventListener(AFTER_COMMIT) @Async}) 가 수신 → Kafka {@code notification} 토픽으로 발행
 * → member-service 의 NotificationConsumer 가 DB 저장 + Redis Pub/Sub → SseEmitterManager 로 SSE 전달.
 *
 * 기존에 산발적인 호출에서 발생했던 문제를 방어:
 *   - {@code receiverId} null 이면 발행을 스킵 (member-service consumer 에서 {@code findById(null)} 예외 방지)
 *   - 트랜잭션 밖에서 호출 시 WARN 로그 (AFTER_COMMIT 훅이 스킵되는 상황을 즉시 인지)
 *   - DEBUG 로그로 모든 발행 추적 가능
 */
@Slf4j
@Service
public class GoalNotificationService {

    private final ApplicationEventPublisher eventPublisher;

    public GoalNotificationService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /** 일반 알림 발행 — 발신자가 있는 경우. */
    public void publish(UUID receiverId,
                        UUID senderId,
                        NotificationType type,
                        String content,
                        UUID targetId,
                        String targetType) {
        if (receiverId == null) {
            log.warn("알림 수신자 없음 — skip. type={}, targetId={}, content={}", type, targetId, content);
            return;
        }
        if (type == null) {
            log.warn("알림 타입 누락 — skip. receiverId={}, targetId={}", receiverId, targetId);
            return;
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            // AFTER_COMMIT 훅은 활성 트랜잭션이 없으면 스킵된다.
            // 호출부가 @Transactional 메서드 안에서 실행되고 있는지 확인할 것.
            log.warn("트랜잭션 밖에서 알림 발행 — AFTER_COMMIT 이벤트 리스너가 실행되지 않을 수 있음. "
                    + "type={}, receiverId={}, targetId={}", type, receiverId, targetId);
        }

        NotificationMessage message = NotificationMessage.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .notificationType(type)
                .content(content)
                .targetId(targetId)
                .targetType(targetType)
                .build();

        eventPublisher.publishEvent(message);
        log.debug("알림 발행 type={}, receiverId={}, senderId={}, targetId={}",
                type, receiverId, senderId, targetId);
    }

    /** 시스템 발송 — senderId 가 없는 케이스 (평가 리마인드 등). */
    public void publishSystem(UUID receiverId,
                              NotificationType type,
                              String content,
                              UUID targetId,
                              String targetType) {
        publish(receiverId, null, type, content, targetId, targetType);
    }
}
