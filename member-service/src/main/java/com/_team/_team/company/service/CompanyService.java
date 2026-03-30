package com._team._team.company.service;

import com._team._team.company.domain.Company;
import com._team._team.company.dto.reqdto.CompanyOnboardingReqDto;
import com._team._team.company.repository.CompanyRepository;
import com._team._team.dto.BusinessException;
import com._team._team.member.service.MemberService;
import com._team._team.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final OrganizationService organizationService;
    private final MemberService memberService;
    private final NtsApiService ntsApiService;
    private final MailService mailService;
    private final RedisTemplate<String, String> emailRedisTemplate;

    public CompanyService(
            CompanyRepository companyRepository,
            OrganizationService organizationService,
            MemberService memberService,
            NtsApiService ntsApiService,
            MailService mailService,
            @Qualifier("emailInventory") RedisTemplate<String, String> emailRedisTemplate) {
        this.companyRepository = companyRepository;
        this.organizationService = organizationService;
        this.memberService = memberService;
        this.ntsApiService = ntsApiService;
        this.mailService = mailService;
        this.emailRedisTemplate = emailRedisTemplate;
    }

    // 이메일 인증 코드 발송
    public void sendVerificationCode(String email) {

        // 이메일 중복 체크
        if (memberService.existsByEmail(email)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "이미 사용중인 이메일입니다."
            );
        }

        // 6자리 인증 코드 생성
        String code = generateVerificationCode();

        // Redis DB1에 저장 (5분)
        emailRedisTemplate.opsForValue().set(
                "EMAIL_VERIFY:" + email,
                code,
                5,
                TimeUnit.MINUTES
        );

        log.info("이메일 인증 코드 발송: {} → {}", email, code);

        // 메일 발송
        mailService.sendVerificationCode(email, code);
    }

    // 이메일 인증 코드 확인
    public void verifyCode(String email, String code) {

        String savedCode = emailRedisTemplate.opsForValue().get("EMAIL_VERIFY:" + email);

        if (savedCode == null) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "인증 코드가 만료됐습니다."
            );
        }

        if (!savedCode.equals(code)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다."
            );
        }

        // 인증 완료 표시 (30분)
        emailRedisTemplate.opsForValue().set(
                "EMAIL_VERIFIED:" + email,
                "YES",
                30,
                TimeUnit.MINUTES
        );

        // 인증 코드 삭제
        emailRedisTemplate.delete("EMAIL_VERIFY:" + email);
    }

    // 회사 온보딩
    public void onboarding(CompanyOnboardingReqDto reqDto) {

        // 1. 사업자번호 검증 (국세청 API)
        ntsApiService.validate(reqDto.getBusinessNumber());

        // 2. 사업자번호 중복 체크
        if (companyRepository.existsByBusinessNumber(reqDto.getBusinessNumber())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "이미 등록된 사업자번호입니다."
            );
        }

        // 3. 이메일 인증 여부 확인
        String verified = emailRedisTemplate.opsForValue()
                .get("EMAIL_VERIFIED:" + reqDto.getAdminEmail());
        if (!"YES".equals(verified)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "이메일 인증이 필요합니다."
            );
        }

        // 4. 비밀번호 확인
        if (!reqDto.getAdminPassword().equals(reqDto.getAdminPasswordCheck())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."
            );
        }

        // 5. 회사 생성
        Company company = createCompany(reqDto);

        // 6. 기본 조직/직급/직책 생성
        organizationService.createDefaultOrganization(company);
        organizationService.createDefaultJobGrade(company);
        organizationService.createDefaultJobTitle(company);

        // 7. 기본 역할/권한 생성
        memberService.createDefaultRoles(company);

        // 8. 관리자 계정 생성
        memberService.createAdminMember(reqDto, company);

        // 9. 인증 정보 삭제
        emailRedisTemplate.delete("EMAIL_VERIFIED:" + reqDto.getAdminEmail());
    }

    private Company createCompany(CompanyOnboardingReqDto reqDto) {
        Company company = Company.builder()
                .companyName(reqDto.getCompanyName())
                .ceoName(reqDto.getCeoName())
                .businessNumber(reqDto.getBusinessNumber())
                .address(reqDto.getAddress())
                .detailAddress(reqDto.getDetailAddress())
                .build();
        return companyRepository.save(company);
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}