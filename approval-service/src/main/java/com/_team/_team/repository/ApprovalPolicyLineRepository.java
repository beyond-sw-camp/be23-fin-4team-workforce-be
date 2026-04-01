package com._team._team.repository;

import com._team._team.domain.ApprovalDocument;
import com._team._team.domain.ApprovalPolicyLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApprovalPolicyLineRepository extends JpaRepository<ApprovalPolicyLine, UUID> {
    boolean existsByApprovalDocumentAndStepOrderAndParallelGroupIdIsNull(
            ApprovalDocument approvalDocument, Integer stepOrder);

    List<ApprovalPolicyLine> findByApprovalDocument_DocumentIdOrderByStepOrderAscPolicyLineIdAsc(UUID documentId);

}
