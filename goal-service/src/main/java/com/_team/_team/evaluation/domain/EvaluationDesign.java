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

    public void update(String name, String sectionsJson, String gradeConfigJson) {
        this.name = name;
        this.sectionsJson = sectionsJson;
        this.gradeConfigJson = gradeConfigJson;
    }
}
