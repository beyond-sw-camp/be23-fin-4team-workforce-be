package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationDesign;
import com._team._team.evaluation.domain.EvaluationGroup;
import com._team._team.evaluation.domain.EvaluationSeason;
import com._team._team.evaluation.domain.converter.EvaluatorMapping;
import com._team._team.evaluation.domain.enums.SeasonStatus;
import com._team._team.evaluation.dto.reqdto.GroupCreateReqDto;
import com._team._team.evaluation.dto.reqdto.GroupUpdateReqDto;
import com._team._team.evaluation.dto.resdto.EvaluatorMappingResDto;
import com._team._team.evaluation.dto.resdto.GroupResDto;
import com._team._team.evaluation.repository.EvaluationDesignRepository;
import com._team._team.evaluation.repository.EvaluationGroupRepository;
import com._team._team.evaluation.repository.EvaluationSeasonRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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

        // [D-3] 타입 세이프 List<EvaluatorMapping> 로 직접 구성. 더 이상 수동 JSON 작성 불필요.
        List<com._team._team.evaluation.domain.converter.EvaluatorMapping> newMaps = new java.util.ArrayList<>();
        for (UUID targetId : targets) {
            for (String rawType : evalTypes) {
                String type = rawType == null ? "" : rawType.trim().toUpperCase();
                com._team._team.evaluation.domain.enums.EvalType evalType;
                try {
                    evalType = com._team._team.evaluation.domain.enums.EvalType.valueOf(type);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                if (evalType == com._team._team.evaluation.domain.enums.EvalType.SELF) {
                    newMaps.add(com._team._team.evaluation.domain.converter.EvaluatorMapping.builder()
                            .targetMemberId(targetId)
                            .evaluatorId(targetId)
                            .evaluationType(evalType)
                            .build());
                    continue;
                }
                List<UUID> candidates;
                try {
                    candidates = memberServiceClient.getCandidatesForEvaluator(companyId.toString(), targetId, type);
                } catch (Exception e) {
                    candidates = List.of();
                }
                if (candidates == null || candidates.isEmpty()) continue;
                UUID evaluatorId = candidates.get(0);
                newMaps.add(com._team._team.evaluation.domain.converter.EvaluatorMapping.builder()
                        .targetMemberId(targetId)
                        .evaluatorId(evaluatorId)
                        .evaluationType(evalType)
                        .build());
            }
        }

        group.updateEvaluatorMaps(newMaps);
        syncGroupScopeFieldsFromMaps(group, newMaps);
        groupRepository.save(group);
        if (group.getSeason().getStatus() == SeasonStatus.ACTIVE) {
            evaluationResponseService.materializeMissingResponsesForGroup(group);
        }
        return toGroupResDto(group);
    }

    @Transactional(readOnly = true)
    public List<GroupResDto> listGroups(UUID seasonId, UUID companyId) {
        EvaluationSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다."));
        if (!season.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "평가 시즌을 찾을 수 없습니다.");
        }
        return groupRepository.findBySeason_SeasonId(seasonId)
                .stream().map(this::toGroupResDto).collect(Collectors.toList());
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

        return toGroupResDto(groupRepository.save(group));
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
                group.getEvaluatorMaps()
        );

        return toGroupResDto(group);
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID companyId) {
        EvaluationGroup group = findGroupOrThrow(groupId, companyId);
        groupRepository.delete(group);
    }

    @Transactional
    public GroupResDto updateEvaluatorMaps(UUID groupId, UUID companyId, String evaluatorMapsJson) {
        EvaluationGroup group = findGroupOrThrow(groupId, companyId);
        // [D-3] 프론트에서 넘어오는 JSON 문자열을 VO 리스트로 파싱 후 엔티티에 주입.
        // 프론트 호환을 위해 API 시그니처는 유지하되 엔티티 내부는 타입 세이프.
        List<com._team._team.evaluation.domain.converter.EvaluatorMapping> maps;
        try {
            maps = evaluatorMapsJson == null || evaluatorMapsJson.isBlank()
                    ? new java.util.ArrayList<>()
                    : objectMapper.readValue(
                            evaluatorMapsJson,
                            new com.fasterxml.jackson.core.type.TypeReference<List<com._team._team.evaluation.domain.converter.EvaluatorMapping>>() {});
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "평가자 매핑 JSON이 올바르지 않습니다.");
        }
        group.updateEvaluatorMaps(maps);
        syncGroupScopeFieldsFromMaps(group, maps);
        groupRepository.save(group);
        if (group.getSeason().getStatus() == SeasonStatus.ACTIVE) {
            evaluationResponseService.materializeMissingResponsesForGroup(group);
        }
        return toGroupResDto(group);
    }

    private EvaluationGroup findGroupOrThrow(UUID groupId, UUID companyId) {
        EvaluationGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 그룹을 찾을 수 없습니다."));
        if (!group.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "평가 그룹을 찾을 수 없습니다.");
        }
        return group;
    }

    private GroupResDto toGroupResDto(EvaluationGroup group) {
        List<EvaluatorMapping> maps = group.getEvaluatorMaps() != null
                ? group.getEvaluatorMaps()
                : List.of();

        Set<UUID> memberIds = new LinkedHashSet<>();
        for (EvaluatorMapping map : maps) {
            if (map == null) continue;
            if (map.getTargetMemberId() != null) memberIds.add(map.getTargetMemberId());
            if (map.getEvaluatorId() != null) memberIds.add(map.getEvaluatorId());
        }

        Map<UUID, com._team._team.evaluation.feignclients.dto.MemberMinimalProfileDto> fetchedProfiles = Map.of();
        if (!memberIds.isEmpty()) {
            try {
                fetchedProfiles = memberServiceClient.getProfilesByIds(List.copyOf(memberIds));
            } catch (Exception ignored) {
                fetchedProfiles = Map.of();
            }
        }
        final Map<UUID, com._team._team.evaluation.feignclients.dto.MemberMinimalProfileDto> profiles = fetchedProfiles;

        List<EvaluatorMappingResDto> resMaps = maps.stream()
                .map(map -> {
                    com._team._team.evaluation.feignclients.dto.MemberMinimalProfileDto targetProfile =
                            profiles.get(map.getTargetMemberId());
                    com._team._team.evaluation.feignclients.dto.MemberMinimalProfileDto evaluatorProfile =
                            profiles.get(map.getEvaluatorId());
                    return EvaluatorMappingResDto.builder()
                            .targetMemberId(map.getTargetMemberId())
                            .evaluatorId(map.getEvaluatorId())
                            .evaluationType(map.getEvaluationType())
                            .targetMemberProfileUrl(targetProfile != null ? targetProfile.getProfileUrl() : null)
                            .evaluatorProfileUrl(evaluatorProfile != null ? evaluatorProfile.getProfileUrl() : null)
                            .build();
                })
                .collect(Collectors.toList());

        return GroupResDto.from(group, resMaps);
    }

    /**
     * evaluatorMaps 를 단일 소스 오브 트루스로 보고
     * 그룹의 evaluationTypesJson / targetMemberIdsJson 을 동기화한다.
     */
    private void syncGroupScopeFieldsFromMaps(EvaluationGroup group, List<EvaluatorMapping> maps) {
        Set<String> evalTypes = new LinkedHashSet<>();
        Set<UUID> targetIds = new LinkedHashSet<>();
        if (maps != null) {
            for (EvaluatorMapping map : maps) {
                if (map == null) continue;
                if (map.getEvaluationType() != null) {
                    evalTypes.add(map.getEvaluationType().name());
                }
                if (map.getTargetMemberId() != null) {
                    targetIds.add(map.getTargetMemberId());
                }
            }
        }
        String evalTypesJson;
        String targetMemberIdsJson;
        try {
            evalTypesJson = objectMapper.writeValueAsString(evalTypes);
            targetMemberIdsJson = objectMapper.writeValueAsString(targetIds);
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "그룹 매핑 동기화에 실패했습니다.");
        }
        group.update(
                group.getName(),
                evalTypesJson,
                targetMemberIdsJson,
                group.getEvaluatorMaps()
        );
    }
}
