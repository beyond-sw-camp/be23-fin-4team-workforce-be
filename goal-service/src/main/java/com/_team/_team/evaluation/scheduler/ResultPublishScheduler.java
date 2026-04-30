package com._team._team.evaluation.scheduler;

import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.EvaluationSeason;
import com._team._team.evaluation.domain.enums.EvaluationStage;
import com._team._team.evaluation.repository.EvaluationResponseRepository;
import com._team._team.evaluation.repository.EvaluationSeasonRepository;
import com._team._team.dto.NotificationMessage;
import com._team._team.notification.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * ResultPublishScheduler
 *
 *  resultPublishDate 도래한 시즌을 발견 → markResultsPublished 호출 + 본인들에게 알림.
 *  매일 09:00 실행.
 */
@Component
public class ResultPublishScheduler {

    private static final Logger log = LoggerFactory.getLogger(ResultPublishScheduler.class);

    private final EvaluationSeasonRepository seasonRepository;
    private final EvaluationResponseRepository responseRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ResultPublishScheduler(EvaluationSeasonRepository seasonRepository,
                                  EvaluationResponseRepository responseRepository,
                                  ApplicationEventPublisher eventPublisher) {
        this.seasonRepository = seasonRepository;
        this.responseRepository = responseRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void runDaily() {
        LocalDate today = LocalDate.now();
        List<EvaluationSeason> due = seasonRepository.findAll().stream()
                .filter(s -> s.getResultPublishDate() != null)
                .filter(s -> !s.isResultsPublished())
                .filter(s -> !today.isBefore(s.getResultPublishDate()))
                .toList();

        for (EvaluationSeason s : due) {
            s.markResultsPublished();
            int notified = notifyConfirmed(s);
            log.info("[ResultPublish] season={} published, notified={}", s.getSeasonId(), notified);
        }
    }

    private int notifyConfirmed(EvaluationSeason season) {
        List<EvaluationResponse> confirmed = responseRepository
                .findByCompanyIdAndStageOrderByCreatedAtDesc(
                        season.getCompanyId(), EvaluationStage.CONFIRMED);

        List<EvaluationResponse> targets = confirmed.stream()
                .filter(r -> r.getGroup() != null
                          && r.getGroup().getSeason() != null
                          && r.getGroup().getSeason().getSeasonId().equals(season.getSeasonId()))
                .toList();

        for (EvaluationResponse r : targets) {
            eventPublisher.publishEvent(NotificationMessage.builder()
                    .receiverId(r.getTargetMemberId())
                    .senderId(null)
                    .notificationType(NotificationType.GOAL_EVALUATED)
                    .content("평가 결과가 공개되었습니다. 확정된 등급 및 점수를 확인하세요.")
                    .targetId(season.getSeasonId())
                    .targetType("EVALUATION_SEASON")
                    .build());
        }
        return targets.size();
    }
}
