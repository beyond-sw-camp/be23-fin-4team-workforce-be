package com._team._team.salary.controller;

import com._team._team.salary.domain.MemberAllowance;
import com._team._team.salary.domain.enums.AllowanceApprovalStatus;
import com._team._team.salary.dto.reqdto.MemberAllowanceAutoGrantReqDto;
import com._team._team.salary.dto.resdto.MemberAllowanceResDto;
import com._team._team.salary.service.MemberAllowanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// 관리자, 신규 직원 수당 등록 및 회사 전체 수당 조회
@RestController
@RequestMapping("/salary/admin/allowances")
public class MemberAllowanceAdminController {

    private final MemberAllowanceService memberAllowanceService;

    @Autowired
    public MemberAllowanceAdminController(MemberAllowanceService memberAllowanceService) {
        this.memberAllowanceService = memberAllowanceService;
    }

    // 관리자, 신규 입사자에게 기본 수당 AUTO 등록 (결재 생략)
    @PostMapping("/auto-grant")
    public ResponseEntity<MemberAllowanceResDto> autoGrant(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @Valid @RequestBody MemberAllowanceAutoGrantReqDto reqDto) {

        MemberAllowance saved = memberAllowanceService.autoGrant(
                reqDto.getMemberId(), companyId,
                reqDto.getSalaryItemTemplateId(),
                reqDto.getAmount(),
                reqDto.getEffectiveFrom());

        return ResponseEntity.ok(MemberAllowanceResDto.fromEntity(saved));
    }

    // 관리자, 회사 전체 수당 상태별 조회 (결재 대기, 승인 등)
    @GetMapping
    public ResponseEntity<List<MemberAllowanceResDto>> listByStatus(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestParam AllowanceApprovalStatus status) {

        List<MemberAllowanceResDto> result = memberAllowanceService
                .findByCompanyAndStatus(companyId, status).stream()
                .map(MemberAllowanceResDto::fromEntity)
                .toList();

        return ResponseEntity.ok(result);
    }

    // 관리자, 특정 직원의 정산일 기준 활성 수당 조회 (급여 계산 미리보기)
    @GetMapping("/members/{memberId}/active")
    public ResponseEntity<List<MemberAllowanceResDto>> activeOf(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID memberId,
            @RequestParam LocalDate date) {

        List<MemberAllowanceResDto> result = memberAllowanceService
                .findActiveAt(memberId, companyId, date).stream()
                .map(MemberAllowanceResDto::fromEntity)
                .toList();

        return ResponseEntity.ok(result);
    }
}