package com._team._team.organization.domain;

import com._team._team.company.domain.Company;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Organization extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT),
            nullable = false)
    private Company company;

    @Column(columnDefinition = "BINARY(16)")
    private UUID parentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    @Builder.Default
    private String delYn = "NO";

    // 비즈니스 메서드
    public void updateName(String name) {
        this.name = name;
    }

    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void updateParent(UUID parentId) {
        this.parentId = parentId;
    }

    public void delete() {
        this.delYn = "YES";
    }

    public void restore() {
        this.delYn = "NO";
    }
}
