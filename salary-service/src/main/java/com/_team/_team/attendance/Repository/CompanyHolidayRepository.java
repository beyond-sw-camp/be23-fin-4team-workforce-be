package com._team._team.attendance.Repository;


import com._team._team.attendance.domain.CompanyHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 회사 공휴일 Repository
 * - 월간 근태표에 공휴일 표시, 출근 여부 판단에 사용
 */

@Repository
public interface CompanyHolidayRepository extends JpaRepository<CompanyHoliday, UUID> {

    /** 회사 전체 공휴일 목록 (관리자 설정 화면) */
    List<CompanyHoliday> findByCompanyIdAndDelYnOrderByHolidayDate(
            UUID companyId, String delYn);

    /** 특정일이 공휴일인지 확인 (출근 시 상태 판단용) */
    boolean existsByCompanyIdAndHolidayDateAndDelYn(
            UUID companyId, LocalDate date, String delYn);

    /** 회사 특정 공휴일 조회 */
    Optional<CompanyHoliday> findByCompanyIdAndCompanyHolidayIdAndDelYn(UUID companyId, UUID companyHolidayId, String delYn);

    /**
     * 정산기간 내 공휴일 목록 조회
     * 공휴일근무수당 계산: isPaidYn='Y'인 날짜에 출근했는지 확인
     */
    @Query("SELECT ch FROM CompanyHoliday ch " +
            "WHERE ch.companyId = :companyId " +
            "AND ch.holidayDate BETWEEN :from AND :to " +
            "AND ch.delYn = 'N'")
    List<CompanyHoliday> findByCompanyIdAndPeriod(
            @Param("companyId") UUID companyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
