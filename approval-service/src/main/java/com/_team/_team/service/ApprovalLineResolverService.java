package com._team._team.service;

import com._team._team.domain.ApprovalPolicyLine;
import com._team._team.domain.read.MemberPositionView;
import com._team._team.dtos.resdto.ApproverCandidateDto;
import com._team._team.dtos.resdto.ResolvedApprovalLineDto;
import com._team._team.repository.ApprovalPolicyLineRepository;
import com._team._team.repository.ApprovalDocumentRepository;
import com._team._team.repository.MemberPositionViewRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 결재 정책 라인(템플릿)을 실제 결재자로 변환하는 리졸버 서비스.
 *
 * ApprovalPolicyLine의 approverType에 따라:
 * - SPECIFIC: specificMemberPositionId를 그대로 사용
 * - JOB_TITLE: 요청자 부서에서 해당 직책을 가진 사람을 member-service에 조회
 * - JOB_GRADE: 요청자 부서에서 해당 직급을 가진 사람을 member-service에 조회
 */
@Service
@Transactional(readOnly = true)
public class ApprovalLineResolverService {

    private final ApprovalPolicyLineRepository policyLineRepository;
    private final ApprovalDocumentRepository documentRepository;
    private final MemberPositionViewRepository memberPositionViewRepository;

    public ApprovalLineResolverService(
            ApprovalPolicyLineRepository policyLineRepository,
            ApprovalDocumentRepository documentRepository, MemberPositionViewRepository memberPositionViewRepository) {
        this.policyLineRepository = policyLineRepository;
        this.documentRepository = documentRepository;
        this.memberPositionViewRepository = memberPositionViewRepository;
    }

    public List<ResolvedApprovalLineDto> resolveApprovalLine(UUID documentId, UUID requesterPositionId) {
        // 1. 문서 양식 존재 확인
        documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException("문서양식이 존재하지 않습니다."));

        // 2. 해당 문서의 정책 라인 조회 (stepOrder 순)
        List<ApprovalPolicyLine> policyLines =
                policyLineRepository.findByApprovalDocument_DocumentIdOrderByStepOrderAscPolicyLineIdAsc(documentId);

        if (policyLines.isEmpty()) {
            throw new IllegalStateException("해당 문서에 결재 정책 라인이 설정되어 있지 않습니다.");
        }

        // 3. 요청자 정보 조회 (소속 부서 등)
        MemberPositionView requester = memberPositionViewRepository.findByIdWithDetails(requesterPositionId)
                .orElseThrow(() -> new EntityNotFoundException("요청자 정보를 찾을 수 없습니다."));


        // 4. 각 정책 라인별로 실제 결재자 후보 매핑
        List<ResolvedApprovalLineDto> resolvedLines = new ArrayList<>();

        for (ApprovalPolicyLine policyLine : policyLines) {
            List<ApproverCandidateDto> candidates = resolveCandidates(policyLine, requester);

            resolvedLines.add(ResolvedApprovalLineDto.builder()
                    .policyLineId(policyLine.getPolicyLineId())
                    .stepOrder(policyLine.getStepOrder())
                    .parallelGroupId(policyLine.getParallelGroupId())
                    .approverType(policyLine.getApproverType())
                    .candidates(candidates)
                    .build());
        }

        return resolvedLines;
    }

    /**
     * 정책 라인 하나에 대해 실제 결재자 후보를 찾는다.
     */
    private List<ApproverCandidateDto> resolveCandidates(
            ApprovalPolicyLine policyLine,
            MemberPositionView requester) {

        switch (policyLine.getApproverType()) {
            case SPECIFIC -> {
                return resolveSpecific(policyLine.getSpecificMemberPositionId());
            }
            case JOB_TITLE -> {
                return resolveByJobTitle(requester.getOrganization().getOrganizationId(), policyLine.getJobTitleId());
            }
            case JOB_GRADE -> {
                return resolveByJobGrade(requester.getOrganization().getOrganizationId(), policyLine.getJobGradeId());
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    /**
     * SPECIFIC 타입: 지정된 특정인 조회
     */
    private List<ApproverCandidateDto> resolveSpecific(UUID memberPositionId) {
        return memberPositionViewRepository.findByIdWithDetails(memberPositionId)
                .map(mp -> List.of(toCandidate(mp)))
                .orElse(Collections.emptyList());
    }

    /**
     * JOB_TITLE 타입: 요청자 부서에서 해당 직책자 조회
     */
    private List<ApproverCandidateDto> resolveByJobTitle(UUID organizationId, UUID jobTitleId) {
        List<MemberPositionView> members = memberPositionViewRepository
                .findByOrganizationAndJobTitle(organizationId, jobTitleId);

        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        return members.stream()
                .map(this::toCandidate)
                .collect(Collectors.toList());
    }

    /**
     * JOB_GRADE 타입: 요청자 부서에서 해당 직급자 조회
     */
    private List<ApproverCandidateDto> resolveByJobGrade(UUID organizationId, UUID jobGradeId) {
        List<MemberPositionView> members = memberPositionViewRepository.findByOrganizationAndJobGrade(organizationId, jobGradeId);

        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        return members.stream()
                .map(this::toCandidate)
                .collect(Collectors.toList());
    }

    /**
     * MemberPositionSimpleDto → ApproverCandidateDto 변환
     */
    private ApproverCandidateDto toCandidate(MemberPositionView mp) {
        return ApproverCandidateDto.builder()
                .memberPositionId(mp.getMemberPositionId())
                .memberId(mp.getMember().getMemberId())
                .memberName(mp.getMember().getName())
                .organizationName(mp.getOrganization().getName())
                .jobTitleName(mp.getJobTitle().getName())
                .jobGradeName(mp.getJobGrade().getName())
                .build();
    }
}
