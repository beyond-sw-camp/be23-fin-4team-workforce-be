package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.CalibrationHistory;
import com._team._team.evaluation.domain.EvaluationGroup;
import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.EvaluationSeason;
import com._team._team.evaluation.domain.enums.EvalType;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import com._team._team.evaluation.domain.enums.SeasonStatus;
import com._team._team.evaluation.dto.reqdto.CalibrationAdjustReqDto;
import com._team._team.evaluation.dto.reqdto.ResponseSaveReqDto;
import com._team._team.evaluation.dto.resdto.GoalSnapshotDto;
import com._team._team.evaluation.dto.resdto.GoalSummaryCardResDto;
import com._team._team.evaluation.dto.resdto.ResponseResDto;
import com._team._team.evaluation.repository.CalibrationHistoryRepository;
import com._team._team.evaluation.repository.EvaluationResponseRepository;
import com._team._team.evaluation.repository.EvaluationSeasonRepository;
import com._team._team.evaluation.service.scoring.ScoreBreakdown;
import com._team._team.evaluation.service.scoring.SectionScorer;
import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.repository.GoalRepository;
import com._team._team.goal.service.GoalNotificationService;
import com._team._team.meeting.service.MeetingRecordService;
import com._team._team.notification.NotificationType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EvaluationResponseService {

    private final EvaluationResponseRepository responseRepository;
    private final EvaluationSeasonRepository seasonRepository;
    private final CalibrationHistoryRepository calibrationHistoryRepository;
    private final GoalRepository goalRepository;
    private final ObjectMapper objectMapper;
    private final MeetingRecordService meetingRecordService;
    private final EvaluationSeasonService evaluationSeasonService;
    private final com._team._team.evaluation.feignclients.MemberServiceClient memberServiceClient;
    private final SectionScorer sectionScorer;
    private final GoalNotificationService notificationService;

    public EvaluationResponseService(EvaluationResponseRepository responseRepository,
                                      EvaluationSeasonRepository seasonRepository,
                                      CalibrationHistoryRepository calibrationHistoryRepository,
                                      GoalRepository goalRepository,
                                      ObjectMapper objectMapper,
                                      MeetingRecordService meetingRecordService,
                                      EvaluationSeasonService evaluationSeasonService,
                                      com._team._team.evaluation.feignclients.MemberServiceClient memberServiceClient,
                                      SectionScorer sectionScorer,
                                      GoalNotificationService notificationService) {
        this.responseRepository = responseRepository;
        this.seasonRepository = seasonRepository;
        this.calibrationHistoryRepository = calibrationHistoryRepository;
        this.goalRepository = goalRepository;
        this.objectMapper = objectMapper;
        this.meetingRecordService = meetingRecordService;
        this.evaluationSeasonService = evaluationSeasonService;
        this.memberServiceClient = memberServiceClient;
        this.sectionScorer = sectionScorer;
        this.notificationService = notificationService;
    }

    /**
     * 응답 리스트를 DTO 로 매핑하면서 대상자/평가자 이름·소속·프로필을 일괄 주입한다.
     * N+1 방지를 위해 Feign 호출 1회로 모든 프로필을 가져온다.
     * member-service 장애 시 이름은 null 로 두고 계속 진행.
     */
    private List<ResponseResDto> mapResponsesWithNames(List<EvaluationResponse> responses) {
        if (responses.isEmpty()) return java.util.Collections.emptyList();
        java.util.Set<UUID> ids = new java.util.HashSet<>();
        for (EvaluationResponse r : responses) {
            if (r.getTargetMemberId() != null) ids.add(r.getTargetMemberId());
            if (r.getEvaluatorId() != null) ids.add(r.getEvaluatorId());
        }
        java.util.Map<UUID, com._team._team.evaluation.feignclients.dto.MemberMinimalProfileDto> profiles;
        try {
            profiles = memberServiceClient.getProfilesByIds(new java.util.ArrayList<>(ids));
        } catch (Exception e) {
            profiles = java.util.Collections.emptyMap();
        }
        List<ResponseResDto> out = new java.util.ArrayList<>(responses.size());
        for (EvaluationResponse r : responses) {
            ResponseResDto dto = ResponseResDto.from(r);
            var target = profiles.get(r.getTargetMemberId());
            if (target != null) {
                dto.setTargetMemberName(target.getName());
                dto.setTargetMemberDepartment(target.getDepartment());
                dto.setTargetMemberProfileUrl(target.getProfileUrl());
            }
            var evaluator = profiles.get(r.getEvaluatorId());
            if (evaluator != null) {
                dto.setEvaluatorName(evaluator.getName());
                dto.setEvaluatorDepartment(evaluator.getDepartment());
                dto.setEvaluatorProfileUrl(evaluator.getProfileUrl());
            }
            dto.setScoreBreakdown(computeBreakdownForResponse(r));
            out.add(dto);
        }
        return out;
    }

    /**
     * [L-1 / Phase D] 응답의 섹션별 채점 breakdown 을 재계산한다.
     * 저장하지 않고 매 조회마다 design·answers·snapshot 으로부터 만들어낸다.
     * design 이 없거나 섹션이 없으면 null.
     *
     * PEER_FEEDBACK 섹션이 하나라도 존재하면, 같은 시즌·같은 대상자의 다른 응답들 answersJson 을
     * 모아 질문별 동료 평균을 계산해 scorer 에 넘긴다. (자기 자신 응답은 제외)
     */
    private ScoreBreakdown computeBreakdownForResponse(EvaluationResponse r) {
        var group = r.getGroup();
        if (group == null || group.getDesign() == null) return null;
        var design = group.getDesign();
        var sections = design.getSections();
        if (sections == null || sections.isEmpty()) return null;

        java.util.Map<String, java.util.List<BigDecimal>> peerScores = null;
        boolean hasPeerSection = sections.stream()
                .anyMatch(s -> s.resolveType() == com._team._team.evaluation.domain.enums.SectionType.PEER_FEEDBACK);
        if (hasPeerSection && group.getSeason() != null && r.getTargetMemberId() != null) {
            UUID seasonId = group.getSeason().getSeasonId();
            List<String> peerAnswersJson = responseRepository
                    .findByGroup_Season_SeasonIdAndTargetMemberId(seasonId, r.getTargetMemberId())
                    .stream()
                    .filter(other -> !other.getResponseId().equals(r.getResponseId()))
                    .filter(other -> other.getEvaluationType() == EvalType.PEER)
                    .filter(other -> other.getStatus() == EvaluationStatus.SUBMITTED)
                    .map(EvaluationResponse::getAnswersJson)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            peerScores = sectionScorer.aggregatePeerScores(peerAnswersJson);
        }

        return sectionScorer.score(
                sections,
                r.getAnswersJson(),
                sectionScorer.parseGoalSnapshots(r.getGoalSnapshotJson()),
                peerScores);
    }

    /**
     * 단건 응답 DTO에 대상자/평가자 프로필(이름·소속·프로필 URL)을 주입한다.
     * member-service 조회 실패 시 기본 DTO를 그대로 반환한다.
     */
    private ResponseResDto mapResponseWithNames(EvaluationResponse response) {
        ResponseResDto dto = ResponseResDto.from(response);
        java.util.Set<UUID> ids = new java.util.HashSet<>();
        if (response.getTargetMemberId() != null) ids.add(response.getTargetMemberId());
        if (response.getEvaluatorId() != null) ids.add(response.getEvaluatorId());
        if (ids.isEmpty()) return dto;
        try {
            java.util.Map<UUID, com._team._team.evaluation.feignclients.dto.MemberMinimalProfileDto> profiles =
                    memberServiceClient.getProfilesByIds(new java.util.ArrayList<>(ids));
            var target = profiles.get(response.getTargetMemberId());
            if (target != null) {
                dto.setTargetMemberName(target.getName());
                dto.setTargetMemberDepartment(target.getDepartment());
                dto.setTargetMemberProfileUrl(target.getProfileUrl());
            }
            var evaluator = profiles.get(response.getEvaluatorId());
            if (evaluator != null) {
                dto.setEvaluatorName(evaluator.getName());
                dto.setEvaluatorDepartment(evaluator.getDepartment());
                dto.setEvaluatorProfileUrl(evaluator.getProfileUrl());
            }
        } catch (Exception ignored) {
            // member-service 장애 시 이름 주입을 생략하고 기본 DTO 반환
        }
        dto.setScoreBreakdown(computeBreakdownForResponse(response));
        return dto;
    }

    @Transactional(readOnly = true)
    public List<ResponseResDto> listMyResponses(UUID evaluatorId) {
        return mapResponsesWithNames(responseRepository.findByEvaluatorId(evaluatorId));
    }

    /**
     * 본인(피평가자)이 받은 평가 응답 목록.
     * 결과가 공개된(PUBLISHED) 시즌에 한해서만 반환하여, 결과 공개 이전의 평가 내용이 유출되지 않도록 한다.
     * 일반 사원도 접근 가능 — 컨트롤러에서 @CheckPermission 을 걸지 않는다.
     */
    @Transactional(readOnly = true)
    public List<ResponseResDto> listMyReceivedResults(UUID memberId, UUID companyId) {
        List<EvaluationResponse> list = responseRepository.findByTargetMemberId(memberId).stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .filter(r -> r.getGroup() != null && r.getGroup().getSeason() != null)
                .filter(r -> r.getGroup().getSeason().isResultsPublished())
                .collect(Collectors.toList());
        return mapResponsesWithNames(list);
    }

    /**
     * 본인(피평가자)이 받은 특정 시즌의 평가 응답 목록.
     * 결과가 공개된(PUBLISHED) 시즌만 조회 허용 — 그렇지 않으면 400.
     */
    @Transactional(readOnly = true)
    public List<ResponseResDto> listMySeasonResult(UUID seasonId, UUID memberId, UUID companyId) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다.");
        }
        if (!season.isResultsPublished()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "아직 결과가 공개되지 않은 시즌입니다.");
        }
        List<EvaluationResponse> list = responseRepository.findByGroup_Season_SeasonId(seasonId).stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .filter(r -> r.getTargetMemberId().equals(memberId))
                .collect(Collectors.toList());
        return mapResponsesWithNames(list);
    }

    /**
     * 시즌 시작 이후에 평가자 매핑이 추가·변경된 경우, 기존에는 JSON만 갱신되고 {@link EvaluationResponse} 행이
     * 생기지 않아 평가자 목록에 안 보이던 문제를 해소합니다. (startSeason 시점에만 생성되던 동작 보완)
     */
    @Transactional
    public void materializeMissingResponsesForGroup(EvaluationGroup group) {
        // [D-3] 타입 세이프 List<EvaluatorMapping> 직접 순회. raw JSON 파싱 제거.
        List<com._team._team.evaluation.domain.converter.EvaluatorMapping> maps = group.getEvaluatorMaps();
        if (maps == null || maps.isEmpty()) return;

        UUID companyId = group.getCompanyId();
        UUID groupId = group.getGroupId();
        List<EvaluationResponse> toSave = new ArrayList<>();
        for (com._team._team.evaluation.domain.converter.EvaluatorMapping em : maps) {
            UUID targetMemberId = em.getTargetMemberId();
            UUID evaluatorId = em.getEvaluatorId();
            EvalType evalType = em.getEvaluationType();
            if (targetMemberId == null || evaluatorId == null || evalType == null) continue;
            if (evalType != EvalType.SELF && targetMemberId.equals(evaluatorId)) {
                continue;
            }
            if (responseRepository.existsByGroup_GroupIdAndTargetMemberIdAndEvaluatorIdAndEvaluationType(
                    groupId, targetMemberId, evaluatorId, evalType)) {
                continue;
            }
            EvaluationSeasonService.MemberGoalSnapshots snaps =
                    evaluationSeasonService.captureMemberGoalSnapshotsAtNow(companyId, targetMemberId);
            toSave.add(EvaluationResponse.builder()
                    .companyId(companyId)
                    .group(group)
                    .targetMemberId(targetMemberId)
                    .evaluatorId(evaluatorId)
                    .evaluationType(evalType)
                    .goalSnapshotJson(snaps.goalSnapshotJson())
                    .build());
        }
        if (!toSave.isEmpty()) {
            responseRepository.saveAll(toSave);
        }
    }

    @Transactional(readOnly = true)
    public ResponseResDto getResponse(UUID responseId, UUID requesterId) {
        EvaluationResponse response = responseRepository.findById(responseId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다."));
        // 컨트롤러의 @CheckPermission 을 제거한 대신, 서비스에서 본인(평가자) 여부를 검증해 타인 응답 열람을 차단한다.
        if (!response.getEvaluatorId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "자신의 평가만 조회할 수 있습니다.");
        }
        return mapResponseWithNames(response);
    }

    @Transactional
    public ResponseResDto saveResponse(UUID responseId, UUID requesterId, ResponseSaveReqDto dto) {
        EvaluationResponse response = responseRepository.findById(responseId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다."));
        if (!response.getEvaluatorId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "자신의 평가만 수정할 수 있습니다.");
        }
        if (response.getStatus() == EvaluationStatus.SUBMITTED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 제출된 평가는 수정할 수 없습니다.");
        }
        // 작성 화면에서는 평가자 본인 응답을 항상 저장 가능하게 둔다.
        // 시즌 phase 제어는 운영 UI(시즌 전이)와 관리자 정책에서 관리한다.
        response.saveAnswers(dto.getAnswersJson());
        return mapResponseWithNames(response);
    }

    @Transactional
    public ResponseResDto submitResponse(UUID responseId, UUID requesterId) {
        EvaluationResponse response = responseRepository.findById(responseId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다."));
        if (!response.getEvaluatorId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "자신의 평가만 제출할 수 있습니다.");
        }
        if (response.getStatus() == EvaluationStatus.SUBMITTED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미 제출된 평가입니다.");
        }
        // 평가자 본인 응답은 시즌 phase와 무관하게 제출 허용.
        if (response.getAnswersJson() == null || response.getAnswersJson().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "응답 내용이 비어있어 제출할 수 없습니다.");
        }
        response.submit();
        // [L-1] 제출 시점에 섹션 타입별 채점으로 정규화 점수 산출.
        //   - design.sections 에 KPI_SCORE 섹션이 있으면 goalSnapshot.achievementPctAtSnapshot 이 반영됨
        //   - 채점 불가능 상태면 normalizedScore 는 그대로 null → 후속 applyBaseline 에서 계산 가능
        applySectionScoring(response);
        return mapResponseWithNames(response);
    }

    /**
     * [L-1 / Phase D] response.group.design.sections + answersJson + goalSnapshotJson +
     * (옵션) 동료 응답 집계로 섹션 타입별 채점을 수행하고 normalizedScore 를 갱신한다.
     * design 이 없거나 채점 가능한 섹션이 없으면 조용히 스킵.
     */
    private void applySectionScoring(EvaluationResponse response) {
        ScoreBreakdown breakdown = computeBreakdownForResponse(response);
        if (breakdown != null && breakdown.getTotalScore() != null) {
            response.updateNormalizedScore(breakdown.getTotalScore());
        }
    }

    @Transactional
    public ResponseResDto reopenResponse(UUID responseId, UUID companyId) {
        EvaluationResponse response = responseRepository.findById(responseId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다."));
        if (!response.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다.");
        }
        // 결과가 공개된 뒤에는 재개방 불가
        EvaluationSeason season = response.getGroup().getSeason();
        if (season.isResultsPublished()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "결과 공개 이후에는 응답을 재개방할 수 없습니다.");
        }
        response.reopen();
        String seasonName = season.getName() != null ? season.getName() : "평가";
        notificationService.publishSystem(
                response.getEvaluatorId(),
                NotificationType.EVALUATION_REOPENED,
                "\"" + seasonName + "\" 평가 응답이 재작성 가능한 상태로 변경되었습니다.",
                responseId,
                "EVALUATION_RESPONSE");
        return mapResponseWithNames(response);
    }

    // 리마인드
    @Transactional
    public void sendReminder(UUID seasonId, UUID memberId, UUID companyId) {
        List<EvaluationResponse> pending = responseRepository.findByGroup_Season_SeasonId(seasonId)
                .stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .filter(r -> r.getEvaluatorId().equals(memberId))
                .filter(r -> r.getStatus() != EvaluationStatus.SUBMITTED)
                .collect(Collectors.toList());
        if (pending.isEmpty()) {
            return;
        }
        for (EvaluationResponse r : pending) {
            r.updateRemindedAt();
        }
        String seasonName = seasonRepository.findById(seasonId)
                .map(EvaluationSeason::getName)
                .orElse("평가");
        publishEvaluationReminder(memberId, seasonId, seasonName, pending.size());
    }

    @Transactional
    public void sendBulkReminder(UUID seasonId, UUID companyId) {
        List<EvaluationResponse> responses = responseRepository.findByGroup_Season_SeasonId(seasonId)
                .stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .filter(r -> r.getStatus() != EvaluationStatus.SUBMITTED)
                .collect(Collectors.toList());
        if (responses.isEmpty()) {
            return;
        }
        String seasonName = seasonRepository.findById(seasonId)
                .map(EvaluationSeason::getName)
                .orElse("평가");
        Map<UUID, List<EvaluationResponse>> byEvaluator = responses.stream()
                .collect(Collectors.groupingBy(EvaluationResponse::getEvaluatorId));
        for (List<EvaluationResponse> list : byEvaluator.values()) {
            for (EvaluationResponse r : list) {
                r.updateRemindedAt();
            }
        }
        for (Map.Entry<UUID, List<EvaluationResponse>> e : byEvaluator.entrySet()) {
            publishEvaluationReminder(e.getKey(), seasonId, seasonName, e.getValue().size());
        }
    }

    private void publishEvaluationReminder(UUID receiverId, UUID seasonId, String seasonName, int pendingCount) {
        notificationService.publishSystem(
                receiverId,
                NotificationType.EVALUATION_REMINDER,
                "\"" + seasonName + "\" 미제출 평가가 " + pendingCount + "건 있습니다.",
                seasonId,
                "EVALUATION_SEASON");
    }

    /**
     * 기준점(baseline) 기반 정규화 점수 재계산.
     * answersJson 에서 항목별 점수를 합산한 뒤, baselineValue 를 기준으로 0~100 스케일로 정규화합니다.
     * range = "team" 이면 시즌 전체 평균을 기준으로, "individual" 이면 baselineValue 를 직접 기준으로 사용합니다.
     */
    @Transactional
    public void applyBaseline(UUID seasonId, UUID companyId, String range, Double baselineValue) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다.");
        }
        if (!season.canCalibrate()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "진행 중이면서 결과 미공개 상태에서만 기준점을 적용할 수 있습니다.");
        }

        List<EvaluationResponse> responses = responseRepository.findByGroup_Season_SeasonId(seasonId)
                .stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .filter(r -> r.getStatus() == EvaluationStatus.SUBMITTED)
                .collect(Collectors.toList());

        if (responses.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "제출된 응답이 없어 기준점을 적용할 수 없습니다.");
        }

        // 각 응답의 원시 점수 추출 (answersJson 에서 "totalScore" 필드)
        List<BigDecimal> rawScores = responses.stream()
                .map(this::extractRawScore)
                .collect(Collectors.toList());

        // 기준점 결정
        BigDecimal baseline;
        if ("team".equalsIgnoreCase(range)) {
            // 팀 전체 평균을 기준으로 사용
            BigDecimal sum = rawScores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            baseline = sum.divide(BigDecimal.valueOf(rawScores.size()), 4, RoundingMode.HALF_UP);
        } else {
            // individual: 직접 지정한 baselineValue 사용
            baseline = baselineValue != null ? BigDecimal.valueOf(baselineValue) : BigDecimal.ZERO;
        }

        if (baseline.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "기준점이 0이면 정규화할 수 없습니다.");
        }

        // 정규화 점수 계산: (rawScore / baseline) * 50 → 기준점 = 50점
        for (int i = 0; i < responses.size(); i++) {
            EvaluationResponse response = responses.get(i);
            BigDecimal rawScore = rawScores.get(i);
            BigDecimal normalized = rawScore
                    .divide(baseline, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(50))
                    .min(BigDecimal.valueOf(100))
                    .max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            response.updateNormalizedScore(normalized);
        }
    }

    private BigDecimal extractRawScore(EvaluationResponse response) {
        if (response.getAnswersJson() == null) return BigDecimal.ZERO;
        try {
            var node = objectMapper.readTree(response.getAnswersJson());
            if (node.has("totalScore")) {
                return BigDecimal.valueOf(node.get("totalScore").asDouble());
            }
            // totalScore 필드가 없으면 items 배열의 score 합산 시도
            if (node.has("items") && node.get("items").isArray()) {
                BigDecimal sum = BigDecimal.ZERO;
                for (var item : node.get("items")) {
                    if (item.has("score")) {
                        sum = sum.add(BigDecimal.valueOf(item.get("score").asDouble()));
                    }
                }
                return sum;
            }
        } catch (Exception ignore) {
        }
        return BigDecimal.ZERO;
    }

    // 캘리브레이션
    @Transactional
    public void adjustCalibrations(UUID seasonId, UUID companyId, UUID actorMemberId, CalibrationAdjustReqDto dto) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다.");
        }
        // 결과 공개 이전 ACTIVE 시즌에서만 조정 가능
        if (!season.canCalibrate()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "진행 중이면서 결과 미공개 상태에서만 등급 조율이 가능합니다.");
        }
        for (CalibrationAdjustReqDto.Adjustment adj : dto.getAdjustments()) {
            EvaluationResponse response = responseRepository.findById(adj.getResponseId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다."));
            if (!response.getCompanyId().equals(companyId)) {
                throw new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다.");
            }
            if (response.getStatus() != EvaluationStatus.SUBMITTED) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "제출되지 않은 응답은 캘리브레이션할 수 없습니다. responseId=" + response.getResponseId());
            }
            String previousGrade = extractAdjustedGrade(response.getCalibrationJson());
            try {
                ObjectNode calibration = objectMapper.createObjectNode();
                if (response.getCalibrationJson() != null) {
                    calibration = (ObjectNode) objectMapper.readTree(response.getCalibrationJson());
                }
                calibration.put("adjustedGrade", adj.getAdjustedGrade());
                calibration.put("adjustmentReason", adj.getAdjustmentReason());
                calibration.put("adjustedAt", LocalDateTime.now().toString());
                calibration.put("adjustedBy", actorMemberId != null ? actorMemberId.toString() : "unknown");
                response.updateCalibration(objectMapper.writeValueAsString(calibration));
            } catch (Exception e) {
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "캘리브레이션 데이터 처리 실패");
            }
            // 감사 이력 저장 — 매 조정마다 append
            calibrationHistoryRepository.save(CalibrationHistory.builder()
                    .companyId(companyId)
                    .seasonId(seasonId)
                    .responseId(response.getResponseId())
                    .adjustedGrade(adj.getAdjustedGrade())
                    .adjustmentReason(adj.getAdjustmentReason())
                    .previousGrade(previousGrade)
                    .adjustedBy(actorMemberId != null ? actorMemberId : companyId)
                    .build());
        }
    }

    @Transactional
    public void confirmCalibration(UUID seasonId, UUID companyId) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다.");
        }
        // 결과 공개 이전 ACTIVE 시즌에서만 확정 가능
        if (!season.canCalibrate()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "진행 중이면서 결과 미공개 상태에서만 등급 조율을 확정할 수 있습니다.");
        }
        List<EvaluationResponse> responses = responseRepository.findByGroup_Season_SeasonId(seasonId)
                .stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .collect(Collectors.toList());
        // 모든 응답이 제출(SUBMITTED)되어야 캘리브레이션 확정 가능
        long notSubmitted = responses.stream()
                .filter(r -> r.getStatus() != EvaluationStatus.SUBMITTED)
                .count();
        if (notSubmitted > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "제출되지 않은 평가 응답이 " + notSubmitted + "건 있어 캘리브레이션을 확정할 수 없습니다.");
        }
        for (EvaluationResponse r : responses) {
            try {
                ObjectNode calibration;
                if (r.getCalibrationJson() != null) {
                    calibration = (ObjectNode) objectMapper.readTree(r.getCalibrationJson());
                } else {
                    calibration = objectMapper.createObjectNode();
                }
                calibration.put("confirmedAt", LocalDateTime.now().toString());
                r.updateCalibration(objectMapper.writeValueAsString(calibration));
            } catch (Exception e) {
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "캘리브레이션 확정 처리 실패");
            }
        }
        // phase 전이는 더 이상 존재하지 않음. 각 응답의 calibrationJson.confirmedAt 이
        // "이 응답은 확정되었다" 표시자 역할을 계속 수행한다.
    }

    /**
     * 결과 공개 — 관리자가 호출하거나 스케줄러가 자동 호출.
     * resultsPublishedAt 을 now() 로 세팅하고 피드백 면담을 자동 생성합니다.
     *
     * 선행 조건:
     *  - status = ACTIVE
     *  - 이미 공개된 상태가 아님 (resultsPublishedAt == null)
     *  - 모든 응답이 SUBMITTED
     *
     * 참고: 캘리브레이션 데이터 누락은 경고만 로깅하고 공개는 허용합니다
     * (관리자가 캘리브레이션을 건너뛰고 원시 점수로 공개하는 시나리오 지원).
     */
    @Transactional
    public void publishResults(UUID seasonId, UUID companyId) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "시즌을 찾을 수 없습니다.");
        }
        if (season.getStatus() != SeasonStatus.ACTIVE && season.getStatus() != SeasonStatus.CLOSED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "DRAFT 상태에서는 결과를 공개할 수 없습니다. 먼저 시즌을 시작하세요.");
        }
        if (season.isResultsPublished()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "이미 결과가 공개된 시즌입니다.");
        }
        List<EvaluationResponse> responses = responseRepository.findByGroup_Season_SeasonId(seasonId)
                .stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .collect(Collectors.toList());
        long notSubmitted = responses.stream()
                .filter(r -> r.getStatus() != EvaluationStatus.SUBMITTED)
                .count();
        if (notSubmitted > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "제출되지 않은 평가 응답이 " + notSubmitted + "건 있어 결과를 공개할 수 없습니다.");
        }
        season.markResultsPublished();
        // 피드백 면담 자동 생성 (같은 트랜잭션에서 수행)
        meetingRecordService.createFeedbackMeetingsForSeason(seasonId, companyId);
    }

    private String extractAdjustedGrade(String calibrationJson) {
        if (calibrationJson == null) return null;
        try {
            var node = objectMapper.readTree(calibrationJson);
            if (node.has("adjustedGrade")) {
                return node.get("adjustedGrade").asText();
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    // 진행도
    @Transactional(readOnly = true)
    public List<ResponseResDto> listBySeasonForProgress(UUID seasonId, UUID companyId) {
        return listBySeasonForProgress(seasonId, companyId, null);
    }

    /**
     * 진행도 조회 + 대상자 이름 contains 필터.
     * q 가 null/빈값이면 전체 반환. q 는 DTO 매핑 이후 targetMemberName 에 대해 case-insensitive contains 매칭한다.
     */
    public List<ResponseResDto> listBySeasonForProgress(UUID seasonId, UUID companyId, String q) {
        List<EvaluationResponse> list = responseRepository.findByGroup_Season_SeasonId(seasonId)
                .stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .collect(Collectors.toList());
        List<ResponseResDto> dtos = mapResponsesWithNames(list);
        if (q == null || q.isBlank()) return dtos;
        String needle = q.trim().toLowerCase();
        return dtos.stream()
                .filter(d -> {
                    String name = d.getTargetMemberName();
                    String dept = d.getTargetMemberDepartment();
                    return (name != null && name.toLowerCase().contains(needle))
                            || (dept != null && dept.toLowerCase().contains(needle));
                })
                .collect(Collectors.toList());
    }

    // ── 목표 요약 카드 ──

    /**
     * 평가 응답에 캡처된 목표 스냅샷과 현재 DB 상의 목표 최신 값을 비교하여
     * 요약 카드 목록을 반환한다.
     *
     * 요청자(requesterId)가 해당 응답의 evaluator 가 아니면 접근 차단한다.
     */
    @Transactional(readOnly = true)
    public List<GoalSummaryCardResDto> listGoalSummaries(UUID responseId, UUID requesterId) {
        EvaluationResponse response = responseRepository.findById(responseId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 응답을 찾을 수 없습니다."));
        if (!response.getEvaluatorId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "자신의 평가만 조회할 수 있습니다.");
        }

        List<GoalSnapshotDto> snapshots = parseSnapshots(response.getGoalSnapshotJson());
        if (snapshots.isEmpty()) {
            return List.of();
        }
        return buildGoalSummaryCardsFromSnapshots(snapshots);
    }

    /**
     * 본인이 피평가자인 자기평가(SELF) 응답의 목표 스냅샷을 기준으로 요약 카드를 만든다.
     * 자기평가 응답이 없거나 스냅샷이 비어 있으면, 시즌 기간과 겹치는 본인 목표를 현재값만으로 반환한다(폴백).
     *
     * @param completedOnly true 이면 현재 DB 기준 완료(COMPLETED) 목표만 남긴다.
     */
    @Transactional(readOnly = true)
    public List<GoalSummaryCardResDto> listMyGoalSummariesForSeason(
            UUID seasonId, UUID requesterMemberId, UUID companyId, boolean completedOnly) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다.");
        }

        Optional<EvaluationResponse> selfEval = responseRepository.findByGroup_Season_SeasonId(seasonId).stream()
                .filter(r -> r.getCompanyId().equals(companyId))
                .filter(r -> r.getEvaluatorId().equals(requesterMemberId))
                .filter(r -> r.getTargetMemberId().equals(requesterMemberId))
                .filter(r -> r.getEvaluationType() == EvalType.SELF)
                .findFirst();

        if (selfEval.isPresent()) {
            List<GoalSnapshotDto> snapshots = parseSnapshots(selfEval.get().getGoalSnapshotJson());
            if (!snapshots.isEmpty()) {
                List<GoalSummaryCardResDto> cards = buildGoalSummaryCardsFromSnapshots(snapshots);
                if (completedOnly) {
                    return cards.stream()
                            .filter(this::goalCardRepresentsCompleted)
                            .collect(Collectors.toList());
                }
                return cards;
            }
        }

        return buildFallbackGoalCardsForSeason(companyId, requesterMemberId, season, completedOnly);
    }

    private List<GoalSummaryCardResDto> buildGoalSummaryCardsFromSnapshots(List<GoalSnapshotDto> snapshots) {
        List<UUID> goalIds = snapshots.stream().map(GoalSnapshotDto::getGoalId).toList();
        Map<UUID, Goal> currentGoals = goalRepository.findAllById(goalIds).stream()
                .collect(Collectors.toMap(Goal::getGoalId, Function.identity()));

        List<GoalSummaryCardResDto> cards = new ArrayList<>();
        for (GoalSnapshotDto snap : snapshots) {
            Goal current = currentGoals.get(snap.getGoalId());
            GoalSummaryCardResDto.CurrentGoalView currentView = null;
            boolean changed = false;
            List<String> changeSummary = new ArrayList<>();

            if (current != null) {
                currentView = toCurrentGoalView(current);

                if (!Objects.equals(snap.getTitle(), current.getTitle())) {
                    changeSummary.add("TITLE");
                }
                if (snap.getTargetValue() != null && current.getTargetValue() != null
                        && snap.getTargetValue().compareTo(current.getTargetValue()) != 0) {
                    changeSummary.add("TARGET_VALUE");
                }
                if (!Objects.equals(snap.getStatusAtSnapshot(), current.getStatus())) {
                    changeSummary.add("STATUS");
                }
                if (!Objects.equals(snap.getEndDate(), current.getEndDate())) {
                    changeSummary.add("PERIOD");
                }
                if (snap.getActualValueAtSnapshot() != null && current.getActualValue() != null
                        && snap.getActualValueAtSnapshot().compareTo(current.getActualValue()) != 0) {
                    changeSummary.add("PROGRESS");
                }
                changed = !changeSummary.isEmpty();
            } else {
                changeSummary.add("DELETED_OR_HIDDEN");
                changed = true;
            }

            cards.add(GoalSummaryCardResDto.builder()
                    .goalId(snap.getGoalId())
                    .snapshot(snap)
                    .current(currentView)
                    .changedSinceSnapshot(changed)
                    .changeSummary(changeSummary)
                    .build());
        }
        return cards;
    }

    private static GoalSummaryCardResDto.CurrentGoalView toCurrentGoalView(Goal current) {
        return GoalSummaryCardResDto.CurrentGoalView.builder()
                .title(current.getTitle())
                .status(current.getStatus().name())
                .targetValue(current.getTargetValue())
                .actualValue(current.getActualValue())
                .achievementPct(current.getAchievementPct())
                .rolledAchievementPct(current.getRolledAchievementPct())
                .weightPct(current.getWeightPct())
                .unitLabel(current.getUnitLabel())
                .build();
    }

    private boolean goalCardRepresentsCompleted(GoalSummaryCardResDto card) {
        if (card.getCurrent() != null && card.getCurrent().getStatus() != null) {
            return GoalStatus.COMPLETED.name().equals(card.getCurrent().getStatus());
        }
        if (card.getSnapshot() != null && card.getSnapshot().getStatusAtSnapshot() != null) {
            return card.getSnapshot().getStatusAtSnapshot() == GoalStatus.COMPLETED;
        }
        return false;
    }

    private static boolean goalPeriodOverlapsSeason(Goal goal, EvaluationSeason season) {
        LocalDate sStart = season.getStartDate();
        LocalDate sEnd = season.getEndDate();
        return !goal.getEndDate().isBefore(sStart) && !goal.getStartDate().isAfter(sEnd);
    }

    private List<GoalSummaryCardResDto> buildFallbackGoalCardsForSeason(
            UUID companyId, UUID memberId, EvaluationSeason season, boolean completedOnly) {
        List<Goal> goals = goalRepository.findByCompanyIdAndOwnerId(companyId, memberId).stream()
                .filter(g -> goalPeriodOverlapsSeason(g, season))
                .filter(g -> !completedOnly || g.getStatus() == GoalStatus.COMPLETED)
                .collect(Collectors.toList());
        List<GoalSummaryCardResDto> cards = new ArrayList<>();
        for (Goal g : goals) {
            cards.add(GoalSummaryCardResDto.builder()
                    .goalId(g.getGoalId())
                    .snapshot(null)
                    .current(toCurrentGoalView(g))
                    .changedSinceSnapshot(false)
                    .changeSummary(List.of())
                    .build());
        }
        return cards;
    }

    private List<GoalSnapshotDto> parseSnapshots(String goalSnapshotJson) {
        if (goalSnapshotJson == null || goalSnapshotJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(goalSnapshotJson, new TypeReference<List<GoalSnapshotDto>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

}