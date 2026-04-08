package com._team._team.attendance.Repository;

import com._team._team.attendance.domain.DailyAttendance;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * 일별 근태 Repository
 *
 * - 월간 조회: daily_attendance만 읽음 (attendance_log JOIN 없음)
 * - 페이징 적용: 전 직원 월간 조회 시 한 번에 전체 로딩 방지
 * - (member_id, attendance_date) UNIQUE 인덱스로 중복 출근 방지
 * - (company_id, attendance_date) 인덱스로 관리자 일별 조회 최적화
 */
@Repository
public interface DailyAttendanceRepository extends JpaRepository<DailyAttendance, UUID> {

    /** 특정 직원의 특정일 근태 (출퇴근 처리 시 조회/생성용) */
    Optional<DailyAttendance> findByCompanyIdAndMemberIdAndAttendanceDate(
            UUID companyId, UUID memberId, LocalDate attendanceDate);

    /**
     * 개인 월간 근태 (직원 본인 조회)
     * - attendance_date로 정렬, 페이징 적용
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
}
