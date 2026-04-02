package com._team._team.repository;

import com._team._team.domain.Request;
import com._team._team.domain.enums.RequestStatus;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequestRepository extends JpaRepository<Request, UUID> {

    // 내가 올린 결재 목록 (DRAFT 제외)
    List<Request> findByMemberIdAndDelYnAndStatusNotOrderByCreatedAtDesc(
            UUID memberId, String delYn, RequestStatus status);

    // 내 임시저장 목록
    List<Request> findByMemberIdAndDelYnAndStatusOrderByUpdatedAtDesc(
            UUID memberId, String delYn, RequestStatus status);

    @Query("SELECT r FROM Request r JOIN FETCH r.approvalDocument " +
            "WHERE r.requestId = :requestId AND r.delYn = 'N'")
    Optional<Request> findByIdWithDocument(@Param("requestId") UUID requestId);
}
