package com._team._team.salary.service;

import com._team._team.dto.BusinessException;
import com._team._team.salary.domain.RetirementPolicy;
import com._team._team.salary.domain.Salary;
import com._team._team.salary.domain.enums.RetirementType;
import com._team._team.salary.dto.reqdto.RetirementSimReqDto;
import com._team._team.salary.dto.resdto.RetirementSimResDto;
import com._team._team.salary.repository.RetirementPolicyRepository;
import com._team._team.salary.repository.SalaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

// 회사 정책 LEGAL DB DC 분기 퇴직금 추정
// LEGAL DB 평균임금 × 30 × 근속일수 365
// DC 평균월급 × 근속연수 매월 1 12 누적 근사
@Slf4j
@Service
@Transactional(readOnly = true)
public class RetirementSimulationService {

    private final RetirementPolicyRepository retirementPolicyRepository;
    private final SalaryRepository salaryRepository;

    @Autowired
    public RetirementSimulationService(RetirementPolicyRepository retirementPolicyRepository,
                                       SalaryRepository salaryRepository) {
        this.retirementPolicyRepository = retirementPolicyRepository;
        this.salaryRepository = salaryRepository;
    }

    public RetirementSimResDto simulateForMember(UUID companyId, UUID memberId, RetirementSimReqDto req) {
        // 1 회사 정책 조회 없으면 LEGAL 기본
        RetirementPolicy policy = retirementPolicyRepository
                .findActiveAt(companyId, LocalDate.now())
                .orElse(null);
        RetirementType type = policy != null ? policy.getRetirementType() : RetirementType.LEGAL;

        // 2 근속일수 프론트가 보낸 joinDate 사용
        if (req.getJoinDate() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "정산 시작일은 필수입니다.");
        }
        long days = ChronoUnit.DAYS.between(req.getJoinDate(), req.getResignDate());
        if (days < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "퇴직일이 입사일보다 빠릅니다.");
        }
        long years = days / 365;
        boolean eligible = days >= 365;

        // 3 평균 월급 활성 Salary 의 baseSalary 사용 salary-service 자체 데이터
        long avgMonthly = salaryRepository
                .findActiveSalary(memberId, companyId, LocalDate.now())
                .map(Salary::getBaseSalary)
                .orElse(0L);

        // 4 모드별 계산 1년 미만이면 0
        long estimated = eligible ? calculateByMode(type, avgMonthly, days, years) : 0L;

        log.info("[RETIREMENT-SIM] memberId={} type={} days={} avg={} estimated={}",
                memberId, type, days, avgMonthly, estimated);

        return RetirementSimResDto.builder()
                .retirementType(type)
                .modeDescription(describeMode(type))
                .memberId(memberId)
                .memberName(null)
                .joinDate(req.getJoinDate())
                .resignDate(req.getResignDate())
                .serviceDays(days)
                .avgMonthlyWage(avgMonthly)
                .estimatedAmount(estimated)
                .eligible(eligible)
                .disclaimer(disclaimer(type))
                .build();
    }

    private long calculateByMode(RetirementType type, long avgMonthly, long days, long years) {
        return switch (type) {
            case LEGAL, DB -> {
                // 평균임금 일액 30 일치 × 근속일수 365
                long avgDaily = avgMonthly / 30;
                yield avgDaily * 30 * days / 365;
            }
            case DC -> {
                // 평균 월급 × 근속연수 근사 매월 1 12 적립의 합
                yield avgMonthly * years;
            }
        };
    }

    private String describeMode(RetirementType t) {
        return switch (t) {
            case LEGAL -> "법정 퇴직금 사내 적립 일시금 지급";
            case DB -> "DB형 퇴직연금 외부 금융기관 운영 계산식 동일";
            case DC -> "DC형 퇴직연금 매월 1 12 외부 적립 운용수익 별도";
        };
    }

    private String disclaimer(RetirementType t) {
        return switch (t) {
            case LEGAL, DB ->
                    "본 추정치는 기본급 기준이며 실제 평균임금은 직전 3개월 임금총액 기준으로 별도 산정됩니다.";
            case DC ->
                    "본 추정치는 평균월급 기준 근사치이며 정확한 적립금은 가입 금융기관 화면에서 확인하세요.";
        };
    }

}