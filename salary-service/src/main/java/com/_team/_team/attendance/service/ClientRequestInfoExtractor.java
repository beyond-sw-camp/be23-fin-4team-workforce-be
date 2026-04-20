package com._team._team.attendance.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// 요청 객체에서 IP / User-Agent 뽑기
// 프록시/게이트웨이 거치면 X-Forwarded-For가 진짜 클라이언트 IP라 여기부터 확인
@Component
public class ClientRequestInfoExtractor {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    // 클라이언트 IP. 프록시 거쳤으면 최초 origin IP, 아니면 remoteAddr.
    public String extractIp(HttpServletRequest request) {
        if (request == null) return null;
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value) && !"unknown".equalsIgnoreCase(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    // User-Agent 원문 (길어서 deviceId 패킹 시엔 축약해서 쓴다)
    public String extractUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        return request.getHeader("User-Agent");
    }
}
