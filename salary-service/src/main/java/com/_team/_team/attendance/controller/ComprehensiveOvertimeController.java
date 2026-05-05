package com._team._team.attendance.controller;

import com._team._team.attendance.dto.resDto.ComprehensiveOvertimeStatusResDto;
import com._team._team.attendance.service.ComprehensiveOvertimeService;
import com._team._team.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 포괄임금제 직원 OT 누적 현황 API
 */
@RestController
@RequestMapping("/attendance/comprehensive-overtime")
public class ComprehensiveOvertimeController {

    private final ComprehensiveOvertimeService service;

    @Autowired
    public ComprehensiveOvertimeController(ComprehensiveOvertimeService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<List<ComprehensiveOvertimeStatusResDto>>> getStatus(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestParam(value = "baseDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {

        LocalDate target = baseDate != null ? baseDate : LocalDate.now();
        List<ComprehensiveOvertimeStatusResDto> result = service.getStatus(companyId, target);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(result, "조회 성공"));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<ComprehensiveOvertimeStatusResDto>> getMy(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestParam(value = "baseDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {

        LocalDate target = baseDate != null ? baseDate : LocalDate.now();
        ComprehensiveOvertimeStatusResDto result = service.getMyStatus(companyId, memberId, target);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(result, "조회 성공"));
    }
}
