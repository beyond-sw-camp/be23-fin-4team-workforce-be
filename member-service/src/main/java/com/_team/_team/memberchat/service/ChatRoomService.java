package com._team._team.memberchat.service;

import com._team._team.memberchat.domain.*;
import com._team._team.memberchat.domain.enums.AuditAction;
import com._team._team.memberchat.domain.enums.ChatParticipantStatus;
import com._team._team.memberchat.domain.enums.ChatRoomRole;
import com._team._team.memberchat.domain.enums.ChatRoomType;
import com._team._team.memberchat.dto.req.CreateDirectRoomRequest;
import com._team._team.memberchat.dto.req.CreateGroupRoomRequest;
import com._team._team.memberchat.dto.res.ChatRoomResponse;
import com._team._team.memberchat.error.ChatErrorCode;
import com._team._team.memberchat.error.ChatException;
import com._team._team.memberchat.repository.ChatParticipantRepository;
import com._team._team.memberchat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository roomRepo;
    private final ChatParticipantRepository participantRepo;
    private final AuditLogService audit;

    @Transactional
    public ChatRoomResponse getOrCreateDirectRoom(UUID me, UUID companyId, CreateDirectRoomRequest req) {
        if (me.equals(req.otherMemberId())) {
            throw new ChatException(ChatErrorCode.VALIDATION_ERROR, "자기 자신과의 1:1 방은 만들 수 없습니다.");
        }
        return roomRepo.findDirectRoomOf(companyId, me, req.otherMemberId(), ChatRoomType.DIRECT)
                .map(existing -> {
                    // HIDDEN 참여자 → 필요 시 복구 (채팅 재개)
                    participantRepo.findByChatRoomIdAndMemberId(existing.getId(), me)
                            .ifPresent(p -> { if (p.getStatus() == ChatParticipantStatus.HIDDEN) p.restoreFromHidden(null); });
                    return toResponse(existing);
                })
                .orElseGet(() -> {
                    ChatRoom room = roomRepo.save(ChatRoom.builder()
                            .name("direct") // 클라이언트가 상대 이름으로 렌더
                            .type(ChatRoomType.DIRECT)
                            .companyId(companyId)
                            .createdBy(me)
                            .build());
                    persistParticipant(room, me, ChatRoomRole.OWNER);
                    persistParticipant(room, req.otherMemberId(), ChatRoomRole.MEMBER);
                    audit.record(me, AuditAction.ROOM_CREATED, "ROOM", room.getId());
                    return toResponse(room);
                });
    }

    @Transactional
    public ChatRoomResponse createGroup(UUID me, UUID companyId, CreateGroupRoomRequest req) {
        ChatRoom room = roomRepo.save(ChatRoom.builder()
                .name(req.name())
                .type(ChatRoomType.GROUP)
                .companyId(companyId)
                .createdBy(me)
                .build());

        persistParticipant(room, me, ChatRoomRole.OWNER);
        req.memberIds().stream()
                .distinct()
                .filter(id -> !id.equals(me))
                .forEach(id -> persistParticipant(room, id, ChatRoomRole.MEMBER));

        audit.record(me, AuditAction.ROOM_CREATED, "ROOM", room.getId());
        return toResponse(room);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> myRooms(UUID me) {
        return roomRepo.findMyRooms(me).stream().map(this::toResponse).toList();
    }

    private ChatRoomResponse toResponse(ChatRoom room) {
        long n = participantRepo.countByChatRoom_IdAndStatus(room.getId(), ChatParticipantStatus.JOINED);
        return ChatRoomResponse.from(room, n);
    }

    @Transactional
    public void addMember(UUID actor, long roomId, UUID target) {
        ChatParticipant me = participantRepo.findByChatRoomIdAndMemberId(roomId, actor)
                .orElseThrow(() -> new ChatException(ChatErrorCode.NOT_PARTICIPANT));
        if (me.getRole() != ChatRoomRole.OWNER && me.getRole() != ChatRoomRole.MODERATOR) {
            throw new ChatException(ChatErrorCode.ROLE_FORBIDDEN);
        }
        ChatRoom room = roomRepo.findById(roomId).orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));
        if (room.isDeleted()) throw new ChatException(ChatErrorCode.ROOM_DELETED);
        persistParticipant(room, target, ChatRoomRole.MEMBER);
        audit.record(actor, AuditAction.ROOM_MEMBER_ADDED, "ROOM", roomId);
    }

    @Transactional
    public void leave(UUID me, long roomId) {
        ChatParticipant p = participantRepo.findByChatRoomIdAndMemberId(roomId, me)
                .orElseThrow(() -> new ChatException(ChatErrorCode.NOT_PARTICIPANT));
        // 1:1 방은 HIDDEN(soft leave), 그룹은 LEFT
        if (p.getChatRoom().getType() == ChatRoomType.DIRECT) p.hide();
        else p.leave();
        audit.record(me, AuditAction.ROOM_MEMBER_REMOVED, "ROOM", roomId);
    }

    @Transactional
    public void deleteRoom(UUID actor, long roomId, boolean adminOverride) {
        ChatRoom room = roomRepo.findById(roomId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));
        if (room.isLegalHold()) throw new ChatException(ChatErrorCode.LEGAL_HOLD_LOCKED);

        if (!adminOverride) {
            ChatParticipant p = participantRepo.findByChatRoomIdAndMemberId(roomId, actor)
                    .orElseThrow(() -> new ChatException(ChatErrorCode.NOT_PARTICIPANT));
            if (p.getRole() != ChatRoomRole.OWNER) throw new ChatException(ChatErrorCode.ROLE_FORBIDDEN);
        }
        room.softDelete();
        audit.record(actor, AuditAction.ROOM_DELETED, "ROOM", roomId);
    }

    private void persistParticipant(ChatRoom room, UUID memberId, ChatRoomRole role) {
        participantRepo.save(ChatParticipant.builder()
                .chatRoom(room)
                .memberId(memberId)
                .role(role)
                .status(ChatParticipantStatus.JOINED)
                .build());
    }
}
