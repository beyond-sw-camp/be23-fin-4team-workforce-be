package com._team._team.memberchat.repository;

import com._team._team.memberchat.domain.ChatLegalHold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatLegalHoldRepository extends JpaRepository<ChatLegalHold, Long> {
    List<ChatLegalHold> findByReleasedFalseAndRoomId(Long roomId);
}
