package com._team._team.approval.domain;

import com._team._team.approval.domain.enums.ApproverType;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
public class ApprovalPolicyLine extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID policyLineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="document_id", nullable = false)
    private ApprovalDocument approvalDocument;

    @Column(nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApproverType approverType; // JOB_TITLE(직책), JOB_GRADE(직급), SPECIFIC(특정인)

    private UUID specificMemberPositionId; // specific일 때 member_position

    private UUID jobGradeId; // job_grade일 때

    private UUID jobTitleId; // job_title일 때

    @Column(nullable = false)
    @Builder.Default
    private String delYn = "N";

    public void approvalPolicyLineDelete() {
        this.delYn = "Y";
    }


}
