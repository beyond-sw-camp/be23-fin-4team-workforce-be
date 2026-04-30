package com._team._team.goal.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.goal.domain.converter.UuidListConverter;
import com._team._team.goal.domain.enums.GoalApprovalStatus;
import com._team._team.goal.domain.enums.GoalHealthStatus;
import com._team._team.goal.domain.enums.GoalOwnerType;
import com._team._team.goal.domain.enums.GoalStatus;
import com._team._team.goal.domain.enums.GoalVisibility;
import com._team._team.goal.domain.enums.KpiCycle;
import com._team._team.goal.domain.enums.RollupSource;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "goal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Goal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "goal_id")
    private UUID goalId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 32)
    private GoalOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "aligned_org_goal_id")
    private UUID alignedOrgGoalId;

    @Column(name = "title", length = 300, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle", nullable = false, length = 32)
    private KpiCycle cycle;

    @Column(name = "cycle_start_date", nullable = false)
    private LocalDate cycleStartDate;

    @Column(name = "cycle_end_date", nullable = false)
    private LocalDate cycleEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 32)
    private GoalVisibility visibility;

    @Column(name = "weight_pct", nullable = false)
    @Builder.Default
    private int weightPct = 0;

    @Column(name = "grade_s_criteria", columnDefinition = "TEXT")
    private String gradeSCriteria;

    @Column(name = "grade_a_criteria", columnDefinition = "TEXT")
    private String gradeACriteria;

    @Column(name = "grade_b_criteria", columnDefinition = "TEXT")
    private String gradeBCriteria;

    @Column(name = "grade_c_criteria", columnDefinition = "TEXT")
    private String gradeCCriteria;

    @Convert(converter = UuidListConverter.class)
    @Column(name = "visible_team_ids_json", columnDefinition = "TEXT")
    @Builder.Default
    private List<UUID> visibleTeamIds = new ArrayList<>();

    @Convert(converter = UuidListConverter.class)
    @Column(name = "participant_member_ids_json", columnDefinition = "TEXT")
    @Builder.Default
    private List<UUID> participantMemberIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private GoalStatus status = GoalStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_approval_status", nullable = false, length = 32)
    @Builder.Default
    private GoalApprovalStatus goalApprovalStatus = GoalApprovalStatus.NOT_REQUESTED;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "actual_value", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal actualValue = BigDecimal.ZERO;

    @Column(name = "achievement_pct", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal achievementPct = BigDecimal.ZERO;

    @Column(name = "rolled_achievement_pct", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal rolledAchievementPct = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", length = 32)
    @Builder.Default
    private GoalHealthStatus healthStatus = GoalHealthStatus.NOT_STARTED;

    @Column(name = "auto_update", nullable = false)
    @Builder.Default
    private boolean autoUpdate = true;

    public void markPending() {
        if (this.status != GoalStatus.DRAFT) {
            throw new IllegalStateException("Only draft goals can be submitted for approval: " + this.status);
        }
        this.status = GoalStatus.PENDING;
        this.goalApprovalStatus = GoalApprovalStatus.PENDING;
    }

    public void activate(UUID approverId) {
        if (this.status != GoalStatus.PENDING) {
            throw new IllegalStateException("Only pending goals can be activated: " + this.status);
        }
        this.status = GoalStatus.ACTIVE;
        this.goalApprovalStatus = GoalApprovalStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = LocalDateTime.now();
    }

    public void activateObjective() {
        this.status = GoalStatus.ACTIVE;
        this.goalApprovalStatus = GoalApprovalStatus.NOT_REQUESTED;
        this.weightPct = 0;
    }

    public void reject() {
        if (this.status != GoalStatus.PENDING) {
            throw new IllegalStateException("Only pending goals can be rejected: " + this.status);
        }
        this.status = GoalStatus.DRAFT;
        this.goalApprovalStatus = GoalApprovalStatus.REJECTED;
    }

    public void withdraw() {
        if (this.status != GoalStatus.PENDING) {
            throw new IllegalStateException("Only pending goals can be withdrawn: " + this.status);
        }
        this.status = GoalStatus.DRAFT;
        this.goalApprovalStatus = GoalApprovalStatus.NOT_REQUESTED;
    }

    public void completeForEvaluation() {
        if (this.status != GoalStatus.ACTIVE) {
            throw new IllegalStateException("Only active goals can enter evaluation: " + this.status);
        }
        this.status = GoalStatus.COMPLETED;
    }

    public void skipForLeaver() {
        this.status = GoalStatus.SKIPPED;
    }

    public void cancel() {
        if (this.status == GoalStatus.COMPLETED) {
            throw new IllegalStateException("Completed goals cannot be cancelled. Use HR skip instead.");
        }
        if (this.status == GoalStatus.CANCELLED || this.status == GoalStatus.SKIPPED) {
            return;
        }
        this.status = GoalStatus.CANCELLED;
    }

    public void forceSkipByHr(String reason) {
        if (this.status == GoalStatus.SKIPPED) {
            return;
        }
        this.status = GoalStatus.SKIPPED;
    }

    public void updateContent(String title, String description) {
        if (this.status != GoalStatus.DRAFT) {
            throw new IllegalStateException("Only draft goals can be edited.");
        }
        this.title = title;
        this.description = description;
    }

    public void updateObjectiveContent(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void updateObjectiveOwner(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public void updateObjectiveGradeCriteria(String gradeS, String gradeA, String gradeB, String gradeC) {
        this.gradeSCriteria = gradeS;
        this.gradeACriteria = gradeA;
        this.gradeBCriteria = gradeB;
        this.gradeCCriteria = gradeC;
    }

    public void updateWeight(int weightPct) {
        if (this.status != GoalStatus.DRAFT) {
            throw new IllegalStateException("Only draft goals can change weight.");
        }
        if (weightPct < 0 || weightPct > 100) {
            throw new IllegalArgumentException("weightPct must be between 0 and 100.");
        }
        this.weightPct = weightPct;
    }

    public void updateGradeCriteria(String gradeS, String gradeA, String gradeB, String gradeC) {
        if (this.status != GoalStatus.DRAFT) {
            throw new IllegalStateException("Only draft goals can change grade criteria.");
        }
        this.gradeSCriteria = gradeS;
        this.gradeACriteria = gradeA;
        this.gradeBCriteria = gradeB;
        this.gradeCCriteria = gradeC;
    }

    public void updateAlignment(UUID alignedOrgGoalId) {
        if (this.status != GoalStatus.DRAFT) {
            throw new IllegalStateException("Only draft goals can change alignment.");
        }
        this.alignedOrgGoalId = alignedOrgGoalId;
    }

    public void updateVisibility(GoalVisibility visibility, List<UUID> visibleTeamIds) {
        if (this.status != GoalStatus.DRAFT) {
            throw new IllegalStateException("Only draft goals can change visibility.");
        }
        this.visibility = visibility;
        if (visibleTeamIds != null) {
            this.visibleTeamIds = new ArrayList<>(visibleTeamIds);
        }
    }

    public void updateObjectiveVisibility(GoalVisibility visibility, List<UUID> visibleTeamIds) {
        this.visibility = visibility;
        this.visibleTeamIds = visibleTeamIds == null
                ? new ArrayList<>()
                : new ArrayList<>(visibleTeamIds);
    }

    public void updateParticipants(List<UUID> participantMemberIds) {
        if (this.status != GoalStatus.DRAFT) {
            throw new IllegalStateException("Only draft goals can change participants.");
        }
        this.participantMemberIds = participantMemberIds == null
                ? new ArrayList<>()
                : new ArrayList<>(participantMemberIds);
    }

    public void updateObjectiveParticipants(List<UUID> participantMemberIds) {
        this.participantMemberIds = participantMemberIds == null
                ? new ArrayList<>()
                : new ArrayList<>(participantMemberIds);
    }

    public void applyManualProgress(BigDecimal value, GoalHealthStatus status) {
        this.actualValue = value;
        this.achievementPct = value;
        this.rolledAchievementPct = value;
        this.healthStatus = status == null ? GoalHealthStatus.ON_TRACK : status;
    }

    public void updateRollup(BigDecimal rolled, RollupSource source) {
        this.rolledAchievementPct = rolled;
    }
}
