package com._team._team.approval.service;

import com._team._team.approval.domain.ApprovalDocument;
import com._team._team.approval.domain.ApprovalPolicyLine;
import com._team._team.approval.dtos.reqdto.ApprovalPolicyLineCreateReqDto;
import com._team._team.approval.dtos.resdto.ApprovalPolicyLineResDto;
import com._team._team.approval.repository.ApprovalDocumentRepository;
import com._team._team.approval.repository.ApprovalPolicyLineRepository;
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
public class ApprovalPolicyLineService {
    private final ApprovalPolicyLineRepository policyLineRepository;
    private final ApprovalDocumentRepository approvalDocumentRepository;

    @Autowired
    public ApprovalPolicyLineService(ApprovalPolicyLineRepository policyLineRepository, ApprovalDocumentRepository approvalDocumentRepository) {
        this.policyLineRepository = policyLineRepository;
        this.approvalDocumentRepository = approvalDocumentRepository;
    }

//    결재라인 정책 일괄 저장(전체 교체)
    public List<ApprovalPolicyLineResDto> savePolicyLines(UUID companyId, ApprovalPolicyLineCreateReqDto reqDto){
        ApprovalDocument document = getDocumentOrThrow(reqDto.getDocumentId());
        validateCompanyAccess(document, companyId);

        //    approverType별 교차 검증
        for (ApprovalPolicyLineCreateReqDto.PolicyLineItem item : reqDto.getPolicyLines()) {
            validatePolicyLineItem(item);
        }

        // stepOrder 연속성 검증
        validateStepOrders(reqDto.getPolicyLines());

        // 기존 라인 전체 삭제 후 새로 저장
        List<ApprovalPolicyLine> existingLines =
                policyLineRepository.findByDocumentIdOrderByStepOrder(reqDto.getDocumentId());
        existingLines.forEach(ApprovalPolicyLine::approvalPolicyLineDelete);

        // 새 라인 저장
        List<ApprovalPolicyLine> newLines = reqDto.getPolicyLines().stream()
                .map(item -> ApprovalPolicyLine.builder()
                        .approvalDocument(document)
                        .stepOrder(item.getStepOrder())
                        .approverType(item.getApproverType())
                        .specificMemberPositionId(item.getSpecificMemberPositionId())
                        .jobGradeId(item.getJobGradeId())
                        .jobTitleId(item.getJobTitleId())
                        .build())
                .collect(Collectors.toList());

        List<ApprovalPolicyLine> saved = policyLineRepository.saveAll(newLines);

        return saved.stream()
                .map(ApprovalPolicyLineResDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 양식별 결재라인 정책 조회
    @Transactional(readOnly = true)
    public List<ApprovalPolicyLineResDto> findByDocumentId(UUID companyId, UUID documentId) {
        ApprovalDocument document = getDocumentOrThrow(documentId);
        validateCompanyAccess(document, companyId);

        return policyLineRepository.findByDocumentIdOrderByStepOrder(documentId).stream()
                .map(ApprovalPolicyLineResDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 양식별 결재라인 정책 전체 삭제
    public void deletePolicyLines(UUID companyId, UUID documentId) {
        ApprovalDocument document = getDocumentOrThrow(documentId);
        validateCompanyAccess(document, companyId);

        List<ApprovalPolicyLine> existingLines =
                policyLineRepository.findByDocumentIdOrderByStepOrder(documentId);
        existingLines.forEach(ApprovalPolicyLine::approvalPolicyLineDelete);
    }

    // approverType별 필수 필드 교차 검증
    private void validatePolicyLineItem(ApprovalPolicyLineCreateReqDto.PolicyLineItem item) {
        switch (item.getApproverType()) {
            case SPECIFIC:
                if (item.getSpecificMemberPositionId() == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST,
                            "결재자 유형이 SPECIFIC일 때 specificMemberPositionId는 필수입니다.");
                }
                break;
            case JOB_GRADE:
                if (item.getJobGradeId() == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST,
                            "결재자 유형이 JOB_GRADE일 때 jobGradeId는 필수입니다.");
                }
                break;
            case JOB_TITLE:
                if (item.getJobTitleId() == null) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST,
                            "결재자 유형이 JOB_TITLE일 때 jobTitleId는 필수입니다.");
                }
                break;
        }
    }

    // stepOrder 연속성 및 중복 검증
    private void validateStepOrders(List<ApprovalPolicyLineCreateReqDto.PolicyLineItem> items) {
        List<Integer> orders = items.stream()
                .map(ApprovalPolicyLineCreateReqDto.PolicyLineItem::getStepOrder)
                .sorted()
                .collect(Collectors.toList());

        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i) != i + 1) {
                throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "결재 순서(stepOrder)는 1부터 연속된 값이어야 합니다.");
            }
        }
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
