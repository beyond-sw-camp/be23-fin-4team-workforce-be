package com._team._team.approval.repository;

import com._team._team.approval.domain.ApprovalDocument;
import com._team._team.approval.domain.enums.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApprovalDocumentRepository extends JpaRepository<ApprovalDocument, UUID> {

    // 회사별 활성 문서 양식 목록
    List<ApprovalDocument> findByCompanyIdAndIsActiveYn(UUID companyId, String isActiveYn);

    // 회사별 + 요청타입으로 문서 양식 조회
    List<ApprovalDocument> findByCompanyIdAndRequestTypeAndIsActiveYn(
            UUID companyId, RequestType requestType, String isActiveYn);

    // 회사별 전체 문서 양식 목록
    List<ApprovalDocument> findByCompanyId(UUID companyId);
}
