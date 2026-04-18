package com._team._team.memberchat.repository;

import com._team._team.memberchat.domain.ChatAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatAuditLogRepository extends JpaRepository<ChatAuditLog, Long> {
}
