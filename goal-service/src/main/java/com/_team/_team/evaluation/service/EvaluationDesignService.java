package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationDesign;
import com._team._team.evaluation.domain.converter.EvaluationSection;
import com._team._team.evaluation.domain.converter.GradeConfig;
import com._team._team.evaluation.dto.reqdto.DesignCreateReqDto;
import com._team._team.evaluation.dto.reqdto.DesignUpdateReqDto;
import com._team._team.evaluation.dto.resdto.DesignResDto;
import com._team._team.evaluation.repository.EvaluationDesignRepository;
import com._team._team.evaluation.repository.EvaluationGroupRepository;
import com._team._team.evaluation.repository.EvaluationResponseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvaluationDesignService {

    /** [D-4] 요청 JSON 문자열 → VO 변환 전용 ObjectMapper. 프론트 호환 경로. */
    private static final ObjectMapper OM = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final EvaluationDesignRepository designRepository;
    private final EvaluationGroupRepository groupRepository;
    private final EvaluationResponseRepository responseRepository;

    public EvaluationDesignService(
            EvaluationDesignRepository designRepository,
            EvaluationGroupRepository groupRepository,
            EvaluationResponseRepository responseRepository) {
        this.designRepository = designRepository;
        this.groupRepository = groupRepository;
        this.responseRepository = responseRepository;
    }

    @Transactional(readOnly = true)
    public List<DesignResDto> listDesigns(UUID companyId) {
        return designRepository.findByCompanyId(companyId)
                .stream().map(DesignResDto::from).collect(Collectors.toList());
    }

    @Transactional
    public DesignResDto createDesign(UUID companyId, DesignCreateReqDto dto) {
        List<EvaluationSection> sections = resolveSections(dto.getSections(), dto.getSectionsJson());
        GradeConfig gradeConfig = resolveGradeConfig(dto.getGradeConfig(), dto.getGradeConfigJson());

        EvaluationDesign design = EvaluationDesign.builder()
                .companyId(companyId)
                .name(dto.getName())
                .sections(sections)
                .gradeConfig(gradeConfig)
                .templateVersion(1)
                .defaultTemplate(!designRepository.existsByCompanyIdAndDefaultTemplateTrue(companyId))
                .build();
        return DesignResDto.from(designRepository.save(design));
    }

    /**
     * 평가 작성 화면 등에서 설계 본문을 조회한다.
     * {@code EVALUATION:READ} 권한 없이 접근하는 평가자는, 해당 설계를 사용하는 그룹에 본인이 배정된 경우에만 허용한다.
     */
    @Transactional(readOnly = true)
    public DesignResDto getDesign(UUID designId, UUID companyId, UUID requesterMemberId) {
        EvaluationDesign design = findDesignOrThrow(designId, companyId);
        if (!responseRepository.existsEvaluatorAssignmentForDesign(requesterMemberId, companyId, designId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "이 평가 설계에 접근할 수 없습니다.");
        }
        return DesignResDto.from(design);
    }

    @Transactional
    public DesignResDto updateDesign(UUID designId, UUID companyId, DesignUpdateReqDto dto) {
        EvaluationDesign design = findDesignOrThrow(designId, companyId);

        // VO 우선, JSON 문자열 fallback. null 이면 기존값 유지.
        List<EvaluationSection> sections = null;
        if (dto.getSections() != null) {
            sections = dto.getSections();
        } else if (dto.getSectionsJson() != null) {
            sections = parseSectionsJson(dto.getSectionsJson());
        }

        GradeConfig gradeConfig = null;
        if (dto.getGradeConfig() != null) {
            gradeConfig = dto.getGradeConfig();
        } else if (dto.getGradeConfigJson() != null) {
            gradeConfig = parseGradeConfigJson(dto.getGradeConfigJson());
        }

        design.update(dto.getName(), sections, gradeConfig);
        return DesignResDto.from(design);
    }

    @Transactional
    public DesignResDto duplicateDesign(UUID designId, UUID companyId) {
        EvaluationDesign source = findDesignOrThrow(designId, companyId);
        EvaluationDesign duplicated = EvaluationDesign.builder()
                .companyId(companyId)
                .name(source.getName() + " (복제)")
                .sections(source.getSections() != null ? new ArrayList<>(source.getSections()) : new ArrayList<>())
                .gradeConfig(source.getGradeConfig())
                .templateVersion(1)
                .defaultTemplate(false)
                .build();
        return DesignResDto.from(designRepository.save(duplicated));
    }

    @Transactional
    public void deleteDesign(UUID designId, UUID companyId) {
        EvaluationDesign design = findDesignOrThrow(designId, companyId);
        long usedByGroups = groupRepository.countByDesign_DesignId(designId);
        if (usedByGroups > 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "현재 " + usedByGroups + "개 그룹에서 사용 중인 설계는 삭제할 수 없습니다.");
        }
        if (Boolean.TRUE.equals(design.getDefaultTemplate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "기본 템플릿은 삭제할 수 없습니다.");
        }
        designRepository.delete(design);
    }

    private EvaluationDesign findDesignOrThrow(UUID designId, UUID companyId) {
        EvaluationDesign design = designRepository.findById(designId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 설계를 찾을 수 없습니다."));
        if (!design.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "평가 설계를 찾을 수 없습니다.");
        }
        return design;
    }

    // ─────────────────────────── [D-4] VO / JSON 통합 helper ───────────────────────────

    private List<EvaluationSection> resolveSections(List<EvaluationSection> vo, String json) {
        if (vo != null) return vo;
        if (json != null) return parseSectionsJson(json);
        return new ArrayList<>();
    }

    private GradeConfig resolveGradeConfig(GradeConfig vo, String json) {
        if (vo != null) return vo;
        if (json != null) return parseGradeConfigJson(json);
        return null;
    }

    private List<EvaluationSection> parseSectionsJson(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return OM.readValue(json, new TypeReference<List<EvaluationSection>>() {});
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "sectionsJson 파싱 실패: " + e.getMessage());
        }
    }

    private GradeConfig parseGradeConfigJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return OM.readValue(json, GradeConfig.class);
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "gradeConfigJson 파싱 실패: " + e.getMessage());
        }
    }
}
