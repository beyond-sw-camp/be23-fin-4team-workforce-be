package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationDesign;
import com._team._team.evaluation.dto.reqdto.DesignCreateReqDto;
import com._team._team.evaluation.dto.reqdto.DesignUpdateReqDto;
import com._team._team.evaluation.dto.resdto.DesignResDto;
import com._team._team.evaluation.repository.EvaluationDesignRepository;
import com._team._team.evaluation.repository.EvaluationGroupRepository;
import com._team._team.evaluation.repository.EvaluationResponseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvaluationDesignService {

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
        EvaluationDesign design = EvaluationDesign.builder()
                .companyId(companyId)
                .name(dto.getName())
                .sectionsJson(dto.getSectionsJson())
                .gradeConfigJson(dto.getGradeConfigJson())
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
        design.update(
                dto.getName() != null ? dto.getName() : design.getName(),
                dto.getSectionsJson() != null ? dto.getSectionsJson() : design.getSectionsJson(),
                dto.getGradeConfigJson() != null ? dto.getGradeConfigJson() : design.getGradeConfigJson()
        );
        return DesignResDto.from(design);
    }

    @Transactional
    public DesignResDto duplicateDesign(UUID designId, UUID companyId) {
        EvaluationDesign source = findDesignOrThrow(designId, companyId);
        EvaluationDesign duplicated = EvaluationDesign.builder()
                .companyId(companyId)
                .name(source.getName() + " (복제)")
                .sectionsJson(source.getSectionsJson())
                .gradeConfigJson(source.getGradeConfigJson())
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
}
