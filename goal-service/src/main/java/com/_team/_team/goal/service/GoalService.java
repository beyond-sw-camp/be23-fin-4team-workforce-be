package com._team._team.goal.service;

import com._team._team.dto.BusinessException;
import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.GoalCheckin;
import com._team._team.goal.domain.GoalCycle;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.dto.reqdto.GoalCheckinReqDto;
import com._team._team.goal.dto.reqdto.GoalCreateReqDto;
import com._team._team.goal.dto.resdto.GoalResDto;
import com._team._team.goal.repository.GoalCheckinRepository;
import com._team._team.goal.repository.GoalCycleRepository;
import com._team._team.goal.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class GoalService {

    private final GoalRepository goalRepository;
    private final GoalCycleRepository goalCycleRepository;
    private final GoalCheckinRepository goalCheckinRepository;

    public UUID createGoal(GoalCreateReqDto reqDto) {
        Goal parentGoal = null;
        if (reqDto.getParentGoalId() != null) {
            parentGoal = goalRepository.findById(reqDto.getParentGoalId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "상위 목표를 찾을 수 없습니다."));
        }

        GoalCycle cycle = null;
        if (reqDto.getCycleId() != null) {
            cycle = goalCycleRepository.findById(reqDto.getCycleId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "목표 사이클을 찾을 수 없습니다."));
        }

        Goal goal = Goal.builder()
                .companyId(reqDto.getCompanyId())
                .parentGoal(parentGoal)
                .cycle(cycle)
                .ownerType(reqDto.getOwnerType())
                .ownerId(reqDto.getOwnerId())
                .title(reqDto.getTitle())
                .description(reqDto.getDescription())
                .startDate(reqDto.getStartDate())
                .endDate(reqDto.getEndDate())
                .baseline(reqDto.getBaseline())
                .targetValue(reqDto.getTargetValue())
                .isAutoUpdated(reqDto.getIsAutoUpdated())
                .visibility(reqDto.getVisibility())
                .goalStatus(GoalStatus.DRAFT)
                .build();

        try {
            goal.validateBusinessRule();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        return goalRepository.save(goal).getGoalId();
    }

    @Transactional(readOnly = true)
    public List<GoalResDto> getGoalList(UUID companyId) {
        return goalRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(GoalResDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public GoalResDto getGoalDetail(UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "목표를 찾을 수 없습니다."));
        return GoalResDto.fromEntity(goal);
    }

    public void checkinGoal(UUID memberId, UUID goalId, GoalCheckinReqDto reqDto) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "목표를 찾을 수 없습니다."));

        if (goal.getGoalStatus() == GoalStatus.CANCELLED || goal.getGoalStatus() == GoalStatus.COMPLETED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "종료/취소된 목표는 체크인할 수 없습니다.");
        }

        try {
            goal.updateCheckin(reqDto.getActualValue(), reqDto.getProgressStatus());
        } catch (IllegalStateException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        GoalCheckin goalCheckin = GoalCheckin.builder()
                .goal(goal)
                .memberId(memberId)
                .actualValue(reqDto.getActualValue())
                .progressStatus(reqDto.getProgressStatus())
                .comment(reqDto.getComment())
                .build();
        goalCheckinRepository.save(goalCheckin);
    }
}
