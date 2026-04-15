package com._team._team.memberchat.service;

import com._team._team.memberchat.domain.enums.AuditAction;
import com._team._team.memberchat.domain.ChatAuditLog;
import com._team._team.memberchat.repository.ChatAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final ChatAuditLogRepository repo;

    /**
     * 감사로그는 비즈니스 트랜잭션과 분리(REQUIRES_NEW)되어야 함.
     * 그래야 메시지 저장 롤백 시에도 감사 이벤트가 남거나, 반대로 감사 실패가 비즈니스에 영향을 주지 않도록 설계.
     * 여기서는 REQUIRES_NEW + async 조합으로 비차단.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorId, AuditAction action, String targetType, Long targetId) {
        repo.save(ChatAuditLog.builder()
                .actorId(actorId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .build());
    }
}
