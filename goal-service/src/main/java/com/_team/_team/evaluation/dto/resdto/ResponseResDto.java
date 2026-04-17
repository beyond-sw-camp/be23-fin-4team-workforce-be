package com._team._team.evaluation.dto.resdto;

import com._team._team.evaluation.domain.EvaluationResponse;
import com._team._team.evaluation.domain.enums.EvalType;
import com._team._team.evaluation.domain.enums.EvaluationStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ResponseResDto {
    private UUID responseId;
    private UUID companyId;
    /** 평가 그룹이 속한 시즌 — 클라이언트가 그룹/설계 조회 시 경로 확보용 */
    private UUID seasonId;
    private UUID groupId;
    /** 그룹에 연결된 평가 설계 — 작성 화면에서 문항 로드용 */
    private UUID designId;
    private UUID targetMemberId;
    private UUID evaluatorId;
    private EvalType evaluationType;
    private EvaluationStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime lastRemindedAt;
    private String answersJson;
    private String calibrationJson;
    /** 평가 시즌 시작 시점에 캡처된 타깃 목표 스냅샷 (List&lt;GoalSnapshotDto&gt; 직렬화). */
    private String goalSnapshotJson;
    private String targetGoalIdsJson;

    public static ResponseResDto from(EvaluationResponse e) {
        return ResponseResDto.builder()
                .responseId(e.getResponseId())
                .companyId(e.getCompanyId())
                .seasonId(e.getGroup().getSeason().getSeasonId())
                .groupId(e.getGroup().getGroupId())
                .designId(e.getGroup().getDesign() != null ? e.getGroup().getDesign().getDesignId() : null)
                .targetMemberId(e.getTargetMemberId())
                .evaluatorId(e.getEvaluatorId())
                .evaluationType(e.getEvaluationType())
                .status(e.getStatus())
                .submittedAt(e.getSubmittedAt())
                .lastRemindedAt(e.getLastRemindedAt())
                .answersJson(e.getAnswersJson())
                .calibrationJson(e.getCalibrationJson())
                .goalSnapshotJson(e.getGoalSnapshotJson())
                .targetGoalIdsJson(e.getTargetGoalIdsJson())
                .build();
    }
}
