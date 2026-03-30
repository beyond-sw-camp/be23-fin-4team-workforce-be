package com._team._team.member.repository;

import com._team._team.member.domain.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {
    // 역할별 권한 목록
    List<RolePermission> findByRole_RoleId(UUID roleId);

}
