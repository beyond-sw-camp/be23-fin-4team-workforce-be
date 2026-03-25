package com._team._team.organization.domain;

import com._team._team.company.domain.Company;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class JobTitle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID jobTitleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    @Builder.Default
    private String delYn = "NO";

    public void deleteUpdate() {
        this.delYn = "YES";
    }
}
