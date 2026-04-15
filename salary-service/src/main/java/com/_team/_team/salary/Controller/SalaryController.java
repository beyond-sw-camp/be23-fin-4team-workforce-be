package com._team._team.salary.Controller;

import com._team._team.dto.ApiResponse;
import com._team._team.kafka.event.MemberHiredEvent;
import com._team._team.salary.Service.SalaryAutoCreateService;
import com._team._team.salary.Service.SalaryService;
import com._team._team.salary.dto.reqdto.SalaryCreateReqDto;
import com._team._team.salary.dto.reqdto.SalaryUpdateReqDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/salary/salaries")
public class SalaryController {

    private final SalaryService salaryService;
    private final SalaryAutoCreateService salaryAutoCreateService;

    @Autowired
    public SalaryController(SalaryService salaryService,
                            SalaryAutoCreateService salaryAutoCreateService){
        this.salaryService = salaryService;
        this.salaryAutoCreateService = salaryAutoCreateService;
    }

    /** 급여 생성 */
    @PostMapping("/create")
    public ResponseEntity<?> create(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @Valid @RequestBody SalaryCreateReqDto reqDto){
        return new ResponseEntity<>(
                ApiResponse.success(salaryService.save(companyId, reqDto), "급여가 생성되었습니다."),
                HttpStatus.CREATED
        );
    }

    // 복구용 수동 bootstrap - Kafka 실패 or 기존 직원 백필 시 사용
    // 회사 기본값(base-salary)로 Salary 1건 생성 이후 관리자가 PATCH로 수정
    @PostMapping("/bootstrap")
    public ResponseEntity<?> bootstrap(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestBody MemberHiredEvent body) {
        MemberHiredEvent e = MemberHiredEvent.builder()
                .memberId(body.getMemberId())
                .companyId(companyId)
                .jobGradeId(body.getJobGradeId())
                .jobGradeName(body.getJobGradeName())
                .jobTitleName(body.getJobTitleName())
                .hireDate(body.getHireDate())
                .eventTime(Instant.now())
                .build();
        salaryAutoCreateService.bootstrapSalary(e);
        return new ResponseEntity<>(
                ApiResponse.success(null, "Salary bootstrap 요청 처리됨"),
                HttpStatus.OK
        );
    }

    /** 급여 단건 조회 */
    @GetMapping("/{salaryId}")
    public ResponseEntity<?> findById(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID salaryId){
        return new ResponseEntity<>(
                ApiResponse.success(salaryService.findById(companyId, salaryId), "급여 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 회사별 급여 목록 조회 */
    @GetMapping
    public ResponseEntity<?> findByCompanyId(
            @RequestHeader("X-User-CompanyId") UUID companyId) {
        return new ResponseEntity<>(
                ApiResponse.success(salaryService.findByCompanyId(companyId), "급여 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 직원별 급여 이력 조회 */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<?> findByMemberId(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID memberId){
        return new ResponseEntity<>(
                ApiResponse.success(salaryService.findByMemberId(companyId, memberId), "직원별 급여 이력 조회 성공"),
                HttpStatus.OK
        );
    }

    /** 급여 수정 */
    @PutMapping("/{salaryId}")
    public ResponseEntity<?> update(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID salaryId,
            @Valid @RequestBody SalaryUpdateReqDto reqDto){
        return new ResponseEntity<>(
                ApiResponse.success(salaryService.update(companyId, salaryId, reqDto), "급여가 수정되었습니다."),
                HttpStatus.OK
        );
    }

    /** 급여 삭제 */
    @DeleteMapping("/{salaryId}")
    public ResponseEntity<?> delete(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @PathVariable UUID salaryId){
        salaryService.delete(companyId, salaryId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "급여가 삭제되었습니다."),
                HttpStatus.OK
        );
    }
}
