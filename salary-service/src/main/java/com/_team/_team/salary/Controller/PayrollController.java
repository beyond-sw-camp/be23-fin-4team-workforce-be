package com._team._team.salary.Controller;

import com._team._team.dto.ApiResponse;
import com._team._team.salary.Service.PayrollService;
import com._team._team.salary.dto.reqdto.PayrollCreateReqDto;
import com._team._team.salary.dto.reqdto.PayrollItemCreateReqDto;
import com._team._team.salary.dto.reqdto.PayrollItemUpdateReqDto;
import com._team._team.salary.dto.reqdto.PayrollUpdateReqDto;
import com._team._team.salary.dto.resdto.PayrollItemResDto;
import com._team._team.salary.dto.resdto.PayrollResDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    @Autowired
    public PayrollController(PayrollService payrollService){
        this.payrollService = payrollService;
    }

    // ===================== 급여대장 =====================

    /** 급여대장 생성 */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PayrollResDto>> createPayroll(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @Valid @RequestBody PayrollCreateReqDto reqDto){
        PayrollResDto resDto = payrollService.createPayroll(companyId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여대장이 생성되었습니다."),
                HttpStatus.CREATED
        );
    }

    /** 급여대장 단건 조회 */
    @GetMapping("/{payrollId}")
    public ResponseEntity<ApiResponse<PayrollResDto>> findPayrollById(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollId){
        PayrollResDto resDto = payrollService.findPayrollById(companyId, payrollId);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여대장 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 직원별 급여대장 목록 조회 */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiResponse<List<PayrollResDto>>> findPayrollsByMemberId(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID memberId){
        List<PayrollResDto> resDtoList = payrollService.findPayrollByMemberId(companyId, memberId);
        return new ResponseEntity<>(
                ApiResponse.success(resDtoList, "직원별 급여대장 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 급여대장 수정 */
    @PutMapping("/{payrollId}")
    public ResponseEntity<ApiResponse<PayrollResDto>> updatePayroll(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollId,
            @Valid @RequestBody PayrollUpdateReqDto reqDto){
        PayrollResDto resDto = payrollService.updatePayroll(companyId, payrollId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여대장이 수정되었습니다."),
                HttpStatus.OK
        );
    }

    /** 급여대장 삭제 */
    @DeleteMapping("/{payrollId}")
    public ResponseEntity<ApiResponse<Void>> deletePayroll(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollId){
        payrollService.deletePayroll(companyId, payrollId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "급여대장이 삭제되었습니다."),
                HttpStatus.OK
        );
    }

    /** 급여 확정 (DRAFT → CONFIRMED) */
    @PatchMapping("/{payrollId}/confirm")
    public ResponseEntity<ApiResponse<PayrollResDto>> confirmPayroll(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollId) {
        PayrollResDto resDto = payrollService.confirmPayroll(companyId, payrollId);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여대장이 확정되었습니다."),
                HttpStatus.OK
        );
    }

    /** 지급 완료 (CONFIRMED → PAID) */
    @PatchMapping("/{payrollId}/pay")
    public ResponseEntity<ApiResponse<PayrollResDto>> payPayroll(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollId) {
        PayrollResDto resDto = payrollService.payPayroll(companyId, payrollId);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여가 지급 완료되었습니다."),
                HttpStatus.OK
        );
    }

    // ===================== 급여대장 항목 =====================

    /** 급여대장 항목 추가 */
    @PostMapping("/{payrollId}/items")
    public ResponseEntity<ApiResponse<PayrollItemResDto>> createPayrollItem(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollId,
            @Valid @RequestBody PayrollItemCreateReqDto reqDto) {
        PayrollItemResDto resDto = payrollService.createPayrollItem(companyId, payrollId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여대장 항목이 추가되었습니다."),
                HttpStatus.CREATED
        );
    }

    /** 급여대장별 항목 목록 조회 */
    @GetMapping("/{payrollId}/items")
    public ResponseEntity<ApiResponse<List<PayrollItemResDto>>> findPayrollItems(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollId) {
        List<PayrollItemResDto> resDtoList = payrollService.findPayrollItems(companyId, payrollId);
        return new ResponseEntity<>(
                ApiResponse.success(resDtoList, "급여대장 항목 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 급여대장 항목 수정 */
    @PutMapping("/items/{payrollItemId}")
    public ResponseEntity<ApiResponse<PayrollItemResDto>> updatePayrollItem(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollItemId,
            @Valid @RequestBody PayrollItemUpdateReqDto reqDto) {
        PayrollItemResDto resDto = payrollService.updatePayrollItem(companyId, payrollItemId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "급여대장 항목이 수정되었습니다."),
                HttpStatus.OK
        );
    }

    /** 급여대장 항목 삭제 (소프트 삭제) */
    @DeleteMapping("/items/{payrollItemId}")
    public ResponseEntity<ApiResponse<Void>> deletePayrollItem(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollItemId) {
        payrollService.deletePayrollItem(companyId, payrollItemId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "급여대장 항목이 삭제되었습니다."),
                HttpStatus.OK
        );
    }
}
