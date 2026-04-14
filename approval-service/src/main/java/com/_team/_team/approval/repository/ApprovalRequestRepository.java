package com._team._team.approval.repository;

import com._team._team.approval.domain.ApprovalRequest;
import com._team._team.approval.domain.enums.RequestStatus;
import com._team._team.approval.domain.enums.RequestType;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, UUID> {

    // 요청자별 결재 신청 목록 (삭제되지 않은 것만)
    List<ApprovalRequest> findByMemberIdAndDelYn(UUID memberId, String delYn);

    // 요청자별 + 상태별 조회
    List<ApprovalRequest> findByMemberIdAndRequestStatusAndDelYn(
            UUID memberId, RequestStatus requestStatus, String delYn);

    // 요청자별 + 요청타입별 조회
    List<ApprovalRequest> findByMemberIdAndRequestTypeAndDelYn(
            UUID memberId, RequestType requestType, String delYn);

    // fetch join으로 문서 양식 포함 조회
    @Query("SELECT ar FROM ApprovalRequest ar " +
            "JOIN FETCH ar.approvalDocument " +
            "WHERE ar.requestId = :requestId " +
            "AND ar.delYn = 'N'")
    Optional<ApprovalRequest> findByIdWithDocument(
            @Param("requestId") UUID requestId);

    // 기간별 결재 신청 목록 조회
    @Query("SELECT ar FROM ApprovalRequest ar " +
            "JOIN FETCH ar.approvalDocument " +
            "WHERE ar.memberId = :memberId " +
            "AND ar.delYn = 'N' " +
            "AND ar.createdAt BETWEEN :startDate AND :endDate")
    List<ApprovalRequest> findByMemberIdAndDateRange(
            @Param("memberId") UUID memberId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 부서 문서함 - requesterOrganizationId 기준, isDeptVisibleYn='Y', 승인완료/반려만
    @Query("SELECT ar FROM ApprovalRequest ar " +
            "JOIN FETCH ar.approvalDocument doc " +
            "WHERE doc.companyId = :companyId " +
            "AND doc.isDeptVisibleYn = 'Y' " +
            "AND ar.requesterOrganizationId IN :organizationIds " +
            "AND ar.requestStatus IN :statuses " +
            "AND ar.delYn = 'N' " +
            "ORDER BY ar.createdAt DESC")
    List<ApprovalRequest> findDepartmentRequests(
            @Param("companyId") UUID companyId,
            @Param("organizationIds") List<UUID> organizationIds,
            @Param("statuses") List<RequestStatus> statuses);

    // 특정 직원이 특정 날짜에 승인받은 연장근무 종료시각 중 가장 늦은 값
    // 같은 날 여러 건 연장한 경우도 MAX로 잡기 위해 ar.startDateTime 기준 범위 조회
    // 근태 자동 마감(DailyAttendanceCloseService) 때 이 값으로 퇴근시각을 채워 넣는다
    @Query("SELECT MAX(ar.endDateTime) " +
            "FROM ApprovalRequest ar " +
            "JOIN ar.approvalDocument doc " +
            "WHERE ar.memberId = :memberId " +
            "AND doc.documentName = '연장근무신청' " +
            "AND ar.requestStatus = :status " +
            "AND ar.delYn = 'N' " +
            "AND ar.startDateTime >= :startOfDay " +
            "AND ar.startDateTime < :startOfNextDay")
    Optional<LocalDateTime> findLatestApprovedOvertimeEnd(
            @Param("memberId") UUID memberId,
            @Param("status") RequestStatus status,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("startOfNextDay") LocalDateTime startOfNextDay);
}
