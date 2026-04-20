package com._team._team.salary.controller;

import com._team._team.batch.payroll.worker.PayrollCalculateWorker;
import com._team._team.dto.ApiResponse;
import com._team._team.salary.service.PayrollService;
import com._team._team.salary.dto.reqdto.PayrollCreateReqDto;
import com._team._team.salary.dto.reqdto.PayrollItemCreateReqDto;
import com._team._team.salary.dto.reqdto.PayrollItemUpdateReqDto;
import com._team._team.salary.dto.reqdto.PayrollRecalculateReqDto;
import com._team._team.salary.dto.reqdto.PayrollUpdateReqDto;
import com._team._team.salary.dto.resdto.PayrollRecalculateResDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/salary/payroll")
public class PayrollController {

    private final PayrollService payrollService;
    private final PayrollCalculateWorker payrollCalculateWorker;

    @Autowired
    public PayrollController(PayrollService payrollService,
                             PayrollCalculateWorker payrollCalculateWorker){
        this.payrollService = payrollService;
        this.payrollCalculateWorker = payrollCalculateWorker;
    }

    // ===================== 급여대장 =====================

    /** 급여대장 생성 */
    @PostMapping("/create")
    public ResponseEntity<?> createPayroll(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @Valid @RequestBody PayrollCreateReqDto reqDto){
        return new ResponseEntity<>(
                ApiResponse.success(payrollService.createPayroll(companyId, reqDto), "급여대장이 생성되었습니다."),
                HttpStatus.CREATED
        );
    }

    /** 급여대장 단건 조회 */
    @GetMapping("/{payrollId}")
    public ResponseEntity<?> findPayrollById(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollId){
        return new ResponseEntity<>(
                ApiResponse.success(payrollService.findPayrollById(companyId, payrollId), "급여대장 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 직원별 급여대장 목록 조회 */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<?> findPayrollsByMemberId(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID memberId){
        return new ResponseEntity<>(
                ApiResponse.success(payrollService.findPayrollByMemberId(companyId, memberId), "직원별 급여대장 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 급여대장 수정 */
    @PutMapping("/{payrollId}")
    public ResponseEntity<?> updatePayroll(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollId,
            @Valid @RequestBody PayrollUpdateReqDto reqDto){
        return new ResponseEntity<>(
                ApiResponse.success(payrollService.updatePayroll(companyId, payrollId, reqDto), "급여대장이 수정되었습니다."),
                HttpStatus.OK
        );
    }

    /** 급여대장 삭제 */
    @DeleteMapping("/{payrollId}")
    public ResponseEntity<?> deletePayroll(
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
    public ResponseEntity<?> confirmPayroll(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollId) {
        return new ResponseEntity<>(
                ApiResponse.success(payrollService.confirmPayroll(companyId, payrollId), "급여대장이 확정되었습니다."),
                HttpStatus.OK
        );
    }

    /** 지급 완료 (CONFIRMED → PAID) */
    @PatchMapping("/{payrollId}/pay")
    public ResponseEntity<?> payPayroll(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollId) {
        return new ResponseEntity<>(
                ApiResponse.success(payrollService.payPayroll(companyId, payrollId), "급여가 지급 완료되었습니다."),
                HttpStatus.OK
        );
    }

    // 회사 단위 급여대장 재계산
    // 자동 배치 누락 / 마스터 데이터 늦게 등록한 경우 관리자가 직접 호출
    @PostMapping("/recalculate")
    public ResponseEntity<?> recalculate(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @Valid @RequestBody PayrollRecalculateReqDto reqDto) {
        PayrollCalculateWorker.Counter c =
                payrollCalculateWorker.runForCompany(companyId, reqDto.getSettlementDate());
        return new ResponseEntity<>(
                ApiResponse.success(PayrollRecalculateResDto.from(c), "급여대장 재계산이 완료되었습니다."),
                HttpStatus.OK
        );
    }

    // ===================== 급여대장 항목 =====================

    /** 급여대장 항목 추가 */
    @PostMapping("/{payrollId}/items")
    public ResponseEntity<?> createPayrollItem(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollId,
            @Valid @RequestBody PayrollItemCreateReqDto reqDto) {
        return new ResponseEntity<>(
                ApiResponse.success(payrollService.createPayrollItem(companyId, payrollId, reqDto), "급여대장 항목이 추가되었습니다."),
                HttpStatus.CREATED
        );
    }

    /** 급여대장별 항목 목록 조회 */
    @GetMapping("/{payrollId}/items")
    public ResponseEntity<?> findPayrollItems(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollId) {
        return new ResponseEntity<>(
                ApiResponse.success(payrollService.findPayrollItems(companyId, payrollId), "급여대장 항목 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 급여대장 항목 수정 */
    @PutMapping("/items/{payrollItemId}")
    public ResponseEntity<?> updatePayrollItem(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollItemId,
            @Valid @RequestBody PayrollItemUpdateReqDto reqDto) {
        return new ResponseEntity<>(
                ApiResponse.success(payrollService.updatePayrollItem(companyId, payrollItemId, reqDto), "급여대장 항목이 수정되었습니다."),
                HttpStatus.OK
        );
    }

    /** 급여대장 항목 삭제 (소프트 삭제) */
    @DeleteMapping("/items/{payrollItemId}")
    public ResponseEntity<?> deletePayrollItem(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID payrollItemId) {
        payrollService.deletePayrollItem(companyId, payrollItemId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "급여대장 항목이 삭제되었습니다."),
                HttpStatus.OK
        );
    }
}
