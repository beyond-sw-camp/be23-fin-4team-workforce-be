package com._team._team.domain.read;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Immutable
@Entity
public class MemberPositionView {
    @Id
    private UUID memberPositionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    private MemberView member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", insertable = false, updatable = false)
    private OrganizationView organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_title_id", insertable = false, updatable = false)
    private JobTitleView jobTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_grade_id", insertable = false, updatable = false)
    private JobGradeView jobGrade;

    @Column(name = "is_active_yn")
    private String isActiveYn;

    @Column(name = "del_yn")
    private String delYn;
}
