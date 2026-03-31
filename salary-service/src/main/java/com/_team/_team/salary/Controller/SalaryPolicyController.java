package com._team._team.salary.Controller;


import com._team._team.salary.dto.reqdto.SalaryPolicyCreateReqDto;
import com._team._team.salary.dto.reqdto.SalaryPolicyUpdateReqDto;
import com._team._team.salary.dto.resdto.SalaryPolicyResDto;
import com._team._team.salary.Service.SalaryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/salary-policies")
public class SalaryPolicyController {

    private final SalaryService salaryService;

    public SalaryPolicyController(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    // 급여 정책 생성
    @PostMapping("/create")
    public ResponseEntity<SalaryPolicyResDto> create( @RequestHeader("X-User-CompanyId") UUID companyId,
                                                      @Valid @RequestBody SalaryPolicyCreateReqDto reqDto) {
        SalaryPolicyResDto resDto = salaryService.save(companyId, reqDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(resDto);
    }

    // 급여 정책 단건 조회
    @GetMapping("/{salaryPolicyId}")
    public ResponseEntity<SalaryPolicyResDto> findPolicy(@PathVariable UUID salaryPolicyId){
        SalaryPolicyResDto resDto = salaryService.findById(salaryPolicyId);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    // 회사별 급여 정책 목록 조회
    @GetMapping
    public ResponseEntity<List<SalaryPolicyResDto>> findByCompanyId(
            @RequestHeader("X-User-CompanyId") UUID companyId){
        List<SalaryPolicyResDto> resDtos = salaryService.findByCompanyId(companyId);
        return ResponseEntity.status(HttpStatus.OK).body(resDtos);
    }

    // 급여 정책 수정
    @PutMapping("/{salaryPolicyId}")
    public ResponseEntity<SalaryPolicyResDto> update(
            @PathVariable UUID salaryPolicyId,
            @Valid @RequestBody SalaryPolicyUpdateReqDto reqDto) {
        SalaryPolicyResDto resDto = salaryService.update(salaryPolicyId, reqDto);
        return ResponseEntity.status(HttpStatus.OK).body(resDto);
    }

    // 급여 정책 삭제
    @DeleteMapping("/{salaryPolicyId}")
    public ResponseEntity<Void> delete(@PathVariable UUID salaryPolicyId) {
        salaryService.delete(salaryPolicyId);
        return ResponseEntity.noContent().build();
    }
}
