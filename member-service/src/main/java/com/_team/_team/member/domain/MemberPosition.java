package com._team._team.member.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.organization.domain.JobGrade;
import com._team._team.organization.domain.JobTitle;
import com._team._team.organization.domain.Organization;
import com._team._team.organization.domain.Role;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class MemberPosition extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID memberPositionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_title_id", nullable = false)
    private JobTitle jobTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_grade_id", nullable = false)
    private JobGrade jobGrade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false)
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Column(nullable = false)
    @Builder.Default
    private String isActiveYn = "YES";

    private String name;

    @Column(nullable = false)
    @Builder.Default
    private String delYn = "NO";

    public void deleteUpdate() {
        this.delYn = "YES";
    }

    public void deactivate() {
        this.isActiveYn = "NO";
        this.endDate = LocalDateTime.now();
    }
}

