package com._team._team.domain;

import com._team._team.domain.enums.ApproverType;
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

    private Integer parallelGroupId; // 동일 값이면 동시 결재, NULL이면 단독

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApproverType approverType; //JOB_TITLE(직책), JOB_GRADE(직급), SPECIFIC(특정인)

    private UUID specificMemberPositionId;
    private UUID jobGradeId;
    private UUID jobTitleId;


}
