package com._team._team.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
public class ApprovalLine extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID approvalLineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_id", nullable = false)
    private Approval approval;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_line_id", nullable = false)
    private ApprovalPolicyLine approvalPolicyLine;

    @Column(nullable = false)
    private UUID memberPositionId;

    @Column(nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LineStatus lineStatus;

    private LocalDateTime actedAt;

    @Column(columnDefinition = "TEXT")
    private String comment; // 반려 사유 등

    @Column(columnDefinition = "TEXT")
    private String signatureImageUrl;

    @Column(length = 64)
    private String documentHash;

    @Builder.Default
    @Column(nullable = false)
    private String isSignedYn = "N";

    private LocalDateTime signedAt;

    @Column(length = 45)
    private String ipAddress; // 감사 로그용




}
