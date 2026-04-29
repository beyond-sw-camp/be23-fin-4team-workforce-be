package com._team._team.salary.controller;

import com._team._team.dto.ApiResponse;
import com._team._team.salary.domain.MemberAllowance;
import com._team._team.salary.domain.enums.AllowanceApprovalStatus;
import com._team._team.salary.dto.reqdto.MemberAllowanceAutoGrantReqDto;
import com._team._team.salary.dto.resdto.MemberAllowanceResDto;
import com._team._team.salary.service.MemberAllowanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** 관리자 - 직원 수당 관리 컨트롤러 */
@RestController
@RequestMapping("/salary/admin/allowances")
public class MemberAllowanceAdminController {

    private final MemberAllowanceService memberAllowanceService;

    @Autowired
    public MemberAllowanceAdminController(MemberAllowanceService memberAllowanceService) {
        this.memberAllowanceService = memberAllowanceService;
    }

    /** 신규 입사자에게 기본 수당 AUTO 등록 (결재 생략) */
    @PostMapping("/auto-grant")
    public ResponseEntity<?> autoGrant(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @Valid @RequestBody MemberAllowanceAutoGrantReqDto reqDto) {
        MemberAllowance saved = memberAllowanceService.autoGrant(
                reqDto.getMemberId(), companyId,
                reqDto.getSalaryItemTemplateId(),
                reqDto.getAmount(),
                reqDto.getEffectiveFrom());
        return new ResponseEntity<>(
                ApiResponse.success(MemberAllowanceResDto.fromEntity(saved), "수당이 자동 부여되었습니다."),
                HttpStatus.CREATED
        );
    }

    /** 회사 전체 수당 목록 조회 (월 단위 활성, 상태 필터 지원) */
    @GetMapping
    public ResponseEntity<?> findByCompanyId(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestParam(required = false) AllowanceApprovalStatus status,
            @RequestParam(required = false) String yearMonth) {
        YearMonth ym = (yearMonth == null || yearMonth.isBlank())
                ? YearMonth.now()
                : YearMonth.parse(yearMonth);
        List<MemberAllowanceResDto> result = memberAllowanceService
                .findCompanyActiveInMonth(companyId, status, ym).stream()
                .map(MemberAllowanceResDto::fromEntity)
                .toList();
        return new ResponseEntity<>(
                ApiResponse.success(result, "회사 전체 수당 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    /**
     * 특정 직원의 활성 수당 조회 (정산 미리보기).
     * yearMonth(YYYY-MM) 미지정 시 현재 월. date 가 들어오면 해당 월 기준 (구버전 호환).
     */
    @GetMapping("/members/{memberId}/active")
    public ResponseEntity<?> findActiveByMemberId(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID memberId,
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) LocalDate date) {
        YearMonth ym;
        if (yearMonth != null && !yearMonth.isBlank()) {
            ym = YearMonth.parse(yearMonth);
        } else if (date != null) {
            ym = YearMonth.from(date);
        } else {
            ym = YearMonth.now();
        }
        List<MemberAllowanceResDto> result = memberAllowanceService
                .findMemberActiveInMonth(memberId, companyId, ym).stream()
                .map(MemberAllowanceResDto::fromEntity)
                .toList();
        return new ResponseEntity<>(
                ApiResponse.success(result, "직원 활성 수당 조회 성공"),
                HttpStatus.OK
        );
    }
}
