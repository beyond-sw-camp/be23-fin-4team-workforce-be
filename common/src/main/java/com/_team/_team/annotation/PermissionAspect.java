package com._team._team.annotation;

import com._team._team.dto.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    private final RedisTemplate<String, String> permissionRedisTemplate;

    public PermissionAspect(
            @Qualifier("permissionInventory") RedisTemplate<String, String> permissionRedisTemplate) {
        this.permissionRedisTemplate = permissionRedisTemplate;
    }

    @Around("@annotation(checkPermission)")
    public Object check(ProceedingJoinPoint pjp, CheckPermission checkPermission) throws Throwable {

        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getRequest();

        String isSystemAdmin = request.getHeader("X-User-IsSystemAdmin");
        String memberPositionId = request.getHeader("X-User-MemberPositionId");

        // 1. 시스템 관리자 체크
        if ("YES".equals(isSystemAdmin)) {
            return pjp.proceed();
        }


        // 2. Redis에서 권한 목록 조회
        String permissionStr = permissionRedisTemplate.opsForValue()
                .get("PERMISSION:" + memberPositionId);

        if (permissionStr == null || permissionStr.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "권한 정보를 찾을 수 없습니다. 다시 로그인해주세요."
            );
        }

        // 3. 권한 체크
        Resource resource = checkPermission.resource();
        Action action = checkPermission.action();
        String requiredPermission = resource.name() + ":" + action.name();

        List<String> grantedTokens = List.of(permissionStr.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        boolean hasPermission = grantedTokens.stream()
                .anyMatch(p -> p.startsWith(requiredPermission));

        /*
         * READ 요청은 동일 리소스에 대한 CREATE / UPDATE / DELETE 가 있으면 통과시킨다.
         * (예: EVALUATION:UPDATE 만 부여된 HR이 시즌·설계 목록 조회 API 를 쓸 수 있게 함)
         */
        if (!hasPermission && action == Action.READ) {
            String resPrefix = resource.name() + ":";
            hasPermission = grantedTokens.stream().anyMatch(p ->
                    p.startsWith(resPrefix + Action.CREATE.name())
                            || p.startsWith(resPrefix + Action.UPDATE.name())
                            || p.startsWith(resPrefix + Action.DELETE.name()));
        }

        if (!hasPermission) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "권한이 없습니다."
            );
        }

        return pjp.proceed();
    }
}