package com._team._team.approval.service;

import com._team._team.approval.domain.ApprovalDocument;
import com._team._team.approval.dtos.reqdto.ApprovalDocumentCreateReqDto;
import com._team._team.approval.dtos.resdto.ApprovalDocumentResDto;
import com._team._team.approval.repository.ApprovalDocumentRepository;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApprovalDocumentService {

    private final ApprovalDocumentRepository approvalDocumentRepository;

    @Autowired
    public ApprovalDocumentService(ApprovalDocumentRepository approvalDocumentRepository) {
        this.approvalDocumentRepository = approvalDocumentRepository;
    }

    // 결재 양식 생성
    public ApprovalDocumentResDto create(UUID companyId, ApprovalDocumentCreateReqDto reqDto) {

        ApprovalDocument document = reqDto.toEntity(companyId);
        ApprovalDocument saved = approvalDocumentRepository.save(document);
        return ApprovalDocumentResDto.fromEntity(saved);
    }

    // 결재 양식 단건 조회
    @Transactional(readOnly = true)
    public ApprovalDocumentResDto findById(UUID companyId, UUID documentId) {
        ApprovalDocument document = getDocumentOrThrow(documentId);
        validateCompanyAccess(document, companyId);
        return ApprovalDocumentResDto.fromEntity(document);
    }

    // 회사별 활성 양식 목록 조회
    @Transactional(readOnly = true)
    public List<ApprovalDocumentResDto> findActiveByCompanyId(UUID companyId) {
        return approvalDocumentRepository
                .findByCompanyIdAndIsActiveYn(companyId, "Y")
                .stream()
                .map(ApprovalDocumentResDto::fromEntity)
                .toList();
    }

    // 회사별 전체 양식 목록 조회 (관리자용)
    @Transactional(readOnly = true)
    public List<ApprovalDocumentResDto> findAllByCompanyId(UUID companyId) {
        return approvalDocumentRepository
                .findByCompanyId(companyId)
                .stream()
                .map(ApprovalDocumentResDto::fromEntity)
                .toList();
    }

    // 결재 양식 활성화
    public ApprovalDocumentResDto activate(UUID companyId, UUID documentId) {
        ApprovalDocument document = getDocumentOrThrow(documentId);
        validateCompanyAccess(document, companyId);
        document.activate();
        return ApprovalDocumentResDto.fromEntity(document);
    }

    // 결재 양식 비활성화
    public ApprovalDocumentResDto deactivate(UUID companyId, UUID documentId) {
        ApprovalDocument document = getDocumentOrThrow(documentId);
        validateCompanyAccess(document, companyId);
        document.deactivate();
        return ApprovalDocumentResDto.fromEntity(document);
    }

    private ApprovalDocument getDocumentOrThrow(UUID documentId) {
        return approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));
    }

    private void validateCompanyAccess(ApprovalDocument document, UUID companyId) {
        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }
    }
}
