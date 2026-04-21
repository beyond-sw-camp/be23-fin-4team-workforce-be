package com._team._team.attendance.repository;

import com._team._team.attendance.domain.LeaveRequest;
import com._team._team.attendance.domain.enums.LeaveApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveRequestRepository  extends JpaRepository<LeaveRequest, UUID> {

    // 본인 휴가 이력, 최근 신청 순
    Page<LeaveRequest> findAllByMemberIdAndCompanyIdAndDelYnOrderByRequestedAtDesc(
            UUID memberId, UUID companyId, String delYn, Pageable pageable);

    // 본인 특정일 휴가 신청 목록 (같은 날 여러 건 가능)
    List<LeaveRequest> findAllByMemberIdAndStartDateAndDelYn(
            UUID memberId, LocalDate startDate, String delYn);

    // Kafka Consumer 역조회 (결재 ID 로)
    Optional<LeaveRequest> findByApprovalRequestId(UUID approvalRequestId);

    // 본인 소유 휴가인지 검증
    Optional<LeaveRequest> findByLeaveRequestIdAndMemberId(
            UUID leaveRequestId, UUID memberId);

    // 관리자 상태별 목록, 오래된 순
    List<LeaveRequest> findAllByCompanyIdAndApprovalStatusAndDelYnOrderByRequestedAtAsc(
            UUID companyId, LeaveApprovalStatus status, String delYn);

    /**
     * 근태 마감 배치용, 특정 날짜에 적용되는 승인된 휴가 조회
     */
    @Query("""
           SELECT l FROM LeaveRequest l
           WHERE l.companyId = :companyId
             AND l.memberId = :memberId
             AND l.approvalStatus = 'APPROVED'
             AND l.startDate <= :date
             AND l.endDate >= :date
             AND l.delYn = 'N'
           """)
    Optional<LeaveRequest> findActiveOnDate(
            @Param("companyId") UUID companyId,
            @Param("memberId") UUID memberId,
            @Param("date") LocalDate date);

    /**
     * 연간 사용량 체크
     */
    @Query("""
           SELECT COALESCE(SUM(l.usageDays), 0)
           FROM LeaveRequest l
           WHERE l.memberId = :memberId
             AND l.companyId = :companyId
             AND l.companyLeaveTypeId = :companyLeaveTypeId
             AND l.approvalStatus = 'APPROVED'
             AND EXTRACT(YEAR FROM l.startDate) = :year
             AND l.delYn = 'N'
           """)
    Double sumUsedDaysInYear(
            @Param("memberId") UUID memberId,
            @Param("companyId") UUID companyId,
            @Param("companyLeaveTypeId") UUID companyLeaveTypeId,
            @Param("year") int year);

    /**
     * 월별 휴가 집계
     */
    @Query("""
           SELECT l.companyLeaveTypeId, SUM(l.usageDays)
           FROM LeaveRequest l
           WHERE l.memberId = :memberId
             AND l.approvalStatus = 'APPROVED'
             AND l.startDate <= :endOfMonth
             AND l.endDate >= :startOfMonth
             AND l.delYn = 'N'
           GROUP BY l.companyLeaveTypeId
           """)
    List<Object[]> groupByTypeInMonth(
            @Param("memberId") UUID memberId,
            @Param("startOfMonth") LocalDate startOfMonth,
            @Param("endOfMonth") LocalDate endOfMonth);

    // 단건 조회 + 회사 검증 한 번에
    Optional<LeaveRequest> findByLeaveRequestIdAndCompanyIdAndDelYn(
            UUID leaveRequestId, UUID companyId, String delYn);
}
