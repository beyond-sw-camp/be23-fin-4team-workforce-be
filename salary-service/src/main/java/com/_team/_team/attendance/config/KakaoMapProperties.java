package com._team._team.attendance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// Kakao 지도 API 설정값
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kakao")
public class KakaoMapProperties {

    private Api api = new Api();
    private Map map = new Map();

    @Getter
    @Setter
    public static class Api {
        private String url;
        private String key;
    }

    @Getter
    @Setter
    public static class Map {
        private String reverseGeocodePath = "/geo/coord2address.json";
        private int timeoutMs = 2000;
    }
}
