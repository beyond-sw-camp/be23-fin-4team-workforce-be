package com._team._team.attendance.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

// Kakao 같은 외부 API 호출용 공용 RestTemplate
// 지연 길면 출퇴근 요청 자체가 느려지니까 타임아웃 짧게 잡는다
@Configuration
public class RestTemplateConfig {

    @Bean(name = "kakaoRestTemplate")
    public RestTemplate kakaoRestTemplate(RestTemplateBuilder builder, KakaoMapProperties props) {
        return builder
                .connectTimeout(Duration.ofMillis(props.getMap().getTimeoutMs()))
                .readTimeout(Duration.ofMillis(props.getMap().getTimeoutMs()))
                .build();
    }
}
