package com._team._team.repository;

import com._team._team.domain.ApprovalLine;
import com._team._team.domain.enums.LineStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, UUID> {

    // 특정 결재 건의 결재선 목록 (순서대로)
    List<ApprovalLine> findByApproval_ApprovalIdOrderByStepOrderAsc(UUID approvalId);

    // 특정 결재 건 + 특정 단계의 결재선
    List<ApprovalLine> findByApproval_ApprovalIdAndStepOrder(UUID approvalId, Integer stepOrder);

    // 내가 결재해야 할 목록
    List<ApprovalLine> findByMemberPositionIdAndLineStatus(UUID memberPositionId, LineStatus lineStatus);
}
