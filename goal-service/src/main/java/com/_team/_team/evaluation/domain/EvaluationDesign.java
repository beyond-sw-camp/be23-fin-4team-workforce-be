package com._team._team.evaluation.domain;

import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "evaluation_design")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EvaluationDesign extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "design_id")
    private UUID designId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "sections_json", columnDefinition = "JSON")
    private String sectionsJson;

    @Column(name = "grade_config_json", columnDefinition = "JSON")
    private String gradeConfigJson;

    /** 설계 버전 — 수정 시 증가 */
    @Column(name = "template_version")
    private Integer templateVersion;

    /** 기본 템플릿 여부 */
    @Column(name = "is_default_template")
    private Boolean defaultTemplate;

    public void update(String name, String sectionsJson, String gradeConfigJson) {
        this.name = name;
        this.sectionsJson = sectionsJson;
        this.gradeConfigJson = gradeConfigJson;
        this.templateVersion = (this.templateVersion == null ? 1 : this.templateVersion) + 1;
    }

    public void markDefaultTemplate(boolean value) {
        this.defaultTemplate = value;
    }
}
