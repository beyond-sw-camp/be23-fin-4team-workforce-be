package com._team._team.attendance.Service;

import com._team._team.attendance.config.KakaoMapProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

// Kakao 좌표 -> 주소
// 키 없거나 실패하면 null 리턴, 호출부에서 로그만 남기고 무시
@Slf4j
@Component
public class KakaoMapClient {

    private final KakaoMapProperties props;
    private final RestTemplate restTemplate;

    public KakaoMapClient(KakaoMapProperties props,
                          @Qualifier("kakaoRestTemplate") RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    // 좌표를 도로명/지번 주소 문자열로 변환 실패 시 null
    @SuppressWarnings("unchecked")
    public String reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) return null;
        if (!StringUtils.hasText(props.getApi().getKey()) || !StringUtils.hasText(props.getApi().getUrl())) {
            log.debug("kakao.api.key/url 미설정 → reverseGeocode 스킵");
            return null;
        }

        String url = UriComponentsBuilder
                .fromHttpUrl(props.getApi().getUrl() + props.getMap().getReverseGeocodePath())
                .queryParam("x", longitude)
                .queryParam("y", latitude)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + props.getApi().getKey());

        try {
            ResponseEntity<Map> res = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = res.getBody();
            if (body == null) return null;

            List<Map<String, Object>> documents = (List<Map<String, Object>>) body.get("documents");
            if (documents == null || documents.isEmpty()) return null;

            Map<String, Object> first = documents.get(0);
            Map<String, Object> road = (Map<String, Object>) first.get("road_address");
            if (road != null && road.get("address_name") != null) {
                return String.valueOf(road.get("address_name"));
            }
            Map<String, Object> addr = (Map<String, Object>) first.get("address");
            return addr == null ? null : String.valueOf(addr.get("address_name"));
        } catch (Exception e) {
            log.warn("[KAKAO] reverseGeocode 실패 lat={} lon={} msg={}", latitude, longitude, e.getMessage());
            return null;
        }
    }
}
