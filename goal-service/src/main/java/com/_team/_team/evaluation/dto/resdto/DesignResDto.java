package com._team._team.evaluation.dto.resdto;

import com._team._team.evaluation.domain.EvaluationDesign;
import com._team._team.evaluation.domain.converter.EvaluationSection;
import com._team._team.evaluation.domain.converter.GradeConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * [D-4] 평가 설계 응답 DTO.
 *
 * 두 가지 포맷 동시 노출 (점진적 마이그레이션):
 *   - sections / gradeConfig      : 타입 세이프 VO (신규 프론트가 사용)
 *   - sectionsJson / gradeConfigJson : 원본 JSON 문자열 (기존 프론트 호환용)
 *
 * 프론트가 VO 기반 포맷으로 완전히 이관되면 sectionsJson/gradeConfigJson 은 제거 예정.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DesignResDto {

    private static final ObjectMapper OM = new ObjectMapper();

    private UUID designId;
    private UUID companyId;
    private String name;

    /** [D-4] 타입 세이프 섹션 목록 — 신규 포맷 */
    private List<EvaluationSection> sections;

    /** [D-4] 타입 세이프 등급 설정 — 신규 포맷 */
    private GradeConfig gradeConfig;

    /** @deprecated VO 포맷(sections)을 사용해주세요. 프론트 호환용으로 유지. */
    @Deprecated
    private String sectionsJson;

    /** @deprecated VO 포맷(gradeConfig)을 사용해주세요. 프론트 호환용으로 유지. */
    @Deprecated
    private String gradeConfigJson;

    /** 화면 표시용 버전 라벨 (예: v2) */
    private String designVersion;
    /** 기본 템플릿 여부 */
    private Boolean defaultTemplate;
    /** 최근 수정 시각 */
    private LocalDateTime updatedAt;

    public static DesignResDto from(EvaluationDesign e) {
        int v = e.getTemplateVersion() == null ? 1 : Math.max(1, e.getTemplateVersion());
        List<EvaluationSection> sections = e.getSections() != null ? e.getSections() : new ArrayList<>();

        String sectionsJson = "[]";
        try {
            sectionsJson = OM.writeValueAsString(sections);
        } catch (Exception ignore) {}

        String gradeConfigJson = null;
        if (e.getGradeConfig() != null) {
            try {
                gradeConfigJson = OM.writeValueAsString(e.getGradeConfig());
            } catch (Exception ignore) {}
        }

        return DesignResDto.builder()
                .designId(e.getDesignId())
                .companyId(e.getCompanyId())
                .name(e.getName())
                .sections(sections)
                .gradeConfig(e.getGradeConfig())
                .sectionsJson(sectionsJson)
                .gradeConfigJson(gradeConfigJson)
                .designVersion("v" + v)
                .defaultTemplate(Boolean.TRUE.equals(e.getDefaultTemplate()))
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
