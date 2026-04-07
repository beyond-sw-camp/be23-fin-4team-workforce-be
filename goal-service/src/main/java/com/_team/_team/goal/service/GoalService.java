package com._team._team.goal.service;

import com._team._team.dto.BusinessException;
import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.KpiTemplate;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.domain.enums.GoalVisibility;
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
    public GoalResDto createGoal(GoalCreateReqDto dto, UUID requesterId, UUID requesterCompanyId) {
        if (!dto.getEndDate().isAfter(dto.getStartDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 이후여야 합니다.");
        }
        if (dto.getTargetValue().compareTo(dto.getBaseline()) == 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "목표값은 기준값과 달라야 합니다.");
        }

        // 회사 격리 확인 (DTO의 companyId와 요청자의 companyId 일치 여부)
        if (!dto.getCompanyId().equals(requesterCompanyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사의 목표를 생성할 수 없습니다.");
        }

        KpiTemplate kpiTemplate = kpiTemplateRepository.findById(dto.getKpiTemplateId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "KPI 템플릿을 찾을 수 없습니다. id=" + dto.getKpiTemplateId()));

        // KPI 템플릿 회사 격리 확인
        if (!kpiTemplate.getCompanyId().equals(requesterCompanyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사의 KPI 템플릿을 사용할 수 없습니다.");
        }

        if (!kpiTemplate.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "비활성 KPI 템플릿으로 목표를 생성할 수 없습니다. id=" + dto.getKpiTemplateId());
        }

        Goal parentGoal = null;
        if (dto.getParentGoalId() != null) {
            parentGoal = goalRepository.findById(dto.getParentGoalId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                            "상위 목표를 찾을 수 없습니다. id=" + dto.getParentGoalId()));
            
            // 상위 목표 회사 격리 확인
            if (!parentGoal.getCompanyId().equals(requesterCompanyId)) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "타사의 목표를 상위 목표로 지정할 수 없습니다.");
            }
        }

        Goal goal = Goal.builder()
                .kpiTemplate(kpiTemplate)
                .companyId(requesterCompanyId)
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
    public List<GoalResDto> listGoals(UUID companyId, UUID requesterId, UUID requesterCompanyId) {
        validateSameCompany(companyId, requesterCompanyId);

        return goalRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .filter(g -> isVisible(g, requesterId))
                .map(GoalResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GoalResDto getGoal(UUID goalId, UUID requesterId, UUID requesterCompanyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));

        validateSameCompany(goal.getCompanyId(), requesterCompanyId);

        if (!isVisible(goal, requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "해당 목표에 대한 접근 권한이 없습니다.");
        }

        return GoalResDto.from(goal);
    }

    @Transactional
    public GoalResDto updateGoal(UUID goalId, GoalUpdateReqDto dto, UUID requesterCompanyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));

        validateSameCompany(goal.getCompanyId(), requesterCompanyId);

        if (goal.getStatus() == GoalStatus.COMPLETED || goal.getStatus() == GoalStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "완료되거나 취소된 목표는 수정할 수 없습니다. 현재 상태: " + goal.getStatus());
        }
        goal.update(dto.getTitle(), dto.getDescription(), dto.getVisibility(),
                dto.getWeightPct(), dto.getContributionPct());
        return GoalResDto.from(goal);
    }

    @Transactional
    public GoalResDto activateGoal(UUID goalId, UUID approvedBy, UUID requesterCompanyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));

        validateSameCompany(goal.getCompanyId(), requesterCompanyId);

        if (goal.getStatus() != GoalStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "DRAFT 상태의 목표만 활성화할 수 있습니다. 현재 상태: " + goal.getStatus());
        }
        goal.activate(approvedBy);
        return GoalResDto.from(goal);
    }

    @Transactional
    public GoalResDto completeGoal(UUID goalId, UUID requesterCompanyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));

        validateSameCompany(goal.getCompanyId(), requesterCompanyId);

        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "ACTIVE 상태의 목표만 완료할 수 있습니다. 현재 상태: " + goal.getStatus());
        }
        goal.complete();
        return GoalResDto.from(goal);
    }

    @Transactional
    public GoalResDto cancelGoal(UUID goalId, UUID requesterCompanyId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "목표를 찾을 수 없습니다. id=" + goalId));

        validateSameCompany(goal.getCompanyId(), requesterCompanyId);

        if (goal.getStatus() == GoalStatus.COMPLETED || goal.getStatus() == GoalStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "이미 완료되거나 취소된 목표는 취소할 수 없습니다. 현재 상태: " + goal.getStatus());
        }
        goal.cancel();
        return GoalResDto.from(goal);
    }

    private void validateSameCompany(UUID goalCompanyId, UUID requesterCompanyId) {
        if (!goalCompanyId.equals(requesterCompanyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 목표에 접근할 수 없습니다.");
        }
    }

    // PUBLIC    : 회사 내 전체 허용
    // TEAM_ONLY : 팀 멤버십은 별도 서비스 조회가 필요하므로 현재는 회사 내 전체 허용
    // PRIVATE   : 목표 소유자(ownerId)만 허용
    private boolean isVisible(Goal goal, UUID requesterId) {
        return switch (goal.getVisibility()) {
            case PUBLIC, TEAM_ONLY -> true;
            case PRIVATE -> goal.getOwnerId().equals(requesterId);
        };
    }
}