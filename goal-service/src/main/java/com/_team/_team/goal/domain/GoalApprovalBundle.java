package com._team._team.goal.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.goal.domain.converter.UuidListConverter;
import com._team._team.goal.domain.enums.BundleApprovalStatus;
import com._team._team.goal.domain.enums.GoalApprovalDecision;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "goal_approval_bundle")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GoalApprovalBundle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "bundle_id")
    private UUID bundleId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private BundleApprovalStatus status = BundleApprovalStatus.PENDING;

    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;

    @Column(name = "approver_id", nullable = false)
    private UUID approverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 32)
    @Builder.Default
    private GoalApprovalDecision decision = GoalApprovalDecision.PENDING;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "comment_text", length = 1000)
    private String commentText;

    @Column(name = "completion_summary", length = 2000)
    private String completionSummary;

    @Column(name = "completion_evidence_files", columnDefinition = "TEXT")
    private String completionEvidenceFiles;

    /** 워처 UUID 목록 — DB에는 JSON 배열로 저장 */
    @Convert(converter = UuidListConverter.class)
    @Column(name = "watcher_ids_json", columnDefinition = "TEXT")
    @Builder.Default
    private List<UUID> watcherIds = new ArrayList<>();

    public void approve(String commentText) {
        this.decision = GoalApprovalDecision.APPROVED;
        this.decidedAt = LocalDateTime.now();
        this.commentText = commentText;
        this.status = BundleApprovalStatus.APPROVED;
    }

    public void reject(String reason) {
        this.decision = GoalApprovalDecision.REJECTED;
        this.decidedAt = LocalDateTime.now();
        this.commentText = reason;
        this.status = BundleApprovalStatus.REJECTED;
        this.rejectionReason = reason;
    }

    public void addWatcher(UUID watcherId) {
        if (!watcherIds.contains(watcherId)) {
            watcherIds.add(watcherId);
        }
    }

    public void addWatchers(List<UUID> newWatcherIds) {
        for (UUID id : newWatcherIds) {
            addWatcher(id);
        }
    }
}
