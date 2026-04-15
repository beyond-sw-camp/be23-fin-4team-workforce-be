package com._team._team.memberchat.service;

import com._team._team.memberchat.domain.ChatParticipant;
import com._team._team.memberchat.domain.ChatReadReceipt;
import com._team._team.memberchat.repository.ChatReadReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * 멀티디바이스 일관성을 위해 lastReadMessageId 는 단조증가(monotonic)로만 갱신.
 * 클라이언트는 디바이스마다 다른 deviceId 를 보낸다.
 */
@Service
@RequiredArgsConstructor
public class ReadReceiptService {

    private final ChatReadReceiptRepository receiptRepo;
    private final ChatAuthPolicy authPolicy;
    private final RedisChatPubSubService pubsub;

    @Transactional
    public void ack(UUID memberId, long roomId, long messageId, String deviceId) {
        ChatParticipant p = authPolicy.requireActiveParticipant(memberId, roomId);
        // 단조 증가
        p.updateLastRead(messageId);

        // 수신 영수증(개별 메시지 단위는 감사/통계용, 방-최종 읽음은 participant.lastReadMessageId)
        receiptRepo.findByMessageIdAndMemberId(messageId, memberId)
                .orElseGet(() -> receiptRepo.save(ChatReadReceipt.builder()
                        .roomId(roomId)
                        .messageId(messageId)
                        .memberId(memberId)
                        .deviceId(deviceId)
                        .build()));

        // 다른 참여자들에게 읽음 전파
        pubsub.publish(roomId, Map.of(
                "eventType", "READ",
                "roomId", roomId,
                "memberId", memberId.toString(),
                "messageId", messageId
        ));
    }
}
