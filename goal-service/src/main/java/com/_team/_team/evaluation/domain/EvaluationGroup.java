package com._team._team.evaluation.domain;

import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "evaluation_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EvaluationGroup extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private EvaluationSeason season;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "evaluation_types_json", columnDefinition = "JSON")
    private String evaluationTypesJson;

    @Column(name = "target_member_ids_json", columnDefinition = "JSON")
    private String targetMemberIdsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private EvaluationDesign design;

    @Column(name = "evaluator_maps_json", columnDefinition = "JSON")
    private String evaluatorMapsJson;

    public void update(String name, String evaluationTypesJson, String targetMemberIdsJson, String evaluatorMapsJson) {
        this.name = name;
        this.evaluationTypesJson = evaluationTypesJson;
        this.targetMemberIdsJson = targetMemberIdsJson;
        this.evaluatorMapsJson = evaluatorMapsJson;
    }

    public void assignDesign(EvaluationDesign design) {
        this.design = design;
    }

    public void updateEvaluatorMaps(String evaluatorMapsJson) {
        this.evaluatorMapsJson = evaluatorMapsJson;
    }
}
