package com._team._team.company.feignclients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "salary-service")
public interface SalaryServiceClient {

    // 회사 생성 시 기본 휴가 종류 8종 추가
    @PostMapping("/attendance/leave-types/init")
    void initDefaultLeaveTypes(@RequestParam("companyId") UUID companyId);
}