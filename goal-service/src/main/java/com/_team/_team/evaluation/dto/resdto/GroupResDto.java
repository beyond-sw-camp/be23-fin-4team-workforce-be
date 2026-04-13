package com._team._team.evaluation.dto.resdto;

import com._team._team.evaluation.domain.EvaluationGroup;
import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GroupResDto {
    private UUID groupId;
    private UUID companyId;
    private UUID seasonId;
    private String name;
    private String evaluationTypesJson;
    private String targetMemberIdsJson;
    private UUID designId;
    private String evaluatorMapsJson;

    public static GroupResDto from(EvaluationGroup e) {
        return GroupResDto.builder()
                .groupId(e.getGroupId())
                .companyId(e.getCompanyId())
                .seasonId(e.getSeason().getSeasonId())
                .name(e.getName())
                .evaluationTypesJson(e.getEvaluationTypesJson())
                .targetMemberIdsJson(e.getTargetMemberIdsJson())
                .designId(e.getDesign() != null ? e.getDesign().getDesignId() : null)
                .evaluatorMapsJson(e.getEvaluatorMapsJson())
                .build();
    }
}
