package com._team._team.member.domain;

import com._team._team.organization.domain.JobGrade;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class JobGradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_grade_id", nullable = false)
    private JobGrade jobGrade;

    @Column(nullable = false)
    @Builder.Default
    private String isActiveYn = "YES";

    @Column(nullable = false)
    private LocalDateTime promotionDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
