package com._team._team.goal.service;

import com._team._team.dto.BusinessException;
import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.KpiTemplate;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.dto.reqdto.GoalCreateReqDto;
import com._team._team.goal.dto.reqdto.GoalUpdateReqDto;
import com._team._team.goal.dto.resdto.GoalResDto;
import com._team._team.goal.repository.GoalRepository;
import com._team._team.goal.repository.KpiTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GoalService {

    final GoalRepository goalRepository;
    final KpiTemplateRepository kpiTemplateRepository;

    @Autowired
    public GoalService(GoalRepository goalRepository, KpiTemplateRepository kpiTemplateRepository) {
        this.goalRepository = goalRepository;
        this.kpiTemplateRepository = kpiTemplateRepository;
    }

    @Transactional
    public GoalResDto createGoal(GoalCreateReqDto dto) {
        if (!dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 이후여야 합니다.");
        }
        if (dto.getTargetValue().compareTo(dto.getBaseline()) == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "목표값은 기준값과 달라야 합니다.");
        }

        KpiTemplate kpiTemplate = kpiTemplateRepository.findById(dto.getKpiTemplateId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "KPI 템플릿을 찾을 수 없습니다. id=" + dto.getKpiTemplateId()));

        Goal parentGoal = null;
        if (dto.getParentGoalId() != null) {
            parentGoal = goalRepository.findById(dto.getParentGoalId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                            "상위 목표를 찾을 수 없습니다. id=" + dto.getParentGoalId()));
        }

        Goal goal = Goal.builder()
                .kpiTemplate(kpiTemplate)
                .companyId(dto.getCompanyId())
                .parentGoal(parentGoal)
                .ownerType(dto.getOwnerType())
                .ownerId(dto.getOwnerId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .measureType(dto.getMeasureType())
                .unitType(dto.getUnitType())
                .unitLabel(dto.getUnitLabel())
                .baseline(dto.getBaseline())
                .targetValue(dto.getTargetValue())
                .capPct(dto.getCapPct())
                .contributionPct(dto.getContributionPct())
                .weightPct(dto.getWeightPct())
                .visibility(dto.getVisibility())
                .status(GoalStatus.DRAFT)
                .build();

        Goal saved = goalRepository.save(goal);
        return GoalResDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<GoalResDto> listGoals(UUID companyId) {
        return goalRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(GoalResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GoalResDto getGoal(UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));
        return GoalResDto.from(goal);
    }

    @Transactional
    public GoalResDto updateGoal(UUID goalId, GoalUpdateReqDto dto) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));
        goal.update(dto.getTitle(), dto.getDescription(), dto.getVisibility(),
                dto.getWeightPct(), dto.getContributionPct());
        return GoalResDto.from(goal);
    }

    @Transactional
    public GoalResDto activateGoal(UUID goalId, UUID approvedBy) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));
        if (goal.getStatus() != GoalStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "DRAFT 상태의 목표만 활성화할 수 있습니다. 현재 상태: " + goal.getStatus());
        }
        goal.activate(approvedBy);
        return GoalResDto.from(goal);
    }

    @Transactional
    public GoalResDto completeGoal(UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));
        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "ACTIVE 상태의 목표만 완료할 수 있습니다. 현재 상태: " + goal.getStatus());
        }
        goal.complete();
        return GoalResDto.from(goal);
    }

    @Transactional
    public GoalResDto cancelGoal(UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));
        goal.cancel();
        return GoalResDto.from(goal);
    }
}
