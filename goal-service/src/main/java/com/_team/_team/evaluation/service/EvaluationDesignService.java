package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationDesign;
import com._team._team.evaluation.dto.reqdto.DesignCreateReqDto;
import com._team._team.evaluation.dto.reqdto.DesignUpdateReqDto;
import com._team._team.evaluation.dto.resdto.DesignResDto;
import com._team._team.evaluation.repository.EvaluationDesignRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvaluationDesignService {

    private final EvaluationDesignRepository designRepository;

    public EvaluationDesignService(EvaluationDesignRepository designRepository) {
        this.designRepository = designRepository;
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
                .build();
        return DesignResDto.from(designRepository.save(design));
    }

    @Transactional(readOnly = true)
    public DesignResDto getDesign(UUID designId, UUID companyId) {
        EvaluationDesign design = findDesignOrThrow(designId, companyId);
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

    private EvaluationDesign findDesignOrThrow(UUID designId, UUID companyId) {
        EvaluationDesign design = designRepository.findById(designId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 설계를 찾을 수 없습니다."));
        if (!design.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "평가 설계를 찾을 수 없습니다.");
        }
        return design;
    }
}
