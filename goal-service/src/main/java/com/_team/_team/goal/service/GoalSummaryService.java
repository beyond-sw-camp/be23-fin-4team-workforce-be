package com._team._team.goal.service;

import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.dto.resdto.GoalOrgSummaryResDto;
import com._team._team.goal.repository.GoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GoalSummaryService {

    private final GoalRepository goalRepository;

    public GoalSummaryService(GoalRepository goalRepository) {
        this.goalRepository = goalRepository;
    }

    @Transactional(readOnly = true)
    public List<GoalOrgSummaryResDto> summarizeByOrg(UUID companyId, LocalDate periodStart, LocalDate periodEnd) {
        List<Goal> goals = goalRepository.findByCompanyId(companyId).stream()
                .filter(g -> periodStart == null || !g.getCycleStartDate().isBefore(periodStart))
                .filter(g -> periodEnd == null || !g.getCycleEndDate().isAfter(periodEnd))
                .toList();

        Map<UUID, List<Goal>> byOrg = goals.stream().collect(Collectors.groupingBy(Goal::getOwnerId));
        return byOrg.entrySet().stream()
                .map(entry -> {
                    List<Goal> orgGoals = entry.getValue();
                    BigDecimal avg = orgGoals.stream()
                            .map(Goal::getRolledAchievementPct)
                            .filter(v -> v != null)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    long valid = orgGoals.stream().filter(g -> g.getRolledAchievementPct() != null).count();
                    if (valid > 0) {
                        avg = avg.divide(BigDecimal.valueOf(valid), 2, RoundingMode.HALF_UP);
                    }
                    return GoalOrgSummaryResDto.builder()
                            .orgId(entry.getKey())
                            .goalCount(orgGoals.size())
                            .draftCount(countByStatus(orgGoals, GoalStatus.DRAFT))
                            .activeCount(countByStatus(orgGoals, GoalStatus.ACTIVE))
                            .completedCount(countByStatus(orgGoals, GoalStatus.COMPLETED))
                            .avgRolledAchievementPct(avg)
                            .build();
                })
                .toList();
    }

    private long countByStatus(List<Goal> goals, GoalStatus status) {
        return goals.stream().filter(g -> g.getStatus() == status).count();
    }
}
