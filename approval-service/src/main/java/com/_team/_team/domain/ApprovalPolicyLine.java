package com._team._team.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    private Integer parallelGroupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApproverType approverType;

    private UUID specificMemberPositionId;
    private UUID jobGradeId;
    private UUID jobTitleId;


}
