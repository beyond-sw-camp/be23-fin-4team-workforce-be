package com._team._team.evaluation.domain;

import com._team._team.evaluation.domain.enums.PeerAssignmentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "peer_assignment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PeerAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "peer_assignment_id", columnDefinition = "BINARY(16)")
    private UUID peerAssignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Evaluation evaluation;

    @Column(name = "evaluatee_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID evaluateeId;

    @Column(name = "peer_member_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID peerMemberId;

    @Column(name = "assigned_by", columnDefinition = "BINARY(16)", nullable = false)
    private UUID assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PeerAssignmentStatus status;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public void complete() {
        this.status = PeerAssignmentStatus.COMPLETED;
    }

    public void decline() {
        this.status = PeerAssignmentStatus.DECLINED;
    }

    public void markNotified() {
        this.notifiedAt = LocalDateTime.now();
    }
}
