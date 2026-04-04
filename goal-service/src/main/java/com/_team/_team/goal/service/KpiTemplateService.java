package com._team._team.goal.service;

import com._team._team.dto.BusinessException;
import com._team._team.goal.domain.KpiTemplate;
import com._team._team.goal.dto.reqdto.KpiTemplateCreateReqDto;
import com._team._team.goal.dto.resdto.KpiTemplateResDto;
import com._team._team.goal.repository.KpiTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class KpiTemplateService {

    final KpiTemplateRepository kpiTemplateRepository;

    @Autowired
    public KpiTemplateService(KpiTemplateRepository kpiTemplateRepository) {
        this.kpiTemplateRepository = kpiTemplateRepository;
    }

    @Transactional
    public KpiTemplateResDto createKpiTemplate(KpiTemplateCreateReqDto dto) {
        KpiTemplate kpiTemplate = KpiTemplate.builder()
                .companyId(dto.getCompanyId())
                .name(dto.getName())
                .measureType(dto.getMeasureType())
                .unitType(dto.getUnitType())
                .cycle(dto.getCycle())
                .capPct(dto.getCapPct())
                .isActive(true)
                .build();

        KpiTemplate saved = kpiTemplateRepository.save(kpiTemplate);
        return KpiTemplateResDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<KpiTemplateResDto> listKpiTemplates(UUID companyId) {
        List<KpiTemplate> templates = kpiTemplateRepository.findByCompanyIdIsNullOrCompanyId(companyId);
        return templates.stream()
                .map(KpiTemplateResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivate(UUID kpiTemplateId) {
        KpiTemplate kpiTemplate = kpiTemplateRepository.findById(kpiTemplateId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "KPI 템플릿을 찾을 수 없습니다. id=" + kpiTemplateId));
        kpiTemplate.deactivate();
    }
}
