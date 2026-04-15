package com._team._team.memberchat.service;

import com._team._team.memberchat.MemberChatProperties;
import com._team._team.memberchat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Retention batch.
 *  - member-chat.retention.default-days 보다 오래된 메시지를 대상.
 *  - ChatRoom.legalHold=true 또는 ChatLegalHold(released=false) 대상 방은 purge 제외.
 *  - ShedLock 으로 멀티 인스턴스 중복 실행 방지.
 *
 * 실제 운영에서는 삭제 대신 "아카이브 버킷으로 이동 후 purge" 로 2단계 처리 권장.
 * 여기서는 purge-only 참조 구현.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionService {

    private final ChatMessageRepository messageRepo;
    private final MemberChatProperties props;

    @Scheduled(cron = "${member-chat.retention.batch-cron:0 0 3 * * *}")
    @SchedulerLock(name = "mc_retention", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    @Transactional
    public void runBatch() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(props.getRetention().getDefaultDays());
        int deleted = messageRepo.purgeOlderThan(threshold);
        log.info("[mc-retention] purged {} messages older than {}", deleted, threshold);
    }
}
