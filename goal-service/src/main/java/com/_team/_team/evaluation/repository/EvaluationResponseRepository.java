package com._team._team.evaluation.repository;

import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.enums.EvalType;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 평가자가 해당 설계를 사용하는 그룹에 배정된 응답이 하나라도 있는지(작성 화면에서 설계 조회 허용 판별).
     */
    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM EvaluationResponse r
            JOIN r.group g
            JOIN g.design d
            WHERE r.evaluatorId = :evaluatorId
              AND r.companyId = :companyId
              AND d.designId = :designId
            """)
    boolean existsEvaluatorAssignmentForDesign(
            @Param("evaluatorId") UUID evaluatorId,
            @Param("companyId") UUID companyId,
            @Param("designId") UUID designId);
}
