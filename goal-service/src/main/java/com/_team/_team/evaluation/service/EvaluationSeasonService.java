package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.EvaluationSeason;
import com._team._team.evaluation.domain.EvaluationGroup;
import com._team._team.evaluation.domain.enums.EvalType;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import com._team._team.evaluation.domain.enums.SeasonStatus;
import com._team._team.evaluation.dto.reqdto.SeasonCreateReqDto;
import com._team._team.evaluation.dto.reqdto.SeasonUpdateReqDto;
import com._team._team.evaluation.dto.resdto.SeasonResDto;
import com._team._team.evaluation.repository.EvaluationGroupRepository;
import com._team._team.evaluation.repository.EvaluationResponseRepository;
import com._team._team.evaluation.repository.EvaluationSeasonRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvaluationSeasonService {

    private final EvaluationSeasonRepository seasonRepository;
    private final EvaluationGroupRepository groupRepository;
    private final EvaluationResponseRepository responseRepository;
    private final ObjectMapper objectMapper;

    public EvaluationSeasonService(EvaluationSeasonRepository seasonRepository,
                                    EvaluationGroupRepository groupRepository,
                                    EvaluationResponseRepository responseRepository,
                                    ObjectMapper objectMapper) {
        this.seasonRepository = seasonRepository;
        this.groupRepository = groupRepository;
        this.responseRepository = responseRepository;
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
        List<EvaluationGroup> groups = groupRepository.findBySeason_SeasonId(seasonId);
        List<EvaluationResponse> responses = new ArrayList<>();
        for (EvaluationGroup group : groups) {
            if (group.getEvaluatorMapsJson() != null) {
                try {
                    List<Map<String, String>> maps = objectMapper.readValue(
                            group.getEvaluatorMapsJson(), new TypeReference<>() {});
                    for (Map<String, String> map : maps) {
                        EvaluationResponse response = EvaluationResponse.builder()
                                .companyId(companyId)
                                .group(group)
                                .targetMemberId(UUID.fromString(map.get("targetMemberId")))
                                .evaluatorId(UUID.fromString(map.get("evaluatorId")))
                                .evaluationType(EvalType.valueOf(map.get("evaluationType")))
                                .build();
                        responses.add(response);
                    }
                } catch (Exception e) {
                    throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "평가자 매핑 데이터를 처리할 수 없습니다: " + group.getName());
                }
            }
        }
        if (!responses.isEmpty()) {
            responseRepository.saveAll(responses);
        }

        return SeasonResDto.from(season);
    }

    @Transactional
    public SeasonResDto closeSeason(UUID seasonId, UUID companyId) {
        EvaluationSeason season = findSeasonOrThrow(seasonId, companyId);
        if (season.getStatus() != SeasonStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "진행 중인 시즌만 종료할 수 있습니다.");
        }
        season.close();
        return SeasonResDto.from(season);
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
