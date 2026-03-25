package com._team._team.organization.domain;

import com._team._team.organization.domain.enums.Action;
import com._team._team.organization.domain.enums.PermissionRange;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID permissionId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String resource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_range", nullable = false)
    private PermissionRange range;

    private String description;
}