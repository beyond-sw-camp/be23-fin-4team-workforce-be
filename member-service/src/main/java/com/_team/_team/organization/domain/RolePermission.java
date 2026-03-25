package com._team._team.organization.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID rolePermissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;
}

