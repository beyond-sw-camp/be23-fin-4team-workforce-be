package com._team._team.meeting.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.meeting.domain.enums.Reaction;
import com._team._team.meeting.domain.enums.RepeatCycle;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "meeting_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MeetingRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "meeting_record_id")
    private UUID meetingRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_record_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private MeetingRecord parentRecord;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "manager_id", nullable = false)
    private UUID managerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_cycle", nullable = false)
    private RepeatCycle repeatCycle;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "agenda", columnDefinition = "TEXT")
    private String agenda;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "private_memo", columnDefinition = "TEXT")
    private String privateMemo;

    @Enumerated(EnumType.STRING)
    @Column(name = "manager_reaction")
    private Reaction managerReaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_reaction")
    private Reaction memberReaction;

    public void complete(String memo, Reaction managerReaction) {
        this.completedAt = LocalDateTime.now();
        this.memo = memo;
        this.managerReaction = managerReaction;
    }

    public void recordMemberReaction(Reaction memberReaction) {
        this.memberReaction = memberReaction;
    }

    public void updateAgenda(String agenda) {
        this.agenda = agenda;
    }

    public void updatePrivateMemo(String privateMemo) {
        this.privateMemo = privateMemo;
    }

    public void updateScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public void updateRepeatCycle(RepeatCycle repeatCycle) {
        this.repeatCycle = repeatCycle;
    }
}
