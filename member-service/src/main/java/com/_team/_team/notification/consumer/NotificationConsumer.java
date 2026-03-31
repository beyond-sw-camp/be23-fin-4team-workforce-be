package com._team._team.notification.consumer;

import com._team._team.dto.NotificationMessage;
import com._team._team.member.domain.Member;
import com._team._team.member.repository.MemberRepository;

import com._team._team.notification.domain.Notification;
import com._team._team.notification.repository.NotificationRepository;
import com._team._team.redis.RedisChannel;
import com._team._team.redis.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final RedisMessagePublisher redisMessagePublisher;

    @KafkaListener(topics = "notification", groupId = "member-service")
    public void consume(NotificationMessage message) {
        try {
            // 1. 알림 DB 저장
            Member receiver = memberRepository.findById(message.getReceiverId())
                    .orElseThrow();

            Notification notification = Notification.builder()
                    .member(receiver)
                    .notificationType(message.getNotificationType())
                    .content(message.getContent())
                    .targetId(message.getTargetId())
                    .targetType(message.getTargetType())
                    .isRead("NO")
                    .build();

            Notification saved = notificationRepository.save(notification);

            // 2. Redis Pub/Sub 발행
            // createdAt을 ID로 설정 (Last-Event-ID 용)
            message.setEventId(saved.getCreatedAt().toString()); // ← 추가
            redisMessagePublisher.publish(
                    RedisChannel.NOTIFICATION_CHANNEL,
                    message
            );

        } catch (Exception e) {
            log.error("알림 Consumer 처리 실패: {}", e.getMessage());
        }
    }
}