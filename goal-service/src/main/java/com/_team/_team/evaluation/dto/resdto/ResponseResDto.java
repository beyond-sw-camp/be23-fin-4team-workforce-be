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
    private UUID groupId;
    private UUID targetMemberId;
    private UUID evaluatorId;
    private EvalType evaluationType;
    private EvaluationStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime lastRemindedAt;
    private String answersJson;
    private String calibrationJson;

    public static ResponseResDto from(EvaluationResponse e) {
        return ResponseResDto.builder()
                .responseId(e.getResponseId())
                .companyId(e.getCompanyId())
                .groupId(e.getGroup().getGroupId())
                .targetMemberId(e.getTargetMemberId())
                .evaluatorId(e.getEvaluatorId())
                .evaluationType(e.getEvaluationType())
                .status(e.getStatus())
                .submittedAt(e.getSubmittedAt())
                .lastRemindedAt(e.getLastRemindedAt())
                .answersJson(e.getAnswersJson())
                .calibrationJson(e.getCalibrationJson())
                .build();
    }
}
