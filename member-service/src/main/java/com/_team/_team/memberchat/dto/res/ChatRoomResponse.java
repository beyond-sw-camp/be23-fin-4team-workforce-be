package com._team._team.memberchat.dto.res;

import com._team._team.memberchat.domain.ChatRoom;
import com._team._team.memberchat.domain.enums.ChatRoomType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatRoomResponse(
        Long id,
        String name,
        ChatRoomType type,
        UUID companyId,
        String imageUrl,
        boolean deleted,
        boolean legalHold,
        LocalDateTime createdAt,
        long participantCount
) {
    public static ChatRoomResponse from(ChatRoom r, long participantCount) {
        return new ChatRoomResponse(
                r.getId(), r.getName(), r.getType(), r.getCompanyId(),
                r.getImageUrl(), r.isDeleted(), r.isLegalHold(),
                r.getCreatedAt(), participantCount);
    }
}
