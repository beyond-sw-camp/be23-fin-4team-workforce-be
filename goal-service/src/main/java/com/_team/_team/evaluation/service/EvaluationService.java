package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.Evaluation;
import com._team._team.evaluation.domain.EvaluationPolicy;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import com._team._team.evaluation.dto.reqdto.EvaluationCreateReqDto;
import com._team._team.evaluation.dto.reqdto.EvaluationScoreUpdateReqDto;
import com._team._team.evaluation.dto.resdto.EvaluationResDto;
import com._team._team.evaluation.repository.EvaluationPolicyRepository;
import com._team._team.evaluation.repository.EvaluationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EvaluationService {

    final EvaluationRepository evaluationRepository;
    final EvaluationPolicyRepository evaluationPolicyRepository;

    @Autowired
    public EvaluationService(EvaluationRepository evaluationRepository,
                             EvaluationPolicyRepository evaluationPolicyRepository) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationPolicyRepository = evaluationPolicyRepository;
    }

    @Transactional
    public EvaluationResDto createEvaluation(UUID requesterId, UUID companyId, EvaluationCreateReqDto dto) {
        EvaluationPolicy policy = evaluationPolicyRepository.findById(dto.getEvaluationPolicyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 정책을 찾을 수 없습니다."));

        if (!policy.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사 평가 정책에 대한 평가를 생성할 수 없습니다.");
        }

        Evaluation evaluation = Evaluation.builder()
                .evaluationPolicy(policy)
                .evaluatorId(requesterId) // DTO가 아닌 인증된 사용자 ID 사용
                .evaluateeId(dto.getEvaluateeId())
                .evalType(dto.getEvalType())
                .status(EvaluationStatus.DRAFT)
                .build();

        return EvaluationResDto.from(evaluationRepository.save(evaluation));
    }

    @Transactional
    public EvaluationResDto updateScores(UUID evaluationId, UUID requesterId, EvaluationScoreUpdateReqDto dto) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가를 찾을 수 없습니다."));

        // 본인 평가인지 확인
        if (!evaluation.getEvaluatorId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "자신의 평가만 수정할 수 있습니다.");
        }

        if (evaluation.getStatus() != EvaluationStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "초안 상태의 평가만 수정할 수 있습니다.");
        }

        evaluation.updateScores(dto.getQuantScore(), dto.getQualScore(),
                dto.getGoalScoresJson(), dto.getRubricScoresJson(),
                dto.getComment(), dto.getStrengthComment(), dto.getImproveComment());

        return EvaluationResDto.from(evaluation);
    }

    @Transactional
    public EvaluationResDto submitEvaluation(UUID evaluationId, UUID requesterId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가를 찾을 수 없습니다."));

        // 본인 평가인지 확인
        if (!evaluation.getEvaluatorId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "자신의 평가만 제출할 수 있습니다.");
        }

        if (evaluation.getStatus() != EvaluationStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "초안(DRAFT) 상태의 평가만 제출할 수 있습니다. 현재 상태: " + evaluation.getStatus());
        }

        evaluation.submit();
        return EvaluationResDto.from(evaluation);
    }

    @Transactional
    public EvaluationResDto confirmEvaluation(UUID evaluationId, UUID companyId, BigDecimal finalScore, String grade) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가를 찾을 수 없습니다."));

        // 회사 격리 확인
        if (!evaluation.getEvaluationPolicy().getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사 평가를 확정할 수 없습니다.");
        }

        if (evaluation.getStatus() != EvaluationStatus.SUBMITTED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "제출(SUBMITTED) 상태의 평가만 확정할 수 있습니다. 현재 상태: " + evaluation.getStatus());
        }

        evaluation.confirm(finalScore, grade);
        return EvaluationResDto.from(evaluation);
    }

    @Transactional(readOnly = true)
    public EvaluationResDto getEvaluation(UUID evaluationId, UUID requesterId, UUID companyId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가를 찾을 수 없습니다."));

        // 회사 격리 확인
        if (!evaluation.getEvaluationPolicy().getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사 평가를 조회할 수 없습니다.");
        }

        // TODO: 평가 당사자(evaluator, evaluatee) 또는 관리자 권한 확인 로직 추가 가능

        return EvaluationResDto.from(evaluation);
    }

    @Transactional(readOnly = true)
    public List<EvaluationResDto> listByPolicy(UUID policyId, UUID companyId) {
        EvaluationPolicy policy = evaluationPolicyRepository.findById(policyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 정책을 찾을 수 없습니다."));

        if (!policy.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "타사 평가 정책의 평가 목록을 조회할 수 없습니다.");
        }

        return evaluationRepository.findByEvaluationPolicy_EvaluationPolicyId(policyId)
                .stream()
                .map(EvaluationResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvaluationResDto> listByEvaluator(UUID evaluatorId) {
        return evaluationRepository.findByEvaluatorId(evaluatorId)
                .stream()
                .map(EvaluationResDto::from)
                .collect(Collectors.toList());
    }
}
