package com._team._team.salary.repository;

import com._team._team.salary.domain.SimplifiedTaxTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SimplifiedTaxTableRepository extends JpaRepository<SimplifiedTaxTable, UUID> {

    // 연도 + 월급여 + 부양가족수 기준 소득세 구간 조회
    @Query("""
           SELECT t FROM SimplifiedTaxTable t
           WHERE t.effectiveYear = :year
             AND t.salaryLowerBound <= :salary
             AND t.salaryUpperBound > :salary
             AND t.dependentCount = :dependents
             AND t.delYn = 'N'
           """)
    Optional<SimplifiedTaxTable> findTaxFor(
            @Param("year") Integer year,
            @Param("salary") Long salary,
            @Param("dependents") Integer dependents);

    // 연도별 삭제, 재업로드 시 기존 데이터 소프트 삭제
    @org.springframework.data.jpa.repository.Modifying
    @Query("""
           UPDATE SimplifiedTaxTable t SET t.delYn = 'Y'
           WHERE t.effectiveYear = :year AND t.delYn = 'N'
           """)
    int softDeleteByYear(@Param("year") Integer year);
}