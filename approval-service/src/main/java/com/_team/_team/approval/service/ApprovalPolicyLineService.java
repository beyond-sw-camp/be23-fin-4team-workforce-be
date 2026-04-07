package com._team._team.approval.service;

import com._team._team.approval.domain.ApprovalDocument;
import com._team._team.approval.domain.ApprovalPolicyLine;
import com._team._team.approval.dto.reqdto.ApprovalPolicyLineCreateReqDto;
import com._team._team.approval.dto.resdto.ApprovalPolicyLineResDto;
import com._team._team.approval.dto.resdto.PolicyLineCandidateResDto;
import com._team._team.approval.feignclients.MemberServiceFeignClient;
import com._team._team.approval.feignclients.dto.MemberPositionResDto;
import com._team._team.approval.repository.ApprovalDocumentRepository;
import com._team._team.approval.repository.ApprovalPolicyLineRepository;
import com._team._team.dto.ApiResponse;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ApprovalPolicyLineService {
    private final ApprovalPolicyLineRepository policyLineRepository;
    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final MemberServiceFeignClient memberServiceFeignClient;

    @Autowired
    public ApprovalPolicyLineService(ApprovalPolicyLineRepository policyLineRepository, ApprovalDocumentRepository approvalDocumentRepository, MemberServiceFeignClient memberServiceFeignClient) {
        this.policyLineRepository = policyLineRepository;
        this.approvalDocumentRepository = approvalDocumentRepository;
        this.memberServiceFeignClient = memberServiceFeignClient;
    }

//    결재라인 정책 일괄 저장(전체 교체)
    public List<ApprovalPolicyLineResDto> savePolicyLines(UUID companyId, ApprovalPolicyLineCreateReqDto reqDto){
        ApprovalDocument document = approvalDocumentRepository.findById(reqDto.getDocumentId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        // 직급 중복 검증
        List<UUID> jobGradeIds = reqDto.getPolicyLines().stream()
                .map(ApprovalPolicyLineCreateReqDto.PolicyLineItem::getJobGradeId)
                .toList();

        if (jobGradeIds.size() != jobGradeIds.stream().distinct().count()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "동일한 직급을 중복 지정할 수 없습니다.");
        }


        // 기존 라인 전체 삭제 후 새로 저장
        List<ApprovalPolicyLine> existingLines =
                policyLineRepository.findByDocumentId(reqDto.getDocumentId());
        existingLines.forEach(ApprovalPolicyLine::approvalPolicyLineDelete);

        // 새 라인 저장
        List<ApprovalPolicyLine> newLines = reqDto.getPolicyLines().stream()
                .map(item -> ApprovalPolicyLine.builder()
                        .approvalDocument(document)
                        .jobGradeId(item.getJobGradeId())
                        .build())
                .toList();


        List<ApprovalPolicyLine> saved = policyLineRepository.saveAll(newLines);

        return saved.stream()
                .map(ApprovalPolicyLineResDto::fromEntity)
                .toList();
    }

    // 양식별 결재라인 정책 조회
    @Transactional(readOnly = true)
    public List<ApprovalPolicyLineResDto> findByDocumentId(UUID companyId, UUID documentId) {
        ApprovalDocument document = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        return policyLineRepository.findByDocumentId(documentId).stream()
                .map(ApprovalPolicyLineResDto::fromEntity)
                .toList();
    }

    // 양식별 결재라인 정책 전체 삭제
    public void deletePolicyLines(UUID companyId, UUID documentId) {
        ApprovalDocument document = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        List<ApprovalPolicyLine> existingLines =
                policyLineRepository.findByDocumentId(documentId);
        existingLines.forEach(ApprovalPolicyLine::approvalPolicyLineDelete);
    }

    // 양식별 결재라인 정책 조회 + 후보 결재자 목록 (요청자 기준 필터링)
    @Transactional(readOnly = true)
    public List<PolicyLineCandidateResDto> findCandidatesByDocumentId(
            UUID companyId, UUID documentId, UUID memberPositionId) {

        ApprovalDocument document = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "결재 양식을 찾을 수 없습니다."));

        if (!document.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        // 1. 요청자 MemberPosition 조회 (소속 조직, 직급 정보)
        ApiResponse<MemberPositionResDto> memberResponse =
                memberServiceFeignClient.getMemberPosition(memberPositionId);
        MemberPositionResDto requester = memberResponse.getData();

        if (requester == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND,
                    "요청자 정보를 찾을 수 없습니다.");
        }

        // 2. PolicyLine에서 직급 목록 가져오기
        List<ApprovalPolicyLine> policyLines =
                policyLineRepository.findByDocumentId(documentId);

        // 3. 각 직급별로 요청자 소속 조직에서 후보 결재자 조회
        List<PolicyLineCandidateResDto> result = new ArrayList<>();

        for (ApprovalPolicyLine policyLine : policyLines) {
            ApiResponse<List<MemberPositionResDto>> candidateResponse =
                    memberServiceFeignClient.searchByJobGrade(
                            requester.getOrganizationId(),
                            policyLine.getJobGradeId());

            List<MemberPositionResDto> found = candidateResponse.getData();
            List<MemberPositionResDto> candidates = new ArrayList<>();

            if (found != null && !found.isEmpty()) {
                // 요청자 본인 제외
                candidates = found.stream()
                        .filter(c -> !c.getMemberPositionId().equals(memberPositionId))
                        .toList();
            }

            String jobGradeName = candidates.isEmpty()
                    ? null
                    : candidates.get(0).getJobGradeName();

            result.add(PolicyLineCandidateResDto.builder()
                    .policyLineId(policyLine.getPolicyLineId())
                    .documentId(documentId)
                    .jobGradeId(policyLine.getJobGradeId())
                    .jobGradeName(jobGradeName)
                    .candidates(candidates)
                    .build());
        }

        return result;
    }

}
