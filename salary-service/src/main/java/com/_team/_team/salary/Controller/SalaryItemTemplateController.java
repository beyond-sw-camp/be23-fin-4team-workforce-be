package com._team._team.salary.Controller;

import com._team._team.salary.Service.SalaryItemTemplateService;
import com._team._team.salary.dto.reqdto.SalaryItemTemplateCreateReqDto;
import com._team._team.salary.dto.reqdto.SalaryItemTemplateUpdateReqDto;
import com._team._team.salary.dto.resdto.SalaryItemTemplateResDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 급여 항목 템플릿 컨트롤러
 */
@RestController
@RequestMapping("/salary-item-templates")
public class SalaryItemTemplateController {

    private final SalaryItemTemplateService salaryItemTemplateService;

    public SalaryItemTemplateController(SalaryItemTemplateService salaryItemTemplateService) {
        this.salaryItemTemplateService = salaryItemTemplateService;
    }

    /** 급여 항목 템플릿 생성 */
    @PostMapping("/create")
    public ResponseEntity<SalaryItemTemplateResDto> create(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @Valid @RequestBody SalaryItemTemplateCreateReqDto reqDto) {
        SalaryItemTemplateResDto resDto = salaryItemTemplateService.save(companyId, reqDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resDto);
    }

    /** 급여 항목 템플릿 단건 조회 */
    @GetMapping("/{salaryItemTemplateId}")
    public ResponseEntity<SalaryItemTemplateResDto> findById(
            @PathVariable UUID salaryItemTemplateId) {
        SalaryItemTemplateResDto resDto = salaryItemTemplateService.findById(salaryItemTemplateId);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    /** 회사별 급여 항목 템플릿 목록 조회 */
    @GetMapping
    public ResponseEntity<List<SalaryItemTemplateResDto>> findByCompanyId(
            @RequestHeader("X-User-CompanyId") UUID companyId) {
        List<SalaryItemTemplateResDto> resDtos = salaryItemTemplateService.findByCompanyId(companyId);
        return ResponseEntity.status(HttpStatus.OK).body(resDtos);
    }

    /** 급여 항목 템플릿 수정 */
    @PutMapping("/{salaryItemTemplateId}")
    public ResponseEntity<SalaryItemTemplateResDto> update(
            @PathVariable UUID salaryItemTemplateId,
            @Valid @RequestBody SalaryItemTemplateUpdateReqDto reqDto) {
        SalaryItemTemplateResDto resDto = salaryItemTemplateService.update(salaryItemTemplateId, reqDto);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    /** 급여 항목 템플릿 삭제 (soft delete) */
    @DeleteMapping("/{salaryItemTemplateId}")
    public ResponseEntity<Void> delete(@PathVariable UUID salaryItemTemplateId) {
        salaryItemTemplateService.delete(salaryItemTemplateId);
        return ResponseEntity.noContent().build();
    }
}
