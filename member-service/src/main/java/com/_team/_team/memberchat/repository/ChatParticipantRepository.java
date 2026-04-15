package com._team._team.memberchat.repository;

import com._team._team.memberchat.domain.ChatParticipant;
import com._team._team.memberchat.domain.enums.ChatParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    Optional<ChatParticipant> findByChatRoomIdAndMemberId(Long chatRoomId, UUID memberId);

    long countByChatRoom_IdAndStatus(Long chatRoomId, ChatParticipantStatus status);

    List<ChatParticipant> findByChatRoomId(Long chatRoomId);

    List<ChatParticipant> findByMemberIdAndStatus(UUID memberId, ChatParticipantStatus status);
}
