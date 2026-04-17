package com._team._team.attendance.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// 출퇴근 이벤트 부가정보 조립기
// - IP, UA, Kakao 주소를 감사 로그로 남김
@Slf4j
@Component
public class AttendanceContextResolver {

    private static final int DEVICE_ID_MAX = 100;

    private final ClientRequestInfoExtractor requestInfoExtractor;
    private final KakaoMapClient kakaoMapClient;
    private final boolean reverseGeocodeEnabled;

    public AttendanceContextResolver(
            ClientRequestInfoExtractor requestInfoExtractor,
            KakaoMapClient kakaoMapClient,
            @Value("${attendance.audit.reverse-geocode-enabled:true}") boolean reverseGeocodeEnabled) {
        this.requestInfoExtractor = requestInfoExtractor;
        this.kakaoMapClient = kakaoMapClient;
        this.reverseGeocodeEnabled = reverseGeocodeEnabled;
    }

    // clockIn/clockOut 호출 시점에 한 번 만들어 쓰고 버림, 저장은 deviceId 한 컬럼만
    public Context resolve(HttpServletRequest request,
                           String clientDeviceId,
                           Double latitude,
                           Double longitude) {
        String ip = requestInfoExtractor.extractIp(request);
        String ua = requestInfoExtractor.extractUserAgent(request);
        String address = (reverseGeocodeEnabled && latitude != null && longitude != null)
                ? kakaoMapClient.reverseGeocode(latitude, longitude) : null;

        String packedDeviceId = packDeviceId(clientDeviceId, ip, ua);
        return new Context(ip, ua, address, packedDeviceId);
    }

    // 감사 로그 한 줄 - memberId + 이벤트 타입 + 컨텍스트 정리
    public void writeAuditLog(String eventType, java.util.UUID memberId, Context ctx) {
        log.info("[ATTENDANCE-AUDIT] event={} memberId={} ip={} address={} device={}",
                eventType, memberId, ctx.ip(), ctx.address(), ctx.packedDeviceId());
    }

    // deviceId 컬럼 100자 제한 안에서 "dev|ip=x.x.x.x|ua=Mozilla..." 로 잘라 담는다
    private String packDeviceId(String clientDeviceId, String ip, String ua) {
        String dev = StringUtils.hasText(clientDeviceId) ? clientDeviceId : "n/a";
        String ipPart = StringUtils.hasText(ip) ? "|ip=" + ip : "";
        String uaPart = StringUtils.hasText(ua) ? "|ua=" + truncate(ua, 30) : "";
        String joined = dev + ipPart + uaPart;
        return truncate(joined, DEVICE_ID_MAX);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    // 호출부 편의용 record
    public record Context(String ip, String userAgent, String address, String packedDeviceId) {}
}
