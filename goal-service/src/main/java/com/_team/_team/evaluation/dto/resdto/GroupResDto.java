package com._team._team.evaluation.dto.resdto;

import com._team._team.evaluation.domain.EvaluationGroup;
import com._team._team.evaluation.domain.converter.EvaluatorMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.util.Collections;
import java.util.List;
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

    /**
     * [D-3] evaluatorMaps 를 raw JSON 문자열 대신 구조화된 리스트로 노출.
     * 프론트 호환을 위해 직렬화된 JSON 도 함께 제공 (evaluatorMapsJson — deprecated).
     */
    private List<EvaluatorMappingResDto> evaluatorMaps;

    /** @deprecated [D-3] 하위 호환용. 신규 코드는 evaluatorMaps 를 사용. */
    @Deprecated
    private String evaluatorMapsJson;

    private static final ObjectMapper OM = new ObjectMapper();

    public static GroupResDto from(EvaluationGroup e, List<EvaluatorMappingResDto> maps) {
        List<EvaluatorMappingResDto> safeMaps = maps != null ? maps : Collections.emptyList();
        String legacyJson;
        try {
            List<EvaluatorMapping> legacyMaps = (e.getEvaluatorMaps() != null)
                    ? e.getEvaluatorMaps()
                    : Collections.emptyList();
            legacyJson = OM.writeValueAsString(legacyMaps);
        } catch (Exception ex) {
            legacyJson = "[]";
        }
        return GroupResDto.builder()
                .groupId(e.getGroupId())
                .companyId(e.getCompanyId())
                .seasonId(e.getSeason().getSeasonId())
                .name(e.getName())
                .evaluationTypesJson(e.getEvaluationTypesJson())
                .targetMemberIdsJson(e.getTargetMemberIdsJson())
                .designId(e.getDesign() != null ? e.getDesign().getDesignId() : null)
                .evaluatorMaps(safeMaps)
                .evaluatorMapsJson(legacyJson)
                .build();
    }
}
