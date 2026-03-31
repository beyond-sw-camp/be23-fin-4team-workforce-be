package com._team._team.company.controller;

import com._team._team.company.dto.reqdto.CompanyOnboardingReqDto;
import com._team._team.company.service.CompanyService;
import com._team._team.company.service.NtsApiService;
import com._team._team.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/company")
public class CompanyController {

    private final CompanyService companyService;
    private final NtsApiService ntsApiService;

    @Autowired
    public CompanyController(CompanyService companyService, NtsApiService ntsApiService) {
        this.companyService = companyService;
        this.ntsApiService = ntsApiService;
    }

    // 사업자번호 검증
    @GetMapping("/check-business-number")
    public ResponseEntity<?> checkBusinessNumber(
            @RequestParam String businessNumber) {
        ntsApiService.validate(businessNumber);
        return new ResponseEntity<>(
                ApiResponse.success(null, "유효한 사업자번호입니다."),
                HttpStatus.OK
        );
    }

    // 이메일 인증 코드 발송
    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(
            @RequestParam String email) {
        companyService.sendVerificationCode(email);
        return new ResponseEntity<>(
                ApiResponse.success(null, "인증 코드가 발송됐습니다."),
                HttpStatus.OK
        );
    }

    // 이메일 인증 코드 확인
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(
            @RequestParam String email,
            @RequestParam String code) {
        companyService.verifyCode(email, code);
        return new ResponseEntity<>(
                ApiResponse.success(null, "이메일 인증이 완료됐습니다."),
                HttpStatus.OK
        );
    }

    // 회사 온보딩
    @PostMapping("/onboarding")
    public ResponseEntity<?> onboarding(
            @Valid @RequestBody CompanyOnboardingReqDto reqDto) {
        companyService.onboarding(reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(null, "온보딩이 완료됐습니다."),
                HttpStatus.CREATED
        );
    }
}