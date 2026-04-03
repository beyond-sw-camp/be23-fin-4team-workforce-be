package com._team._team.service;

import com._team._team.domain.ApprovalDocument;
import com._team._team.domain.ApprovalPolicyLine;
import com._team._team.dtos.reqdto.ApprovalPolicyLineCreateDto;
import com._team._team.dtos.resdto.ApprovalPolicyLineResponseDto;
import com._team._team.repository.ApprovalDocumentRepository;
import com._team._team.repository.ApprovalPolicyLineRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ApprovalPolicyLineService {
    private final ApprovalPolicyLineRepository policyLineRepository;
    private final ApprovalDocumentRepository documentRepository;

    public ApprovalPolicyLineService(ApprovalPolicyLineRepository policyLineRepository, ApprovalDocumentRepository documentRepository) {
        this.policyLineRepository = policyLineRepository;
        this.documentRepository = documentRepository;
    }

    public void createPolicyLine(ApprovalPolicyLineCreateDto dto) {
        // approverType별 필드 검증
        validateApproverField(dto);

        ApprovalDocument document = documentRepository.findById(dto.getDocumentId())
                .orElseThrow(() -> new EntityNotFoundException("문서양식이 존재하지 않습니다."));

        // 동일 document + stepOrder 중복 체크 (병렬그룹 없는 단독 결재 기준)
        if (dto.getParallelGroupId() == null) {
            boolean exists = policyLineRepository.existsByApprovalDocumentAndStepOrderAndParallelGroupIdIsNull(
                    document, dto.getStepOrder());
            if (exists) throw new IllegalArgumentException("해당 stepOrder가 이미 존재합니다.");
        }

        ApprovalPolicyLine policyLine = ApprovalPolicyLine.builder()
                .approvalDocument(document)
                .stepOrder(dto.getStepOrder())
                .parallelGroupId(dto.getParallelGroupId())
                .approverType(dto.getApproverType())
                .specificMemberPositionId(dto.getSpecificMemberPositionId())
                .jobGradeId(dto.getJobGradeId())
                .jobTitleId(dto.getJobTitleId())
                .build();

        policyLineRepository.save(policyLine);
    }

    private void validateApproverField(ApprovalPolicyLineCreateDto dto) {
        switch (dto.getApproverType()) {
            case SPECIFIC -> {
                if (dto.getSpecificMemberPositionId() == null)
                    throw new IllegalArgumentException("SPECIFIC 타입은 specificMemberPositionId가 필수입니다.");
            }
            case JOB_GRADE -> {
                if (dto.getJobGradeId() == null)
                    throw new IllegalArgumentException("JOB_GRADE 타입은 jobGradeId가 필수입니다.");
            }
            case JOB_TITLE -> {
                if (dto.getJobTitleId() == null)
                    throw new IllegalArgumentException("JOB_TITLE 타입은 jobTitleId가 필수입니다.");
            }
        }
    }

    // 정책 리스트 조회
    @Transactional(readOnly = true)
    public List<ApprovalPolicyLineResponseDto> getPolicyLines(UUID documentId){
        documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("문서양식이 존재하지 않습니다."));

        return policyLineRepository
                .findByApprovalDocument_DocumentIdOrderByStepOrderAscPolicyLineIdAsc(documentId)
                .stream()
                .map(ApprovalPolicyLineResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 정책 단건 조회
    @Transactional(readOnly = true)
    public ApprovalPolicyLineResponseDto getPolicyLine(UUID policyLineId){
        ApprovalPolicyLine policyLine = policyLineRepository.findById(policyLineId)
                .orElseThrow(() -> new EntityNotFoundException("결재 정책 라인이 존재하지 않습니다."));

        return ApprovalPolicyLineResponseDto.fromEntity(policyLine);
    }






}
