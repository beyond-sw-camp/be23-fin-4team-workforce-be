package com._team._team.attendance.Repository;

import com._team._team.attendance.domain.WorkSchedule;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, UUID> {

    List<WorkSchedule> findByCompanyIdAndDelYn(UUID companyId, String delYn);
    Optional<WorkSchedule> findByCompanyIdAndScheduleIdAndDelYn(UUID companyId, UUID scheduleId, String delYn);
    /**
     * 특정 날짜에 유효한 스케줄 조회
     * - memberId 매칭 우선, null(회사 기본)은 후순위
     * - ORDER BY memberId DESC NULLS LAST: 개인 스케줄이 먼저 나옴
     * - effectiveFrom <= date, effectiveTo가 null이거나 >= date
     */
    @Query("SELECT ws FROM WorkSchedule ws " +
            "WHERE ws.companyId = :companyId " +
            "AND (ws.memberId = :memberId OR ws.memberId IS NULL) " +
            "AND ws.effectiveFrom <= :date " +
            "AND (ws.effectiveTo IS NULL OR ws.effectiveTo >= :date) " +
            "AND ws.delYn = 'N' " +
            "ORDER BY ws.memberId DESC NULLS LAST")
    List<WorkSchedule> findActiveSchedules(
            @Param("companyId") UUID companyId,
            @Param("memberId") UUID memberId,
            @Param("date") LocalDate date);

}
