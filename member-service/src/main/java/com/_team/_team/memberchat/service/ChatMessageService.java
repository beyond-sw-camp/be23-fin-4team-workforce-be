package com._team._team.memberchat.service;

import com._team._team.memberchat.domain.*;
import com._team._team.memberchat.domain.enums.AuditAction;
import com._team._team.memberchat.dto.req.ChatSendRequest;
import com._team._team.memberchat.dto.res.ChatMessageEvent;
import com._team._team.memberchat.dto.res.ChatMessageResponse;
import com._team._team.memberchat.dto.res.ChatSenderView;
import com._team._team.memberchat.error.ChatErrorCode;
import com._team._team.memberchat.error.ChatException;
import com._team._team.memberchat.repository.ChatMessageRepository;
import com._team._team.memberchat.repository.ChatMessageRevisionRepository;
import com._team._team.memberchat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatRoomRepository roomRepo;
    private final ChatMessageRepository messageRepo;
    private final ChatMessageRevisionRepository revisionRepo;
    private final ChatAuthPolicy authPolicy;
    private final AuditLogService audit;
    private final RedisChatPubSubService pubsub;
    private final ChatMessageSenderEnricher senderEnricher;

    @Transactional
    public ChatMessageEvent saveAndPublish(UUID senderId, long roomId, ChatSendRequest req) {
        authPolicy.canSendMessage(senderId, roomId, req.type());

        if (req.clientMessageId() != null) {
            messageRepo.findByClientMessageId(req.clientMessageId())
                    .ifPresent(existing -> { throw new ChatException(ChatErrorCode.DUPLICATE); });
        }

        ChatRoom room = roomRepo.findById(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));
        if (room.isDeleted()) throw new ChatException(ChatErrorCode.ROOM_DELETED);

        ChatMessage saved = messageRepo.save(ChatMessage.builder()
                .chatRoom(room)
                .senderId(senderId)
                .type(req.type())
                .content(req.content())
                .clientMessageId(req.clientMessageId())
                .replyToId(req.replyToId())
                .build());

        audit.record(senderId, AuditAction.MESSAGE_SENT, "MESSAGE", saved.getId());

        ChatMessageEvent event = ChatMessageEvent.created(saved);
        pubsub.publish(roomId, event);
        return event;
    }

    @Transactional
    public ChatMessageEvent editMessage(UUID actorId, long messageId, String newContent) {
        ChatMessage m = messageRepo.findById(messageId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.MESSAGE_NOT_FOUND));
        if (m.isDeleted()) throw new ChatException(ChatErrorCode.MESSAGE_NOT_FOUND);
        if (!m.getSenderId().equals(actorId)) throw new ChatException(ChatErrorCode.EDIT_FORBIDDEN);
        if (m.isOlderThan(Duration.ofHours(24))) throw new ChatException(ChatErrorCode.EDIT_WINDOW_EXPIRED);

        revisionRepo.save(ChatMessageRevision.builder()
                .messageId(messageId)
                .action(ChatMessageRevision.RevisionAction.EDIT)
                .previousContent(m.getContent())
                .actorId(actorId)
                .build());

        m.updateContent(newContent);
        audit.record(actorId, AuditAction.MESSAGE_EDITED, "MESSAGE", messageId);

        ChatMessageEvent event = ChatMessageEvent.edited(m);
        pubsub.publish(m.getChatRoom().getId(), event);
        return event;
    }

    @Transactional
    public ChatMessageEvent deleteMessage(UUID actorId, long messageId, boolean adminOverride) {
        ChatMessage m = messageRepo.findById(messageId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.MESSAGE_NOT_FOUND));
        if (m.isDeleted()) throw new ChatException(ChatErrorCode.MESSAGE_NOT_FOUND);
        if (!adminOverride && !m.getSenderId().equals(actorId)) {
            throw new ChatException(ChatErrorCode.EDIT_FORBIDDEN);
        }
        if (m.getChatRoom().isLegalHold()) {
            throw new ChatException(ChatErrorCode.LEGAL_HOLD_LOCKED);
        }

        revisionRepo.save(ChatMessageRevision.builder()
                .messageId(messageId)
                .action(ChatMessageRevision.RevisionAction.DELETE)
                .previousContent(m.getContent())
                .actorId(actorId)
                .build());

        m.softDelete();
        audit.record(actorId, AuditAction.MESSAGE_DELETED, "MESSAGE", messageId);

        ChatMessageEvent event = ChatMessageEvent.deleted(m);
        pubsub.publish(m.getChatRoom().getId(), event);
        return event;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> history(UUID memberId, long roomId, Long cursor, int size) {
        authPolicy.requireActiveParticipant(memberId, roomId);
        var room = roomRepo.findById(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));
        List<ChatMessage> list = messageRepo.pageBefore(roomId, cursor, PageRequest.of(0, Math.min(size, 200)));
        return toMessageResponses(room.getCompanyId(), roomId, list);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> sync(UUID memberId, long roomId, Long lastSeenMessageId, int size) {
        authPolicy.requireActiveParticipant(memberId, roomId);
        var room = roomRepo.findById(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));
        long lastSeen = lastSeenMessageId == null ? 0L : lastSeenMessageId;
        List<ChatMessage> list = messageRepo.fetchAfter(roomId, lastSeen, PageRequest.of(0, Math.min(size, 500)));
        return toMessageResponses(room.getCompanyId(), roomId, list);
    }

    private List<ChatMessageResponse> toMessageResponses(UUID companyId, long roomId, List<ChatMessage> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Set<UUID> senderIds = list.stream().map(ChatMessage::getSenderId).collect(Collectors.toCollection(HashSet::new));
        Map<UUID, ChatSenderView> views = senderEnricher.loadSenders(companyId, senderIds);
        return list.stream()
                .map(m -> ChatMessageResponse.from(m, roomId, views.get(m.getSenderId())))
                .toList();
    }
}
