package com._team._team.approval.domain;

import com._team._team.approval.domain.enums.LineStatus;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
@Entity
public class Approval extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID approvalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ApprovalRequest request;

    @Column(nullable = false)
    private UUID approverMemberPositionId;

    @Column(nullable = false)
    private UUID approverMemberId;

    @Column(nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LineStatus approvalStatus;

    private LocalDateTime actedAt;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(columnDefinition = "TEXT")
    private String signatureImageUrl;

    @Column(length = 64)
    private String documentHash;

    @Builder.Default
    @Column(nullable = false)
    private String isSignedYn = "N";

    @Column(length = 45)
    private String ipAddress; // 감사 로그용

    public void approve(String comment) {
        this.approvalStatus = LineStatus.APPROVED;
        this.comment = comment;
        this.actedAt = LocalDateTime.now();
    }

    public void reject(String comment) {
        this.approvalStatus = LineStatus.REJECTED;
        this.comment = comment;
        this.actedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.approvalStatus = LineStatus.CANCELED;
        this.actedAt = LocalDateTime.now();
    }

    public void sign(String signatureImageUrl, String documentHash, String ipAddress) {
        this.isSignedYn = "Y";
        this.signatureImageUrl = signatureImageUrl;
        this.documentHash = documentHash;
        this.ipAddress = ipAddress;
    }

    public void activate() {
        this.approvalStatus = LineStatus.PENDING;
    }



}

