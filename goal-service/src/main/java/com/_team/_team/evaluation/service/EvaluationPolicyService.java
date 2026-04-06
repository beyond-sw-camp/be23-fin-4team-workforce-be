package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.EvaluationPolicy;
import com._team._team.evaluation.dto.reqdto.EvaluationPolicyCreateReqDto;
import com._team._team.evaluation.dto.resdto.EvaluationPolicyResDto;
import com._team._team.evaluation.repository.EvaluationPolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvaluationPolicyService {

    final EvaluationPolicyRepository evaluationPolicyRepository;

    @Autowired
    public EvaluationPolicyService(EvaluationPolicyRepository evaluationPolicyRepository) {
        this.evaluationPolicyRepository = evaluationPolicyRepository;
    }

    @Transactional
    public EvaluationPolicyResDto createPolicy(EvaluationPolicyCreateReqDto dto) {
        if (!dto.getPeriodEnd().isAfter(dto.getPeriodStart())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 이후여야 합니다.");
        }

        int evaluatorWeightSum = dto.getSelfWeightPct() + dto.getSupervisorWeightPct() + dto.getPeerWeightPct();
        if (evaluatorWeightSum != 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "평가자 가중치(자기·상사·동료)의 합은 100이어야 합니다. 현재 합계: " + evaluatorWeightSum);
        }

        int scoreWeightSum = dto.getQuantWeightPct() + dto.getQualWeightPct();
        if (scoreWeightSum != 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "정량·정성 가중치의 합은 100이어야 합니다. 현재 합계: " + scoreWeightSum);
        }

        if (dto.getPeerCountMin() > dto.getPeerCountMax()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "동료 평가 최소 인원은 최대 인원보다 클 수 없습니다. (min=" + dto.getPeerCountMin() + ", max=" + dto.getPeerCountMax() + ")");
        }

        EvaluationPolicy policy = EvaluationPolicy.builder()
                .companyId(dto.getCompanyId())
                .policyName(dto.getPolicyName())
                .evalCycle(dto.getEvalCycle())
                .periodStart(dto.getPeriodStart())
                .periodEnd(dto.getPeriodEnd())
                .resultOpenDate(dto.getResultOpenDate())
                .editAllowedDays(dto.getEditAllowedDays())
                .quantWeightPct(dto.getQuantWeightPct())
                .qualWeightPct(dto.getQualWeightPct())
                .selfWeightPct(dto.getSelfWeightPct())
                .supervisorWeightPct(dto.getSupervisorWeightPct())
                .peerWeightPct(dto.getPeerWeightPct())
                .gradeType(dto.getGradeType())
                .gradeConfigJson(dto.getGradeConfigJson())
                .approvalRequired(dto.isApprovalRequired())
                .biasCheckEnabled(dto.isBiasCheckEnabled())
                .peerCountMin(dto.getPeerCountMin())
                .peerCountMax(dto.getPeerCountMax())
                .isActive(true)
                .build();

        return EvaluationPolicyResDto.from(evaluationPolicyRepository.save(policy));
    }

    @Transactional(readOnly = true)
    public List<EvaluationPolicyResDto> listPolicies(UUID companyId) {
        return evaluationPolicyRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(EvaluationPolicyResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvaluationPolicyResDto> listActivePolicies(UUID companyId) {
        return evaluationPolicyRepository.findByCompanyIdAndIsActiveTrueOrderByCreatedAtDesc(companyId)
                .stream()
                .map(EvaluationPolicyResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivatePolicy(UUID policyId) {
        EvaluationPolicy policy = evaluationPolicyRepository.findById(policyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 정책을 찾을 수 없습니다."));
        policy.deactivate();
    }
}
