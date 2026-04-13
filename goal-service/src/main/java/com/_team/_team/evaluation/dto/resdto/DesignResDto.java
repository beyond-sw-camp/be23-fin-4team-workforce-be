package com._team._team.evaluation.dto.resdto;

import com._team._team.evaluation.domain.EvaluationDesign;
import lombok.*;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DesignResDto {
    private UUID designId;
    private UUID companyId;
    private String name;
    private String sectionsJson;
    private String gradeConfigJson;

    public static DesignResDto from(EvaluationDesign e) {
        return DesignResDto.builder()
                .designId(e.getDesignId())
                .companyId(e.getCompanyId())
                .name(e.getName())
                .sectionsJson(e.getSectionsJson())
                .gradeConfigJson(e.getGradeConfigJson())
                .build();
    }
}
