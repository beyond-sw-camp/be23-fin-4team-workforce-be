package com._team._team.evaluation.repository;

import com._team._team.evaluation.domain.PeerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PeerAssignmentRepository extends JpaRepository<PeerAssignment, UUID> {

    List<PeerAssignment> findByEvaluation_EvaluationIdAndEvaluateeId(UUID evaluationId, UUID evaluateeId);

    boolean existsByEvaluation_EvaluationIdAndEvaluateeIdAndPeerMemberId(UUID evaluationId, UUID evaluateeId, UUID peerMemberId);
}
