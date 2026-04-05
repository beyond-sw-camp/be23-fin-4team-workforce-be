package com._team._team.approval.repository;

import com._team._team.approval.domain.Approval;
import com._team._team.approval.domain.ApprovalRequest;
import com._team._team.approval.domain.enums.LineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;



import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, UUID> {

    // 결재 요청별 결재라인 목록 (stepOrder 순)
    List<Approval> findByRequestOrderByStepOrderAsc(ApprovalRequest request);

    // 결재 요청ID별 결재라인 목록
    @Query("SELECT a FROM Approval a " +
            "JOIN FETCH a.request " +
            "WHERE a.request.requestId = :requestId " +
            "ORDER BY a.stepOrder ASC")
    List<Approval> findByRequestIdWithRequest(
            @Param("requestId") UUID requestId);

    // 내가 결재해야 할 대기건 목록 (memberPositionId + PENDING)
    @Query("SELECT a FROM Approval a " +
            "JOIN FETCH a.request r " +
            "JOIN FETCH r.approvalDocument " +
            "WHERE a.memberPositionId = :memberPositionId " +
            "AND a.approvalStatus = :status " +
            "AND r.delYn = 'N'")
    List<Approval> findPendingByMemberPositionId(
            @Param("memberPositionId") UUID memberPositionId,
            @Param("status") LineStatus status);

    // 특정 결재 요청의 특정 단계 조회
    Optional<Approval> findByRequestAndStepOrder(
            ApprovalRequest request, Integer stepOrder);

    // 내가 처리한 결재 이력
    @Query("SELECT a FROM Approval a " +
            "JOIN FETCH a.request r " +
            "JOIN FETCH r.approvalDocument " +
            "WHERE a.memberPositionId = :memberPositionId " +
            "AND a.approvalStatus IN :statuses " +
            "AND r.delYn = 'N' " +
            "ORDER BY a.actedAt DESC")
    List<Approval> findActedByMemberPositionId(
            @Param("memberPositionId") UUID memberPositionId,
            @Param("statuses") List<LineStatus> statuses);

    // 결재 요청별 마지막 단계 번호 조회
    @Query("SELECT MAX(a.stepOrder) FROM Approval a " +
            "WHERE a.request.requestId = :requestId")
    Optional<Integer> findMaxStepOrderByRequestId(
            @Param("requestId") UUID requestId);
}
