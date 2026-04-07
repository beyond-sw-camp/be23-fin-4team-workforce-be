package com._team._team.meeting.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.meeting.domain.enums.TlRating;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meeting_action")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MeetingAction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "meeting_action_id")
    private UUID meetingActionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_record_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private MeetingRecord meetingRecord;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private boolean isCompleted = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "tl_rating")
    private TlRating tlRating;

    @Column(name = "approval_id")
    private UUID approvalId;

    public void complete() {
        this.isCompleted = true;
        this.completedAt = LocalDateTime.now();
    }

    public void rate(TlRating tlRating) {
        this.tlRating = tlRating;
    }

    public void linkApproval(UUID approvalId) {
        this.approvalId = approvalId;
    }
}
