package com._team._team.batch.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Quartz 스케줄 등록을 YAML에서 제어
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "batch.scheduler")
public class BatchSchedulerProperties {

    /**
     * 운영 환경에서는 JDBC JobStore 및 크론을 검증한 뒤 true 설정
     */
    private boolean enabled = false;

    /**
     * Spring Batch Job 이름 → Quartz Cron 문자열.
     */
    private Map<String, String> jobs = new LinkedHashMap<>();
}
