package com._team._team.memberchat.service;

import com._team._team.memberchat.domain.enums.AuditAction;
import com._team._team.memberchat.domain.ChatLegalHold;
import com._team._team.memberchat.domain.ChatRoom;
import com._team._team.memberchat.error.ChatErrorCode;
import com._team._team.memberchat.error.ChatException;
import com._team._team.memberchat.repository.ChatLegalHoldRepository;
import com._team._team.memberchat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LegalHoldService {

    private final ChatLegalHoldRepository holdRepo;
    private final ChatRoomRepository roomRepo;
    private final AuditLogService audit;

    @Transactional
    public ChatLegalHold apply(UUID actor, Long roomId, UUID memberId, String reason, String caseId) {
        ChatLegalHold hold = holdRepo.save(ChatLegalHold.builder()
                .roomId(roomId)
                .memberId(memberId)
                .reason(reason)
                .caseId(caseId)
                .startAt(LocalDateTime.now())
                .build());
        if (roomId != null) {
            ChatRoom r = roomRepo.findById(roomId)
                    .orElseThrow(() -> new ChatException(ChatErrorCode.ROOM_NOT_FOUND));
            r.applyLegalHold(true);
        }
        audit.record(actor, AuditAction.LEGAL_HOLD_APPLIED, "ROOM", roomId);
        return hold;
    }

    @Transactional
    public void release(UUID actor, Long holdId) {
        ChatLegalHold hold = holdRepo.findById(holdId)
                .orElseThrow(() -> new ChatException(ChatErrorCode.VALIDATION_ERROR, "hold not found"));
        hold.release();
        if (hold.getRoomId() != null) {
            // 해당 방의 다른 active hold 가 없으면 room.legalHold=false
            if (holdRepo.findByReleasedFalseAndRoomId(hold.getRoomId()).isEmpty()) {
                roomRepo.findById(hold.getRoomId()).ifPresent(r -> r.applyLegalHold(false));
            }
        }
        audit.record(actor, AuditAction.LEGAL_HOLD_RELEASED, "ROOM", hold.getRoomId());
    }
}
