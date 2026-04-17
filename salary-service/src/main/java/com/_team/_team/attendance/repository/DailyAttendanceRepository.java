package com._team._team.attendance.repository;

import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.domain.enums.AttendanceStatus;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 일별 근태 Repository
 */
@Repository
public interface DailyAttendanceRepository extends JpaRepository<DailyAttendance, UUID> {

    /** 특정 직원의 특정일 근태 (출퇴근 처리 시 조회/생성용) */
    Optional<DailyAttendance> findByCompanyIdAndMemberIdAndAttendanceDate(
            UUID companyId, UUID memberId, LocalDate attendanceDate);

    /**
     * 개인 월간 근태 (직원 본인 조회)
     */
    @Query("SELECT da FROM DailyAttendance da " +
            "WHERE da.memberId = :memberId " +
            "AND da.attendanceDate BETWEEN :from AND :to " +
            "ORDER BY da.attendanceDate")
    Page<DailyAttendance> findMonthlyByMember(
            @Param("memberId") UUID memberId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    /**
     * 회사 전체 특정일 근태 (관리자 대시보드)
     * - 오늘 출근 현황, 결근자 파악 등
     */
    @Query("SELECT da FROM DailyAttendance da " +
            "WHERE da.companyId = :companyId " +
            "AND da.attendanceDate = :date " +
            "ORDER BY da.memberId")
    Page<DailyAttendance> findDailyByCompany(
            @Param("companyId") UUID companyId,
            @Param("date") LocalDate date,
            Pageable pageable);

    /**
     * 회사 전체 월간 근태 (관리자 리포트)
     * - 직원 수 × 근무일 수만큼 데이터 → 페이징 사용
     * - 100명 × 22일 = 2,200건/월
     */
    @Query("SELECT da FROM DailyAttendance da " +
            "WHERE da.companyId = :companyId " +
            "AND da.attendanceDate BETWEEN :from AND :to " +
            "ORDER BY da.attendanceDate, da.memberId")
    Page<DailyAttendance> findMonthlyByCompany(
            @Param("companyId") UUID companyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    /**
     * 급여 계산용: 직원 1명의 정산기간 내 근태 전체 조회
     * - 1명 × 최대 31일 = 최대 31건
     * - 급여 계산은 전체 기간을 한 번에 봐야 하므로 페이징 부적절
     * - PayrollService.createPayroll() → 초과근무수당/공휴일근무수당 계산 데이터
     */
    @Query("SELECT da FROM DailyAttendance da " +
            "WHERE da.memberId = :memberId " +
            "AND da.companyId = :companyId " +
            "AND da.attendanceDate BETWEEN :from AND :to " +
            "ORDER BY da.attendanceDate")
    List<DailyAttendance> findByMemberAndPeriod(
            @Param("memberId") UUID memberId,
            @Param("companyId") UUID companyId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // 근태 마감 대상 조회 - 출근은 찍혔는데 퇴근이 없는 레코드
    @Query("SELECT da FROM DailyAttendance da " +
            "WHERE da.attendanceDate = :date " +
            "AND da.firstClockIn IS NOT NULL " +
            "AND da.lastClockOut IS NULL")
    List<DailyAttendance> findCloseCandidates(@Param("date") LocalDate date);

    // 휴가(연차/반차)인데 출근 찍힌 케이스 - 관리자 점검/알림용
    // statuses는 보통 LEAVE, HALF 두 개 넘김
    @Query("SELECT da FROM DailyAttendance da " +
            "WHERE da.attendanceDate = :date " +
            "AND da.status IN :statuses " +
            "AND da.firstClockIn IS NOT NULL")
    List<DailyAttendance> findLeaveDateWithClockIn(
            @Param("date") LocalDate date,
            @Param("statuses") List<AttendanceStatus> statuses);

    // 이상감지용 - 해당일 전체 근태 레코드 스캔
    // 회사수 × 인원수 수준이라 페이지 없이 바로 훑음
    @Query("SELECT da FROM DailyAttendance da WHERE da.attendanceDate = :date")
    List<DailyAttendance> findAllByAttendanceDate(@Param("date") LocalDate date);
}
