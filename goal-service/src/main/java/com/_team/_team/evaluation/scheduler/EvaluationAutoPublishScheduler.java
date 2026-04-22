package com._team._team.evaluation.scheduler;

import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.EvaluationSeason;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import com._team._team.evaluation.domain.enums.SeasonStatus;
import com._team._team.evaluation.repository.EvaluationResponseRepository;
import com._team._team.evaluation.repository.EvaluationSeasonRepository;
import com._team._team.evaluation.service.EvaluationResponseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 평가 결과 자동 공개 스케줄러.
 *
 * 매 분마다 다음 조건을 모두 만족하는 시즌을 찾아 결과를 공개합니다:
 *  - status = ACTIVE
 *  - resultsPublishedAt == null (아직 미공개)
 *  - resultPublishDate != null && resultPublishDate <= today()
 *  - 해당 시즌의 모든 응답이 SUBMITTED
 *
 * 하나라도 미제출 응답이 있으면 공개를 보류하고 다음 주기에 다시 시도합니다.
 */
@Component
public class EvaluationAutoPublishScheduler {

    private static final Logger log = LoggerFactory.getLogger(EvaluationAutoPublishScheduler.class);

    private final EvaluationSeasonRepository seasonRepository;
    private final EvaluationResponseRepository responseRepository;
    private final EvaluationResponseService responseService;

    public EvaluationAutoPublishScheduler(EvaluationSeasonRepository seasonRepository,
                                           EvaluationResponseRepository responseRepository,
                                           EvaluationResponseService responseService) {
        this.seasonRepository = seasonRepository;
        this.responseRepository = responseRepository;
        this.responseService = responseService;
    }

    /**
     * 60초마다 실행. 다수 인스턴스 환경에서는 shedlock 등 분산 락 도입 검토.
     * 현재는 단일 인스턴스 가정 + 개별 시즌별 트랜잭션 격리로 동시성 방어.
     */
    @Scheduled(fixedDelay = 60_000L, initialDelay = 30_000L)
    public void autoPublishDueSeasons() {
        LocalDate today = LocalDate.now();
        List<EvaluationSeason> candidates = seasonRepository.findAll().stream()
                .filter(s -> s.getStatus() == SeasonStatus.ACTIVE)
                .filter(s -> s.getResultsPublishedAt() == null)
                .filter(s -> s.getResultPublishDate() != null)
                .filter(s -> !s.getResultPublishDate().isAfter(today))
                .toList();

        if (candidates.isEmpty()) {
            return;
        }

        for (EvaluationSeason season : candidates) {
            try {
                if (hasUnsubmittedResponses(season)) {
                    log.info("[auto-publish] skip season={} ({}) — 미제출 응답 존재",
                            season.getSeasonId(), season.getName());
                    continue;
                }
                responseService.publishResults(season.getSeasonId(), season.getCompanyId());
                log.info("[auto-publish] published season={} ({})",
                        season.getSeasonId(), season.getName());
            } catch (Exception e) {
                log.warn("[auto-publish] failed season={} ({}): {}",
                        season.getSeasonId(), season.getName(), e.getMessage());
            }
        }
    }

    private boolean hasUnsubmittedResponses(EvaluationSeason season) {
        List<EvaluationResponse> all = responseRepository.findByGroup_Season_SeasonId(season.getSeasonId());
        return all.stream()
                .filter(r -> r.getCompanyId().equals(season.getCompanyId()))
                .anyMatch(r -> r.getStatus() != EvaluationStatus.SUBMITTED);
    }
}
