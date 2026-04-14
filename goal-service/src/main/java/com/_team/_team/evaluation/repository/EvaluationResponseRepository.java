package com._team._team.evaluation.repository;

import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.enums.EvalType;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface EvaluationResponseRepository extends JpaRepository<EvaluationResponse, UUID> {
    List<EvaluationResponse> findByGroup_GroupId(UUID groupId);
    List<EvaluationResponse> findByEvaluatorId(UUID evaluatorId);
    List<EvaluationResponse> findByTargetMemberId(UUID targetMemberId);
    List<EvaluationResponse> findByGroup_GroupIdAndStatus(UUID groupId, EvaluationStatus status);
    List<EvaluationResponse> findByGroup_Season_SeasonId(UUID seasonId);

    boolean existsByGroup_GroupIdAndTargetMemberIdAndEvaluatorIdAndEvaluationType(
            UUID groupId,
            UUID targetMemberId,
            UUID evaluatorId,
            EvalType evaluationType);

    long countByGroup_GroupId(UUID groupId);
    long countByGroup_GroupIdAndStatus(UUID groupId, EvaluationStatus status);
    long countByGroup_Season_SeasonId(UUID seasonId);
    long countByGroup_Season_SeasonIdAndStatus(UUID seasonId, EvaluationStatus status);
}
