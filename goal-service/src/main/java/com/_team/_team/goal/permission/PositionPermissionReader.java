package com._team._team.goal.permission;

import com._team._team.annotation.Action;
import com._team._team.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * goal-service 전용 — PermissionAspect 와 동일한 Redis 키로 GOAL 권한 보조 판별.
 */
@Component
public class PositionPermissionReader {

    private final RedisTemplate<String, String> permissionRedisTemplate;

    public PositionPermissionReader(
            @Qualifier("permissionInventory") RedisTemplate<String, String> permissionRedisTemplate) {
        this.permissionRedisTemplate = permissionRedisTemplate;
    }

    public boolean isSystemAdmin(HttpServletRequest request) {
        return "YES".equals(request.getHeader("X-User-IsSystemAdmin"));
    }

    /** 시스템 관리자이거나 Redis 에 해당 권한이 있으면 true. 미보유·미조회는 false (예외 없음). */
    public boolean hasPermission(HttpServletRequest request, Resource resource, Action action) {
        if (isSystemAdmin(request)) {
            return true;
        }
        String memberPositionId = request.getHeader("X-User-MemberPositionId");
        if (memberPositionId == null || memberPositionId.isEmpty()) {
            return false;
        }
        String permissionStr = permissionRedisTemplate.opsForValue().get("PERMISSION:" + memberPositionId);
        if (permissionStr == null || permissionStr.isEmpty()) {
            return false;
        }
        String required = resource.name() + ":" + action.name();
        return List.of(permissionStr.split(",")).stream().anyMatch(p -> p.startsWith(required));
    }
}
