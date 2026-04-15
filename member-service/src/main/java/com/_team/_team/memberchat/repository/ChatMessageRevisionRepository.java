package com._team._team.memberchat.repository;

import com._team._team.memberchat.domain.ChatMessageRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRevisionRepository extends JpaRepository<ChatMessageRevision, Long> {
    List<ChatMessageRevision> findByMessageIdOrderByIdAsc(Long messageId);
}
