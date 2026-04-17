package com._team._team.salary.repository;

import com._team._team.salary.domain.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, UUID> {

    /** 회사별 급여 목록 조회 */
    List<Salary> findByCompanyId(UUID companyId);

    /** 직원별 급여 이력 조회(회사별) */
    List<Salary> findByMemberIdAndCompanyId(UUID memberId, UUID companyId);

    /**
     * 직원의 특정 날짜 기준 적용 중인 급여 조회
     * - effectiveFrom <= 기준일
     * - effectiveTo가 null(현재 적용 중)이거나 >= 기준일
     */
    @Query("SELECT s FROM Salary s " +
           "WHERE s.memberId = :memberId " +
           "AND s.companyId = :companyId " +
           "AND s.effectiveFrom <= :date " +
           "AND (s.effectiveTo IS NULL OR s.effectiveTo >= :date)")
    Optional<Salary> findActiveSalary(@Param("memberId") UUID memberId,
                                      @Param("companyId") UUID companyId,
                                      @Param("date") LocalDate date);

    // 급여 계산 배치시 회사 ID 조회
    @Query("SELECT DISTINCT s.companyId FROM Salary s")
    List<UUID> findDistinctCompanyIds();
}
