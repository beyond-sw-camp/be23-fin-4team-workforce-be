package com._team._team.salary.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 신규 입사 Salary 자동 생성 시 적용할 기본값
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "salary.auto-create")
public class SalaryAutoCreateProperties {
    private long defaultBaseSalary = 0L;
}
