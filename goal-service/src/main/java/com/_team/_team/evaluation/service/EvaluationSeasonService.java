package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.EvaluationSeason;
import com._team._team.evaluation.domain.EvaluationGroup;
import com._team._team.evaluation.domain.enums.EvalType;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import com._team._team.evaluation.domain.enums.SeasonPhase;
import com._team._team.evaluation.domain.enums.SeasonStatus;
import com._team._team.evaluation.dto.reqdto.SeasonCreateReqDto;
import com._team._team.evaluation.dto.reqdto.SeasonUpdateReqDto;
import com._team._team.evaluation.dto.resdto.GoalSnapshotDto;
import com._team._team.evaluation.dto.resdto.SeasonResDto;
import com._team._team.evaluation.repository.EvaluationGroupRepository;
import com._team._team.evaluation.repository.EvaluationResponseRepository;
import com._team._team.evaluation.repository.EvaluationSeasonRepository;
import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.repository.GoalRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvaluationSeasonService {

    private final EvaluationSeasonRepository seasonRepository;
    private final EvaluationGroupRepository groupRepository;
    private final EvaluationResponseRepository responseRepository;
    private final GoalRepository goalRepository;
    private final ObjectMapper objectMapper;

    public EvaluationSeasonService(EvaluationSeasonRepository seasonRepository,
                                    EvaluationGroupRepository groupRepository,
                                    EvaluationResponseRepository responseRepository,
                                    GoalRepository goalRepository,
                                    ObjectMapper objectMapper) {
        this.seasonRepository = seasonRepository;
        this.groupRepository = groupRepository;
        this.responseRepository = responseRepository;
        this.goalRepository = goalRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SeasonResDto> listSeasons(UUID companyId) {
        return seasonRepository.findByCompanyIdOrderByStartDateDesc(companyId)
                .stream().map(SeasonResDto::from).collect(Collectors.toList());
    }

    @Transactional
    public SeasonResDto createSeason(UUID companyId, SeasonCreateReqDto dto) {
        EvaluationSeason season = EvaluationSeason.builder()
                .companyId(companyId)
                .name(dto.getName())
                .type(dto.getType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .resultPublishDate(dto.getResultPublishDate())
                .scheduleJson(dto.getScheduleJson())
                .build();
        return SeasonResDto.from(seasonRepository.save(season));
    }

    @Transactional(readOnly = true)
    public SeasonResDto getSeason(UUID seasonId, UUID companyId) {
        EvaluationSeason season = findSeasonOrThrow(seasonId, companyId);
        return SeasonResDto.from(season);
    }

    @Transactional
    public SeasonResDto updateSeason(UUID seasonId, UUID companyId, SeasonUpdateReqDto dto) {
        EvaluationSeason season = findSeasonOrThrow(seasonId, companyId);
        if (season.getStatus() != SeasonStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "초안 상태의 시즌만 수정할 수 있습니다.");
        }
        season.update(
                dto.getName() != null ? dto.getName() : season.getName(),
                dto.getType() != null ? dto.getType() : season.getType(),
                dto.getStartDate() != null ? dto.getStartDate() : season.getStartDate(),
                dto.getEndDate() != null ? dto.getEndDate() : season.getEndDate(),
                dto.getResultPublishDate() != null ? dto.getResultPublishDate() : season.getResultPublishDate(),
                dto.getScheduleJson() != null ? dto.getScheduleJson() : season.getScheduleJson()
        );
        return SeasonResDto.from(season);
    }

    @Transactional
    public SeasonResDto startSeason(UUID seasonId, UUID companyId) {
        EvaluationSeason season = findSeasonOrThrow(seasonId, companyId);
        if (season.getStatus() != SeasonStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "초안 상태의 시즌만 시작할 수 있습니다.");
        }
        season.activate();

        // 그룹 내 evaluatorMaps 기반으로 EvaluationResponse 일괄 생성
        // 검증 강화: targetMemberId == evaluatorId 인 경우는 SELF 타입 외에는 건너뛴다.
        List<EvaluationGroup> groups = groupRepository.findBySeason_SeasonId(seasonId);
        List<EvaluationResponse> responses = new ArrayList<>();
        // 같은 시즌 시작 시점에는 대상자당 스냅샷을 한 번만 계산해서 재사용한다.
        Map<UUID, String> snapshotCache = new HashMap<>();
        Map<UUID, String> targetIdsCache = new HashMap<>();
        String takenAtIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        for (EvaluationGroup group : groups) {
            if (group.getEvaluatorMapsJson() == null) continue;
            List<Map<String, String>> maps;
            try {
                maps = objectMapper.readValue(group.getEvaluatorMapsJson(), new TypeReference<>() {});
            } catch (Exception e) {
                // 개별 그룹의 JSON 이상은 400 Bad Request 로 반환 — 클라이언트에 원인 전달
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "평가자 매핑 JSON 이 올바르지 않습니다. 그룹: " + group.getName());
            }
            for (Map<String, String> map : maps) {
                try {
                    UUID targetMemberId = UUID.fromString(map.get("targetMemberId"));
                    UUID evaluatorId = UUID.fromString(map.get("evaluatorId"));
                    EvalType evalType = EvalType.valueOf(map.get("evaluationType"));
                    // SELF 가 아닌데 자기 자신을 평가하는 매핑은 잘못된 것으로 판단하여 제외
                    if (evalType != EvalType.SELF && targetMemberId.equals(evaluatorId)) {
                        continue;
                    }

                    // 평가 대상자의 "평가 가능한" 목표들을 시즌 시작 시점 스냅샷으로 캡처
                    // - ACTIVE/COMPLETED 만 대상 (DRAFT/CANCELLED 는 제외)
                    // - 별도 엔티티 없이 JSON 컬럼으로만 기록
                    String goalSnapshotJson = snapshotCache.computeIfAbsent(targetMemberId,
                            tid -> buildGoalSnapshotJson(companyId, tid, takenAtIso));
                    String targetGoalIdsJson = targetIdsCache.computeIfAbsent(targetMemberId,
                            tid -> buildTargetGoalIdsJson(companyId, tid));

                    responses.add(EvaluationResponse.builder()
                            .companyId(companyId)
                            .group(group)
                            .targetMemberId(targetMemberId)
                            .evaluatorId(evaluatorId)
                            .evaluationType(evalType)
                            .goalSnapshotJson(goalSnapshotJson)
                            .targetGoalIdsJson(targetGoalIdsJson)
                            .build());
                } catch (IllegalArgumentException ignore) {
                    // 잘못된 UUID/enum 값은 해당 매핑만 건너뛴다 (다른 그룹까지 막지 않도록)
                }
            }
        }
        if (!responses.isEmpty()) {
            responseRepository.saveAll(responses);
        }

        return SeasonResDto.from(season);
    }

    /**
     * 평가 대상자의 평가 가능 목표를 시즌 시작 시점 스냅샷으로 직렬화한다.
     * 별도 엔티티 없이 JSON 컬럼만으로 이력 추적이 가능하도록 만든 핵심 로직.
     */
    private String buildGoalSnapshotJson(UUID companyId, UUID targetMemberId, String takenAtIso) {
        try {
            List<Goal> goals = goalRepository.findByCompanyIdAndOwnerId(companyId, targetMemberId);
            List<GoalSnapshotDto> snapshots = goals.stream()
                    .filter(g -> g.getStatus() == GoalStatus.ACTIVE || g.getStatus() == GoalStatus.COMPLETED)
                    .map(g -> GoalSnapshotDto.from(g, takenAtIso))
                    .collect(Collectors.toList());
            if (snapshots.isEmpty()) {
                return null;
            }
            return objectMapper.writeValueAsString(snapshots);
        } catch (Exception e) {
            // 스냅샷 실패는 치명적이지 않음 — 응답 생성 자체는 계속 진행
            return null;
        }
    }

    private String buildTargetGoalIdsJson(UUID companyId, UUID targetMemberId) {
        try {
            List<UUID> goalIds = goalRepository.findByCompanyIdAndOwnerId(companyId, targetMemberId).stream()
                    .filter(g -> g.getStatus() == GoalStatus.ACTIVE || g.getStatus() == GoalStatus.COMPLETED)
                    .map(Goal::getGoalId)
                    .collect(Collectors.toList());
            if (goalIds.isEmpty()) {
                return null;
            }
            return objectMapper.writeValueAsString(goalIds);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 시즌 시작 시점이 아닌, 호출 시점 기준으로 대상자 목표 스냅샷을 만든다.
     * {@code materializeMissingResponsesForGroup} 등 늦게 생성되는 {@link EvaluationResponse}에 부착할 때 사용한다.
     * 규칙은 {@link #startSeason} 과 동일하게 ACTIVE/COMPLETED 목표만 포함한다.
     */
    public record MemberGoalSnapshots(String goalSnapshotJson, String targetGoalIdsJson) {}

    @Transactional(readOnly = true)
    public MemberGoalSnapshots captureMemberGoalSnapshotsAtNow(UUID companyId, UUID targetMemberId) {
        String takenAtIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new MemberGoalSnapshots(
                buildGoalSnapshotJson(companyId, targetMemberId, takenAtIso),
                buildTargetGoalIdsJson(companyId, targetMemberId));
    }

    @Transactional
    public SeasonResDto closeSeason(UUID seasonId, UUID companyId) {
        EvaluationSeason season = findSeasonOrThrow(seasonId, companyId);
        if (season.getStatus() != SeasonStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "진행 중인 시즌만 종료할 수 있습니다.");
        }
        // 결과 공개 이전에는 종료할 수 없음 — 피드백 미팅 자동 생성이 제대로 동작하려면
        // 반드시 PUBLISHED 페이즈까지 도달해야 한다.
        if (season.getPhase() != SeasonPhase.PUBLISHED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "결과 공개(PUBLISHED) 이전에는 시즌을 종료할 수 없습니다. 현재 phase: " + season.getPhase());
        }
        // 안전장치: 미제출 응답이 남아 있으면 종료 차단
        long notSubmitted = responseRepository.findByGroup_Season_SeasonId(seasonId).stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .filter(r -> r.getStatus() != EvaluationStatus.SUBMITTED)
                .count();
        if (notSubmitted > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "제출되지 않은 평가 응답이 " + notSubmitted + "건 있어 시즌을 종료할 수 없습니다.");
        }
        season.close();
        return SeasonResDto.from(season);
    }

    /**
     * Phase 전이: ACTIVE 상태인 시즌에 한해 관리자가 페이즈를 진행시킬 수 있습니다.
     * 현재 phase에 해당하는 평가 유형의 응답이 모두 제출(SUBMITTED)되었는지 사전 검증합니다.
     */
    @Transactional
    public SeasonResDto transitionPhase(UUID seasonId, UUID companyId, SeasonPhase next) {
        EvaluationSeason season = findSeasonOrThrow(seasonId, companyId);

        // 현재 phase에 대응하는 평가 유형의 미제출 응답이 있으면 전이 차단
        EvalType requiredType = mapPhaseToEvalType(season.getPhase());
        if (requiredType != null) {
            long notSubmitted = responseRepository.findByGroup_Season_SeasonId(seasonId).stream()
                    .filter(r -> r.getCompanyId().equals(companyId))
                    .filter(r -> r.getEvaluationType() == requiredType)
                    .filter(r -> r.getStatus() != EvaluationStatus.SUBMITTED)
                    .count();
            if (notSubmitted > 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        requiredType.name() + " 유형의 미제출 응답이 " + notSubmitted
                                + "건 있어 phase 를 전이할 수 없습니다.");
            }
        }

        season.transitionTo(next);
        return SeasonResDto.from(season);
    }

    /**
     * 평가 phase 에 대응하는 EvalType 을 반환합니다.
     * 대응 관계가 없는 phase (NOT_STARTED, CALIBRATION 등) 는 null 을 반환합니다.
     */
    private static EvalType mapPhaseToEvalType(SeasonPhase phase) {
        if (phase == null) return null;
        return switch (phase) {
            case SELF_EVAL -> EvalType.SELF;
            case PEER_EVAL -> EvalType.PEER;
            case UPWARD_EVAL -> EvalType.UPWARD;
            case DOWNWARD_EVAL -> EvalType.DOWNWARD;
            default -> null;
        };
    }

    private EvaluationSeason findSeasonOrThrow(UUID seasonId, UUID companyId) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다.");
        }
        return season;
    }
}