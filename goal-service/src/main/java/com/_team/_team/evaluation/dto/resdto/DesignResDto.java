package com._team._team.evaluation.dto.resdto;

import com._team._team.evaluation.domain.EvaluationDesign;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DesignResDto {
    private UUID designId;
    private UUID companyId;
    private String name;
    private String sectionsJson;
    private String gradeConfigJson;
    /** 화면 표시용 버전 라벨 (예: v2) */
    private String designVersion;
    /** 기본 템플릿 여부 */
    private Boolean defaultTemplate;
    /** 최근 수정 시각 */
    private LocalDateTime updatedAt;

    public static DesignResDto from(EvaluationDesign e) {
        int v = e.getTemplateVersion() == null ? 1 : Math.max(1, e.getTemplateVersion());
        return DesignResDto.builder()
                .designId(e.getDesignId())
                .companyId(e.getCompanyId())
                .name(e.getName())
                .sectionsJson(e.getSectionsJson())
                .gradeConfigJson(e.getGradeConfigJson())
                .designVersion("v" + v)
                .defaultTemplate(Boolean.TRUE.equals(e.getDefaultTemplate()))
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
