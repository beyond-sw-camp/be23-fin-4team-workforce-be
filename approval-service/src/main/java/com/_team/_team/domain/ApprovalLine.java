package com._team._team.domain;

import com._team._team.domain.enums.LineStatus;
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

    @Column(columnDefinition = "TEXT")
    private String comment; // 반려 사유 등

    @Column(columnDefinition = "TEXT")
    private String signatureImageUrl; // 전자서명

    /// //

    @Column(length = 64)
    private String documentHash;

    @Builder.Default
    @Column(nullable = false)
    private String isSignedYn = "N"; // 서명 완료 여부

    private LocalDateTime signedAt; // 서명일시

    @Column(length = 45)
    private String ipAddress; // 감사 로그용

    public void approve(String signatureImageUrl, String documentHash, String ipAddress) {
        this.lineStatus = LineStatus.APPROVED;
        this.signatureImageUrl = signatureImageUrl;
        this.documentHash = documentHash;
        this.ipAddress = ipAddress;
        this.isSignedYn = "Y";
        this.signedAt = LocalDateTime.now();
    }

    public void reject(String comment, String ipAddress) {
        this.lineStatus = LineStatus.REJECTED;
        this.comment = comment;
        this.ipAddress = ipAddress;
    }

    public void activate() {
        this.lineStatus = LineStatus.PENDING;
    }




}
