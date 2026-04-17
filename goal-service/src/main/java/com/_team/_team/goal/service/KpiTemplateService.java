package com._team._team.goal.service;

import com._team._team.dto.BusinessException;
import com._team._team.goal.domain.KpiTemplate;
import com._team._team.goal.domain.converter.KpiDefinition;
import com._team._team.goal.domain.enums.*;
import com._team._team.goal.dto.reqdto.*;
import com._team._team.goal.dto.resdto.GoalResDto;
import com._team._team.goal.dto.resdto.KpiTemplateResDto;
import com._team._team.goal.repository.KpiTemplateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class KpiTemplateService {

    private final KpiTemplateRepository kpiTemplateRepository;
    private final GoalService goalService;
    private final GoalApprovalService goalApprovalService;

    public KpiTemplateService(
            KpiTemplateRepository kpiTemplateRepository,
            GoalService goalService,
            GoalApprovalService goalApprovalService) {
        this.kpiTemplateRepository = kpiTemplateRepository;
        this.goalService = goalService;
        this.goalApprovalService = goalApprovalService;
    }

    @Transactional
    public KpiTemplateResDto createKpiTemplate(KpiTemplateCreateReqDto dto, UUID companyId) {
        if (!dto.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사의 KPI 템플릿을 생성할 수 없습니다.");
        }

        List<KpiDefinition> kpis = new ArrayList<>();
        if (dto.getKpis() != null && !dto.getKpis().isEmpty()) {
            for (KpiDefinitionItemReqDto item : dto.getKpis()) {
                kpis.add(new KpiDefinition(
                        item.getTitle(),
                        item.getTargetValue(),
                        item.getUnit(),
                        item.getOwnerId(),
                        item.getOwnerType()));
            }
        }

        boolean reqApr = dto.getRequireApproval() != null && dto.getRequireApproval();

        // goalApprovalPolicy 가 명시적으로 들어오면 사용, 아니면 requireApproval 로 유도
        GoalApprovalPolicy policy = dto.getGoalApprovalPolicy();
        if (policy == null) {
            policy = GoalApprovalPolicy.fromLegacyFlag(reqApr);
        }

        KpiTemplate kpiTemplate = KpiTemplate.builder()
                .companyId(companyId)
                .name(dto.getName())
                .measureType(dto.getMeasureType())
                .unitType(dto.getUnitType())
                .unitLabel(dto.getUnitLabel())
                .cycle(dto.getCycle())
                .capPct(dto.getCapPct())
                .isActive(true)
                .specCycleType(dto.getSpecCycleType())
                .targetTeamId(dto.getTargetTeamId())
                .requireApproval(reqApr)
                .goalApprovalPolicy(policy)
                .kpis(kpis)
                .build();

        KpiTemplate saved = kpiTemplateRepository.save(kpiTemplate);
        return KpiTemplateResDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<KpiTemplateResDto> listKpiTemplates(UUID companyId) {
        List<KpiTemplate> templates = kpiTemplateRepository.findByCompanyIdIsNullOrCompanyId(companyId);
        return templates.stream().map(KpiTemplateResDto::from).toList();
    }

    @Transactional(readOnly = true)
    public KpiTemplateResDto getKpiTemplate(UUID kpiTemplateId, UUID companyId) {
        KpiTemplate t = kpiTemplateRepository.findById(kpiTemplateId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "KPI 템플릿을 찾을 수 없습니다. id=" + kpiTemplateId));
        if (t.getCompanyId() != null && !t.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사 템플릿입니다.");
        }
        return KpiTemplateResDto.from(t);
    }

    @Transactional
    public void deactivate(UUID kpiTemplateId, UUID companyId) {
        KpiTemplate kpiTemplate = kpiTemplateRepository.findById(kpiTemplateId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "KPI 템플릿을 찾을 수 없습니다. id=" + kpiTemplateId));
        if (kpiTemplate.getCompanyId() != null && !kpiTemplate.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사의 KPI 템플릿을 비활성화할 수 없습니다.");
        }
        kpiTemplate.deactivate();
    }

    @Transactional
    public List<GoalResDto> generateGoals(
            UUID kpiTemplateId, KpiTemplateGenerateReqDto dto, UUID companyId, UUID requesterId) {
        KpiTemplate t = kpiTemplateRepository.findById(kpiTemplateId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "템플릿을 찾을 수 없습니다."));
        if (t.getCompanyId() != null && !t.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사 템플릿입니다.");
        }
        List<KpiDefinition> items = t.getKpis();
        if (items == null || items.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "템플릿에 KPI 정의(kpis)가 없습니다.");
        }
        if (dto.getPeriodStart() == null || dto.getPeriodEnd() == null || !dto.getPeriodEnd().isAfter(dto.getPeriodStart())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "목표 기간(periodStart/periodEnd)을 올바르게 입력하세요.");
        }

        List<UUID> created = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            KpiDefinition item = items.get(i);
            GoalOwnerType ot = item.getOwnerType() != null ? item.getOwnerType() : GoalOwnerType.MEMBER;
            UUID itemOwnerId = item.getOwnerId();
            if (dto.getOwnerMapping() != null) {
                for (KpiGenerateOwnerMappingReqDto m : dto.getOwnerMapping()) {
                    if (m.getKpiIndex() != null && m.getKpiIndex() == i) {
                        itemOwnerId = m.getOwnerId();
                        if (m.getOwnerType() != null) {
                            ot = m.getOwnerType();
                        }
                    }
                }
            }
            GoalCreateReqDto cg = GoalCreateReqDto.builder()
                    .companyId(companyId)
                    .kpiTemplateId(t.getKpiTemplateId())
                    .parentGoalId(dto.getParentGoalId())
                    .cycle(t.getCycle())
                    .ownerType(ot)
                    .ownerId(itemOwnerId)
                    .title(item.getTitle())
                    .description("")
                    .startDate(dto.getPeriodStart())
                    .endDate(dto.getPeriodEnd())
                    .measureType(t.getMeasureType())
                    .unitType(t.getUnitType())
                    .unitLabel(item.getUnit() != null && !item.getUnit().isBlank() ? item.getUnit() : t.getUnitLabel())
                    .baseline(BigDecimal.ZERO)
                    .targetValue(item.getTargetValue())
                    .capPct(Math.max(1, t.getCapPct()))
                    .visibility(GoalVisibility.PUBLIC)
                    .goalKind(GoalKind.KR)
                    .requireApproval(false)
                    .activateImmediately(false)
                    .build();
            GoalResDto g = goalService.createGoal(cg, requesterId, companyId);
            created.add(g.getGoalId());
        }

        GoalApprovalPolicy tplPolicy = t.resolveApprovalPolicy();
        if (tplPolicy.requiresActivation()) {
            // 활성화 승인이 필요한 정책이면 번들을 만들어 승인 요청
            if (dto.getApproval() == null || dto.getApproval().getApproverId() == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "템플릿이 활성화 결재를 요구합니다. 승인 단계 정보가 필요합니다.");
            }
            goalApprovalService.createBundleForGoals(created, requesterId, companyId, dto.getApproval());
        } else {
            // 활성화 승인이 불필요하면 즉시 활성화
            for (UUID gid : created) {
                goalService.activateGoal(gid, requesterId, companyId);
            }
        }

        return created.stream().map(id -> goalService.getGoal(id, requesterId, companyId)).toList();
    }
}
