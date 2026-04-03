package com._team._team.salary.Controller;

import com._team._team.dto.ApiResponse;
import com._team._team.salary.dto.reqdto.SalaryPolicyCreateReqDto;
import com._team._team.salary.dto.reqdto.SalaryPolicyUpdateReqDto;
import com._team._team.salary.dto.resdto.SalaryPolicyResDto;
import com._team._team.salary.Service.SalaryPolicyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/salary-policies")
public class SalaryPolicyController {

    private final SalaryPolicyService salaryService;

    @Autowired
    public SalaryPolicyController(SalaryPolicyService salaryService) {
        this.salaryService = salaryService;
    }

    /** 급여 정책 생성 */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<SalaryPolicyResDto>> create(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @Valid @RequestBody SalaryPolicyCreateReqDto reqDto) {
        SalaryPolicyResDto resDto = salaryService.save(companyId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여 정책이 생성되었습니다."),
                HttpStatus.CREATED
        );
    }

    /** 급여 정책 단건 조회 */
    @GetMapping("/{salaryPolicyId}")
    public ResponseEntity<ApiResponse<SalaryPolicyResDto>> findPolicy(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID salaryPolicyId){
        SalaryPolicyResDto resDto = salaryService.findById(companyId, salaryPolicyId);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여 정책 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 회사별 급여 정책 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SalaryPolicyResDto>>> findByCompanyId(
            @RequestHeader("X-User-CompanyId") UUID companyId){
        List<SalaryPolicyResDto> resDtos = salaryService.findByCompanyId(companyId);
        return new ResponseEntity<>(
                ApiResponse.success(resDtos, "급여 정책 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 급여 정책 수정 */
    @PutMapping("/{salaryPolicyId}")
    public ResponseEntity<ApiResponse<SalaryPolicyResDto>> update(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID salaryPolicyId,
            @Valid @RequestBody SalaryPolicyUpdateReqDto reqDto) {
        SalaryPolicyResDto resDto = salaryService.update(companyId, salaryPolicyId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여 정책이 수정되었습니다."),
                HttpStatus.OK
        );
    }

    /** 급여 정책 삭제 */
    @DeleteMapping("/{salaryPolicyId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID salaryPolicyId) {
        salaryService.delete(companyId, salaryPolicyId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "급여 정책이 삭제되었습니다."),
                HttpStatus.OK
        );
    }
}
