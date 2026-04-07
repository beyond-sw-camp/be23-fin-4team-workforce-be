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
    public EvaluationResDto createEvaluation(EvaluationCreateReqDto dto) {
        EvaluationPolicy policy = evaluationPolicyRepository.findById(dto.getEvaluationPolicyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가 정책을 찾을 수 없습니다."));

        Evaluation evaluation = Evaluation.builder()
                .evaluationPolicy(policy)
                .evaluatorId(dto.getEvaluatorId())
                .evaluateeId(dto.getEvaluateeId())
                .evalType(dto.getEvalType())
                .status(EvaluationStatus.DRAFT)
                .build();

        return EvaluationResDto.from(evaluationRepository.save(evaluation));
    }

    @Transactional
    public EvaluationResDto updateScores(UUID evaluationId, EvaluationScoreUpdateReqDto dto) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가를 찾을 수 없습니다."));

        if (evaluation.getStatus() != EvaluationStatus.DRAFT) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "초안 상태의 평가만 수정할 수 있습니다.");
        }

        evaluation.updateScores(dto.getQuantScore(), dto.getQualScore(),
                dto.getGoalScoresJson(), dto.getRubricScoresJson());

        return EvaluationResDto.from(evaluation);
    }

    @Transactional
    public EvaluationResDto submitEvaluation(UUID evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가를 찾을 수 없습니다."));

        evaluation.submit();
        return EvaluationResDto.from(evaluation);
    }

    @Transactional
    public EvaluationResDto confirmEvaluation(UUID evaluationId, BigDecimal finalScore, String grade) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가를 찾을 수 없습니다."));

        evaluation.confirm(finalScore, grade);
        return EvaluationResDto.from(evaluation);
    }

    @Transactional(readOnly = true)
    public EvaluationResDto getEvaluation(UUID evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가를 찾을 수 없습니다."));
        return EvaluationResDto.from(evaluation);
    }

    @Transactional(readOnly = true)
    public List<EvaluationResDto> listByPolicy(UUID policyId) {
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
