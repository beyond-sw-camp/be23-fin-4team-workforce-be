package com._team._team.salary.controller;


import com._team._team.dto.ApiResponse;
import com._team._team.salary.dto.reqdto.RetirementSimReqDto;
import com._team._team.salary.dto.resdto.RetirementSimResDto;
import com._team._team.salary.service.RetirementSimulationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// 직원 본인 퇴직금 시뮬레이션
@RestController
@RequestMapping("/salary/retirement")
public class RetirementSimulationController {

    private final RetirementSimulationService retirementSimulationService;

    @Autowired
    public RetirementSimulationController(RetirementSimulationService retirementSimulationService) {
        this.retirementSimulationService = retirementSimulationService;
    }

    // 본인 퇴직금 시뮬 회사 정책 자동 적용
    @PostMapping("/simulate/me")
    public ResponseEntity<?> simulateMine(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestHeader("X-User-UUID") UUID memberId,
            @Valid @RequestBody RetirementSimReqDto reqDto) {
        RetirementSimResDto data = retirementSimulationService.simulateForMember(companyId, memberId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(data, "퇴직금 시뮬레이션 성공"),
                HttpStatus.OK);
    }
}