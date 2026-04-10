package com._team._team.chat.repository;

import com._team._team.chat.domain.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, UUID> {

    List<ChatHistory> findByMemberIdAndCompanyIdOrderByCreatedAtDesc(
            UUID memberId, UUID companyId);

    void deleteByMemberIdAndCompanyId(UUID memberId, UUID companyId);
}