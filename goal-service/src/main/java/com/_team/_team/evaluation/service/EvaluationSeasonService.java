package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.EvaluationSeason;
import com._team._team.evaluation.domain.EvaluationGroup;
import com._team._team.evaluation.domain.converter.EvaluatorMapping;
import com._team._team.evaluation.domain.enums.EvalType;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        List<EvaluationGroup> groups = groupRepository.findBySeason_SeasonId(seasonId);
        validateStartReadiness(groups);
        season.activate();

        // 그룹 내 evaluatorMaps 기반으로 EvaluationResponse 일괄 생성
        // 검증 강화: targetMemberId == evaluatorId 인 경우는 SELF 타입 외에는 건너뛴다.
        List<EvaluationResponse> responses = new ArrayList<>();
        // 같은 시즌 시작 시점에는 대상자당 스냅샷을 한 번만 계산해서 재사용한다.
        Map<UUID, String> snapshotCache = new HashMap<>();
        String takenAtIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // [D-3] 타입 세이프 List<EvaluatorMapping> 를 직접 순회. raw JSON 파싱 제거.
        for (EvaluationGroup group : groups) {
            List<EvaluatorMapping> maps = group.getEvaluatorMaps();
            if (maps == null || maps.isEmpty()) continue;
            for (EvaluatorMapping em : maps) {
                UUID targetMemberId = em.getTargetMemberId();
                UUID evaluatorId = em.getEvaluatorId();
                EvalType evalType = em.getEvaluationType();
                if (targetMemberId == null || evaluatorId == null || evalType == null) continue;
                // SELF 가 아닌데 자기 자신을 평가하는 매핑은 잘못된 것으로 판단하여 제외
                if (evalType != EvalType.SELF && targetMemberId.equals(evaluatorId)) {
                    continue;
                }

                // 평가 대상자의 "평가 가능한" 목표들을 시즌 시작 시점 스냅샷으로 캡처
                // - ACTIVE/COMPLETED 만 대상 (DRAFT/CANCELLED 는 제외)
                // - 별도 엔티티 없이 JSON 컬럼으로만 기록
                // [D-5] targetGoalIdsJson 제거 — goalSnapshotJson 안에 goalId 가 이미 포함됨
                String goalSnapshotJson = snapshotCache.computeIfAbsent(targetMemberId,
                        tid -> buildGoalSnapshotJson(companyId, tid, takenAtIso));

                responses.add(EvaluationResponse.builder()
                        .companyId(companyId)
                        .group(group)
                        .targetMemberId(targetMemberId)
                        .evaluatorId(evaluatorId)
                        .evaluationType(evalType)
                        .goalSnapshotJson(goalSnapshotJson)
                        .build());
            }
        }
        if (!responses.isEmpty()) {
            responseRepository.saveAll(responses);
        }

        return SeasonResDto.from(season);
    }

    private void validateStartReadiness(List<EvaluationGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "그룹이 없어 시즌을 시작할 수 없습니다. 최소 1개 그룹을 생성해 주세요.");
        }
        for (EvaluationGroup group : groups) {
            List<UUID> targets = parseTargetIds(group.getTargetMemberIdsJson());
            Set<EvalType> evalTypes = parseEvalTypes(group.getEvaluationTypesJson());
            List<EvaluatorMapping> maps = group.getEvaluatorMaps() != null ? group.getEvaluatorMaps() : List.of();

            if (targets.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "그룹 '" + group.getName() + "'에 대상 인원이 없어 시즌을 시작할 수 없습니다.");
            }
            if (evalTypes.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "그룹 '" + group.getName() + "'에 평가 유형이 없어 시즌을 시작할 수 없습니다.");
            }
            if (maps.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "그룹 '" + group.getName() + "'에 지정된 평가자가 없어 시즌을 시작할 수 없습니다.");
            }

            for (UUID targetId : targets) {
                for (EvalType evalType : evalTypes) {
                    boolean exists = maps.stream().anyMatch(m ->
                            m != null
                                    && targetId.equals(m.getTargetMemberId())
                                    && evalType == m.getEvaluationType()
                                    && m.getEvaluatorId() != null
                                    && (evalType == EvalType.SELF
                                    ? targetId.equals(m.getEvaluatorId())
                                    : !targetId.equals(m.getEvaluatorId()))
                    );
                    if (!exists) {
                        throw new BusinessException(
                                HttpStatus.BAD_REQUEST,
                                "그룹 '" + group.getName() + "'의 평가자 지정이 완료되지 않았습니다. (대상자별 평가 유형 누락)");
                    }
                }
            }
        }
    }

    private List<UUID> parseTargetIds(String targetMemberIdsJson) {
        if (targetMemberIdsJson == null || targetMemberIdsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(targetMemberIdsJson, new TypeReference<List<UUID>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Set<EvalType> parseEvalTypes(String evaluationTypesJson) {
        if (evaluationTypesJson == null || evaluationTypesJson.isBlank()) return Set.of();
        try {
            List<String> raw = objectMapper.readValue(evaluationTypesJson, new TypeReference<List<String>>() {});
            EnumSet<EvalType> out = EnumSet.noneOf(EvalType.class);
            for (String type : raw) {
                if (type == null || type.isBlank()) continue;
                try {
                    out.add(EvalType.valueOf(type.trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {
                    // ignore invalid enum token
                }
            }
            return out;
        } catch (Exception ignored) {
            return Set.of();
        }
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

    // [D-5] buildTargetGoalIdsJson 제거. goalSnapshotJson 이 ID 를 이미 포함.

    /**
     * 시즌 시작 시점이 아닌, 호출 시점 기준으로 대상자 목표 스냅샷을 만든다.
     * {@code materializeMissingResponsesForGroup} 등 늦게 생성되는 {@link EvaluationResponse}에 부착할 때 사용한다.
     * 규칙은 {@link #startSeason} 과 동일하게 ACTIVE/COMPLETED 목표만 포함한다.
     */
    public record MemberGoalSnapshots(String goalSnapshotJson) {}

    @Transactional(readOnly = true)
    public MemberGoalSnapshots captureMemberGoalSnapshotsAtNow(UUID companyId, UUID targetMemberId) {
        String takenAtIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return new MemberGoalSnapshots(
                buildGoalSnapshotJson(companyId, targetMemberId, takenAtIso));
    }

    @Transactional
    public SeasonResDto closeSeason(UUID seasonId, UUID companyId) {
        EvaluationSeason season = findSeasonOrThrow(seasonId, companyId);
        if (season.getStatus() != SeasonStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "진행 중인 시즌만 종료할 수 있습니다.");
        }
        // 종료와 결과 공개(PUBLISHED)는 별개의 워크플로다.
        // 관리자는 결과 공개 없이도 시즌을 행정적으로 종결할 수 있어야 한다.
        // (예: 캘리브레이션 단계까지만 마치고 내부 종결 후 나중에 결과 공개 등)
        // 단, 미제출 응답이 있으면 리마인드/재개방으로 먼저 정리하도록 차단한다.
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

    // [REMOVED] transitionPhase / mapPhaseToEvalType
    // phase 기반 워크플로가 제거되었습니다. 시즌은 status(DRAFT/ACTIVE/CLOSED) 와
    // resultsPublishedAt 만으로 관리되며, 모든 평가 유형은 status=ACTIVE 상태에서 병렬로 진행됩니다.

    private EvaluationSeason findSeasonOrThrow(UUID seasonId, UUID companyId) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다.");
        }
        return season;
    }
}