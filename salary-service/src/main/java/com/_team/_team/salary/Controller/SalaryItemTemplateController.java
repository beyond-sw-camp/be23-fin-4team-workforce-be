package com._team._team.salary.Controller;

import com._team._team.dto.ApiResponse;
import com._team._team.salary.Service.SalaryItemTemplateService;
import com._team._team.salary.dto.reqdto.SalaryItemTemplateCreateReqDto;
import com._team._team.salary.dto.reqdto.SalaryItemTemplateUpdateReqDto;
import com._team._team.salary.dto.resdto.SalaryItemTemplateResDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public SalaryItemTemplateController(SalaryItemTemplateService salaryItemTemplateService) {
        this.salaryItemTemplateService = salaryItemTemplateService;
    }

    /** 급여 항목 템플릿 생성 */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<SalaryItemTemplateResDto>> create(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @Valid @RequestBody SalaryItemTemplateCreateReqDto reqDto) {
        SalaryItemTemplateResDto resDto = salaryItemTemplateService.save(companyId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여 항목 템플릿이 생성되었습니다."),
                HttpStatus.CREATED
        );
    }

    /** 급여 항목 템플릿 단건 조회 */
    @GetMapping("/{salaryItemTemplateId}")
    public ResponseEntity<ApiResponse<SalaryItemTemplateResDto>> findById(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID salaryItemTemplateId) {
        SalaryItemTemplateResDto resDto = salaryItemTemplateService.findById(companyId, salaryItemTemplateId);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여 항목 템플릿 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 회사별 급여 항목 템플릿 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SalaryItemTemplateResDto>>> findByCompanyId(
            @RequestHeader("X-User-CompanyId") UUID companyId) {
        List<SalaryItemTemplateResDto> resDtos = salaryItemTemplateService.findByCompanyId(companyId);
        return new ResponseEntity<>(
                ApiResponse.success(resDtos, "급여 항목 템플릿 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 급여 항목 템플릿 수정 */
    @PutMapping("/{salaryItemTemplateId}")
    public ResponseEntity<ApiResponse<SalaryItemTemplateResDto>> update(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID salaryItemTemplateId,
            @Valid @RequestBody SalaryItemTemplateUpdateReqDto reqDto) {
        SalaryItemTemplateResDto resDto = salaryItemTemplateService.update(companyId, salaryItemTemplateId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여 항목 템플릿이 수정되었습니다."),
                HttpStatus.OK
        );
    }

    /** 급여 항목 템플릿 삭제 (soft delete) */
    @DeleteMapping("/{salaryItemTemplateId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID salaryItemTemplateId) {
        salaryItemTemplateService.delete(companyId, salaryItemTemplateId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "급여 항목 템플릿이 삭제되었습니다."),
                HttpStatus.OK
        );
    }
}
