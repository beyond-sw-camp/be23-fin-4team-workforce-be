package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationDesign;
import com._team._team.evaluation.domain.EvaluationGroup;
import com._team._team.evaluation.domain.EvaluationSeason;
import com._team._team.evaluation.domain.enums.SeasonStatus;
import com._team._team.evaluation.dto.reqdto.GroupCreateReqDto;
import com._team._team.evaluation.dto.reqdto.GroupUpdateReqDto;
import com._team._team.evaluation.dto.resdto.GroupResDto;
import com._team._team.evaluation.repository.EvaluationDesignRepository;
import com._team._team.evaluation.repository.EvaluationGroupRepository;
import com._team._team.evaluation.repository.EvaluationSeasonRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvaluationGroupService {

    private final EvaluationGroupRepository groupRepository;
    private final EvaluationSeasonRepository seasonRepository;
    private final EvaluationDesignRepository designRepository;
    private final ObjectMapper objectMapper;
    private final EvaluationResponseService evaluationResponseService;
    private final com._team._team.evaluation.feignclients.MemberServiceClient memberServiceClient;

    public EvaluationGroupService(EvaluationGroupRepository groupRepository,
                                   EvaluationSeasonRepository seasonRepository,
                                   EvaluationDesignRepository designRepository,
                                   ObjectMapper objectMapper,
                                   EvaluationResponseService evaluationResponseService,
                                   com._team._team.evaluation.feignclients.MemberServiceClient memberServiceClient) {
        this.groupRepository = groupRepository;
        this.seasonRepository = seasonRepository;
        this.designRepository = designRepository;
        this.objectMapper = objectMapper;
        this.evaluationResponseService = evaluationResponseService;
        this.memberServiceClient = memberServiceClient;
    }

    /**
     * 평가자 자동 지정.
     *
     * 각 대상자 × (그룹에 설정된 평가 유형) 조합마다 member-service 후보 조회 API 로 후보를 얻어
     * evaluatorMaps 를 구성한다.
     *  - SELF: 항상 target = evaluator
     *  - DOWNWARD/UPWARD/PEER: 후보 중 첫 번째 (조직 + 직급 조건 일치) 를 배정. 후보 0명이면 건너뜀
     *
     * 기존 evaluatorMaps 는 덮어씀.
     * 반환값: 자동 배정 결과가 반영된 GroupResDto.
     */
    @Transactional
    public GroupResDto autoAssignEvaluators(UUID seasonId, UUID groupId, UUID companyId, String basis) {
        EvaluationGroup group = findGroupOrThrow(groupId, companyId);

        List<UUID> targets;
        List<String> evalTypes;
        try {
            targets = group.getTargetMemberIdsJson() != null
                    ? objectMapper.readValue(group.getTargetMemberIdsJson(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<UUID>>() {})
                    : List.of();
            evalTypes = group.getEvaluationTypesJson() != null
                    ? objectMapper.readValue(group.getEvaluationTypesJson(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {})
                    : List.of();
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "그룹 설정 JSON 이 올바르지 않습니다.");
        }

        List<java.util.Map<String, String>> newMaps = new java.util.ArrayList<>();
        for (UUID targetId : targets) {
            for (String rawType : evalTypes) {
                String type = rawType == null ? "" : rawType.trim().toUpperCase();
                if ("SELF".equals(type)) {
                    newMaps.add(java.util.Map.of(
                            "targetMemberId", targetId.toString(),
                            "evaluatorId", targetId.toString(),
                            "evaluationType", "SELF"));
                    continue;
                }
                List<UUID> candidates;
                try {
                    candidates = memberServiceClient.getCandidatesForEvaluator(companyId.toString(), targetId, type);
                } catch (Exception e) {
                    candidates = List.of();
                }
                if (candidates == null || candidates.isEmpty()) continue;
                // 첫 후보를 기본 배정 (여러 명 후보일 경우 DOWNWARD 는 상급자 중 1명 / PEER 는 동급자 1명 등)
                UUID evaluatorId = candidates.get(0);
                newMaps.add(java.util.Map.of(
                        "targetMemberId", targetId.toString(),
                        "evaluatorId", evaluatorId.toString(),
                        "evaluationType", type));
            }
        }

        try {
            String json = objectMapper.writeValueAsString(newMaps);
            group.updateEvaluatorMaps(json);
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "평가자 매핑 저장에 실패했습니다.");
        }
        groupRepository.save(group);
        if (group.getSeason().getStatus() == SeasonStatus.ACTIVE) {
            evaluationResponseService.materializeMissingResponsesForGroup(group);
        }
        return GroupResDto.from(group);
    }

    @Transactional(readOnly = true)
    public List<GroupResDto> listGroups(UUID seasonId, UUID companyId) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다.");
        }
        return groupRepository.findBySeason_SeasonId(seasonId)
                .stream().map(GroupResDto::from).collect(Collectors.toList());
    }

    @Transactional
    public GroupResDto createGroup(UUID seasonId, UUID companyId, GroupCreateReqDto dto) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다.");
        }

        EvaluationDesign design = null;
        if (dto.getDesignId() != null) {
            design = designRepository.findById(dto.getDesignId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 설계를 찾을 수 없습니다."));
        }

        String evalTypesJson = null;
        String targetMemberIdsJson = null;
        try {
            if (dto.getEvaluationTypes() != null) evalTypesJson = objectMapper.writeValueAsString(dto.getEvaluationTypes());
            if (dto.getTargetMemberIds() != null) targetMemberIdsJson = objectMapper.writeValueAsString(dto.getTargetMemberIds());
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "JSON 변환에 실패했습니다.");
        }

        EvaluationGroup group = EvaluationGroup.builder()
                .companyId(companyId)
                .season(season)
                .name(dto.getName())
                .evaluationTypesJson(evalTypesJson)
                .targetMemberIdsJson(targetMemberIdsJson)
                .design(design)
                .build();

        return GroupResDto.from(groupRepository.save(group));
    }

    @Transactional
    public GroupResDto updateGroup(UUID seasonId, UUID groupId, UUID companyId, GroupUpdateReqDto dto) {
        EvaluationGroup group = findGroupOrThrow(groupId, companyId);

        String evalTypesJson = group.getEvaluationTypesJson();
        String targetMemberIdsJson = group.getTargetMemberIdsJson();
        try {
            if (dto.getEvaluationTypes() != null) evalTypesJson = objectMapper.writeValueAsString(dto.getEvaluationTypes());
            if (dto.getTargetMemberIds() != null) targetMemberIdsJson = objectMapper.writeValueAsString(dto.getTargetMemberIds());
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "JSON 변환에 실패했습니다.");
        }

        if (dto.getDesignId() != null) {
            EvaluationDesign design = designRepository.findById(dto.getDesignId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 설계를 찾을 수 없습니다."));
            group.assignDesign(design);
        }

        group.update(
                dto.getName() != null ? dto.getName() : group.getName(),
                evalTypesJson,
                targetMemberIdsJson,
                group.getEvaluatorMapsJson()
        );

        return GroupResDto.from(group);
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID companyId) {
        EvaluationGroup group = findGroupOrThrow(groupId, companyId);
        groupRepository.delete(group);
    }

    @Transactional
    public GroupResDto updateEvaluatorMaps(UUID groupId, UUID companyId, String evaluatorMapsJson) {
        EvaluationGroup group = findGroupOrThrow(groupId, companyId);
        group.updateEvaluatorMaps(evaluatorMapsJson);
        groupRepository.save(group);
        if (group.getSeason().getStatus() == SeasonStatus.ACTIVE) {
            evaluationResponseService.materializeMissingResponsesForGroup(group);
        }
        return GroupResDto.from(group);
    }

    private EvaluationGroup findGroupOrThrow(UUID groupId, UUID companyId) {
        EvaluationGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 그룹을 찾을 수 없습니다."));
        if (!group.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "평가 그룹을 찾을 수 없습니다.");
        }
        return group;
    }
}
