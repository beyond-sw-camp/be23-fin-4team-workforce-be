package com._team._team.evaluation.service;

import com._team._team.dto.BusinessException;
import com._team._team.evaluation.domain.Evaluation;
import com._team._team.evaluation.domain.PeerAssignment;
import com._team._team.evaluation.domain.enums.PeerAssignmentStatus;
import com._team._team.evaluation.dto.reqdto.PeerAssignmentCreateReqDto;
import com._team._team.evaluation.dto.resdto.PeerAssignmentResDto;
import com._team._team.evaluation.repository.EvaluationRepository;
import com._team._team.evaluation.repository.PeerAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PeerAssignmentService {

    final PeerAssignmentRepository peerAssignmentRepository;
    final EvaluationRepository evaluationRepository;

    @Autowired
    public PeerAssignmentService(PeerAssignmentRepository peerAssignmentRepository,
                                 EvaluationRepository evaluationRepository) {
        this.peerAssignmentRepository = peerAssignmentRepository;
        this.evaluationRepository = evaluationRepository;
    }

    @Transactional
    public PeerAssignmentResDto assign(PeerAssignmentCreateReqDto dto, UUID requesterId) {
        boolean duplicate = peerAssignmentRepository.existsByEvaluation_EvaluationIdAndEvaluateeIdAndPeerMemberId(
                dto.getEvaluationId(), dto.getEvaluateeId(), dto.getPeerMemberId());
        if (duplicate) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 동일한 동료 평가 배정이 존재합니다.");
        }

        Evaluation evaluation = evaluationRepository.findById(dto.getEvaluationId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "평가를 찾을 수 없습니다."));

        PeerAssignment peerAssignment = PeerAssignment.builder()
                .evaluation(evaluation)
                .evaluateeId(dto.getEvaluateeId())
                .peerMemberId(dto.getPeerMemberId())
                .assignedBy(requesterId) // DTO가 아닌 인증된 사용자 ID 사용
                .status(PeerAssignmentStatus.PENDING)
                .build();

        return PeerAssignmentResDto.from(peerAssignmentRepository.save(peerAssignment));
    }

    @Transactional(readOnly = true)
    public List<PeerAssignmentResDto> listByEvaluatee(UUID evaluationId, UUID evaluateeId) {
        return peerAssignmentRepository.findByEvaluation_EvaluationIdAndEvaluateeId(evaluationId, evaluateeId)
                .stream()
                .map(PeerAssignmentResDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void complete(UUID peerAssignmentId, UUID requesterId) {
        PeerAssignment peerAssignment = peerAssignmentRepository.findById(peerAssignmentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "동료 평가 배정을 찾을 수 없습니다."));

        if (!peerAssignment.getPeerMemberId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "자신에게 배정된 동료 평가만 완료할 수 있습니다.");
        }

        peerAssignment.complete();
    }

    @Transactional
    public void decline(UUID peerAssignmentId, UUID requesterId) {
        PeerAssignment peerAssignment = peerAssignmentRepository.findById(peerAssignmentId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "동료 평가 배정을 찾을 수 없습니다."));

        if (!peerAssignment.getPeerMemberId().equals(requesterId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "자신에게 배정된 동료 평가만 거절할 수 있습니다.");
        }

        peerAssignment.decline();
    }
}