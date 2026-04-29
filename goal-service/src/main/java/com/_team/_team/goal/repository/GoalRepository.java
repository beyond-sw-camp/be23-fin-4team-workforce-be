package com._team._team.goal.repository;

import com._team._team.goal.domain.Goal;
import com._team._team.goal.domain.enums.GoalOwnerType;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.domain.enums.GoalVisibility;
import com._team._team.goal.domain.enums.KpiCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {
    List<Goal> findByCompanyId(UUID companyId);

    List<Goal> findByCompanyIdAndOwnerTypeOrderByCycleStartDateDesc(UUID companyId, GoalOwnerType ownerType);

    List<Goal> findByCompanyIdAndOwnerTypeAndOwnerIdInOrderByCycleStartDateDesc(
            UUID companyId, GoalOwnerType ownerType, Collection<UUID> ownerIds);

    List<Goal> findByCompanyIdAndOwnerTypeAndOwnerIdOrderByCycleStartDateDesc(
            UUID companyId, GoalOwnerType ownerType, UUID ownerId);

    // -----------------------------------------------------------------
    // 내 목표 — ownerId = me 인 목표
    // (participantMemberIds JSON 컬럼 매칭은 Service 에서 보강)
    // -----------------------------------------------------------------
    List<Goal> findByCompanyIdAndOwnerIdOrderByCycleStartDateDesc(UUID companyId, UUID ownerId);

    List<Goal> findByCompanyIdAndOwnerIdAndStatus(UUID companyId, UUID ownerId, GoalStatus status);

    // -----------------------------------------------------------------
    // 전사 목표 — visibility 필터
    // -----------------------------------------------------------------
    List<Goal> findByCompanyIdAndVisibilityOrderByCycleStartDateDesc(
            UUID companyId, GoalVisibility visibility);

    @Query("""
        SELECT g FROM Goal g
        WHERE g.companyId = :companyId
          AND g.visibility IN :visibilities
          AND (:cycle IS NULL OR g.cycle = :cycle)
        ORDER BY g.cycleStartDate DESC
        """)
    List<Goal> findCompanyScope(
            @Param("companyId") UUID companyId,
            @Param("visibilities") Collection<GoalVisibility> visibilities,
            @Param("cycle") KpiCycle cycle);

    // -----------------------------------------------------------------
    // 조직 목표 — ownerId in (조직 멤버 ID list)
    // -----------------------------------------------------------------
    @Query("""
        SELECT g FROM Goal g
        WHERE g.companyId = :companyId
          AND g.ownerType = :ownerType
          AND g.ownerId IN :ownerIds
          AND g.visibility IN :visibilities
        ORDER BY g.cycleStartDate DESC
        """)
    List<Goal> findOrgScope(
            @Param("companyId") UUID companyId,
            @Param("ownerType") GoalOwnerType ownerType,
            @Param("ownerIds") Collection<UUID> ownerIds,
            @Param("visibilities") Collection<GoalVisibility> visibilities);

    // -----------------------------------------------------------------
    // 가중치 100% 검증용 — 같은 (ownerId, cycle, cycleStartDate) 묶음 합
    // -----------------------------------------------------------------
    @Query("""
        SELECT COALESCE(SUM(g.weightPct), 0)
        FROM Goal g
        WHERE g.ownerId = :ownerId
          AND g.cycle = :cycle
          AND g.cycleStartDate = :start
          AND g.status IN :statuses
        """)
    int sumWeight(
            @Param("ownerId") UUID ownerId,
            @Param("cycle") KpiCycle cycle,
            @Param("start") LocalDate start,
            @Param("statuses") Collection<GoalStatus> statuses);

    List<Goal> findByOwnerIdAndCycleAndCycleStartDate(
            UUID ownerId, KpiCycle cycle, LocalDate cycleStartDate);

    List<Goal> findByOwnerIdAndCycleAndCycleStartDateAndStatusIn(
            UUID ownerId, KpiCycle cycle, LocalDate cycleStartDate, Collection<GoalStatus> statuses);

    List<Goal> findByOwnerIdAndCycleAndStatusIn(
            UUID ownerId, KpiCycle cycle, Collection<GoalStatus> statuses);

    // -----------------------------------------------------------------
    // alignment 역참조 — 조직 목표가 어떤 개인 목표들과 연결됐는지
    // -----------------------------------------------------------------
    List<Goal> findByAlignedOrgGoalId(UUID alignedOrgGoalId);
}
