package com._team._team.salary.Controller;

import com._team._team.dto.ApiResponse;
import com._team._team.salary.Service.SalaryService;
import com._team._team.salary.dto.reqdto.SalaryCreateReqDto;
import com._team._team.salary.dto.reqdto.SalaryUpdateReqDto;
import com._team._team.salary.dto.resdto.SalaryResDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/salaries")
public class SalaryController {

    private final SalaryService salaryService;

    @Autowired
    public SalaryController(SalaryService salaryService){
        this.salaryService = salaryService;
    }

    /** 급여 생성 */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<SalaryResDto>> create(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @Valid @RequestBody SalaryCreateReqDto reqDto){
        SalaryResDto resDto = salaryService.save(companyId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여가 생성되었습니다."),
                HttpStatus.CREATED
        );
    }

    /** 급여 단건 조회 */
    @GetMapping("/{salaryId}")
    public ResponseEntity<ApiResponse<SalaryResDto>> findById(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID salaryId){
        SalaryResDto resDto = salaryService.findById(companyId, salaryId);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 회사별 급여 목록 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SalaryResDto>>> findByCompanyId(
            @RequestHeader("X-User-CompanyId") UUID companyId) {
        List<SalaryResDto> resDtos = salaryService.findByCompanyId(companyId);
        return new ResponseEntity<>(
                ApiResponse.success(resDtos, "급여 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 직원별 급여 이력 조회 */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<SalaryResDto>>> findByMemberId(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID memberId){
        List<SalaryResDto> resDtos = salaryService.findByMemberId(companyId, memberId);
        return new ResponseEntity<>(
                ApiResponse.success(resDtos, "직원별 급여 이력 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 급여 수정 */
    @PutMapping("/{salaryId}")
    public ResponseEntity<ApiResponse<SalaryResDto>> update(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID salaryId,
            @Valid @RequestBody SalaryUpdateReqDto reqDto){
        SalaryResDto resDto = salaryService.update(companyId, salaryId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여가 수정되었습니다."),
                HttpStatus.OK
        );
    }

    /** 급여 삭제 */
    @DeleteMapping("/{salaryId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID salaryId){
        salaryService.delete(companyId, salaryId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "급여가 삭제되었습니다."),
                HttpStatus.OK
        );
    }
}
