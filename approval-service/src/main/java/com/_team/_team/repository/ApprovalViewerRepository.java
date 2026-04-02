package com._team._team.repository;

import com._team._team.domain.ApprovalViewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalViewerRepository extends JpaRepository<ApprovalViewer, UUID> {

    List<ApprovalViewer> findByApproval_ApprovalId(UUID approvalId);

    // 내가 참조/공람으로 받은 목록
    List<ApprovalViewer> findByMemberId(UUID memberId);
}
