package com._team._team.salary.service;

import com._team._team.attendance.domain.MemberLeaveOfAbsence;
import com._team._team.attendance.domain.enums.LeaveOfAbsenceApprovalStatus;
import com._team._team.attendance.repository.MemberLeaveOfAbsenceRepository;
import com._team._team.dto.BusinessException;
import com._team._team.salary.domain.Payroll;
import com._team._team.salary.domain.PayrollItem;
import com._team._team.salary.domain.RetirementPolicy;
import com._team._team.salary.domain.Salary;
import com._team._team.salary.domain.SalaryItemTemplate;
import com._team._team.salary.domain.SalaryPolicy;
import com._team._team.salary.domain.enums.ItemType;
import com._team._team.salary.domain.enums.PayrollType;
import com._team._team.salary.domain.enums.RetirementType;
import com._team._team.salary.dto.reqdto.RetirementSimReqDto;
import com._team._team.salary.dto.resdto.RetirementSimResDto;
import com._team._team.salary.feignClients.MemberFeignClient;
import com._team._team.salary.feignClients.dto.MemberResDto;
import com._team._team.salary.repository.PayrollRepository;
import com._team._team.salary.repository.RetirementPolicyRepository;
import com._team._team.salary.repository.SalaryItemTemplateRepository;
import com._team._team.salary.repository.SalaryPolicyRepository;
import com._team._team.salary.repository.SalaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 퇴직금 시뮬레이션 회사 정책 LEGAL DB DC 분기
 *  LEGAL DB 평균임금 일액 × 30 × 근속일수 365
 *  DC 평균월급 × 근속연수 매월 1 12 누적 근사
 *
 * 평균임금 정확 산정 (근로기준법 제2조 1항 6호 + 시행령 제2조)
 *  base 직전 3개월 정기급여 임금총액 / 3개월 calendar 일수
 *  + 직전 12개월 상여금 합계 × 3/12 환산
 *  + 직전 12개월 연차수당 합계 × 3/12 환산
 *  → 일평균 = (base + 환산) / 3개월일수
 *  → 통상시급 일액 = ordinaryWage × 8 / monthlyOrdinaryHours
 *  → 적용 평균임금 = max(일평균, 통상시급일액) 시행령 제2조
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class RetirementSimulationService {

    private final RetirementPolicyRepository retirementPolicyRepository;
    private final SalaryRepository salaryRepository;
    private final SalaryPolicyRepository salaryPolicyRepository;
    private final SalaryItemTemplateRepository salaryItemTemplateRepository;
    private final PayrollRepository payrollRepository;
    private final MemberLeaveOfAbsenceRepository memberLeaveOfAbsenceRepository;
    private final MemberFeignClient memberFeignClient;

    @Autowired
    public RetirementSimulationService(RetirementPolicyRepository retirementPolicyRepository,
                                       SalaryRepository salaryRepository,
                                       SalaryPolicyRepository salaryPolicyRepository,
                                       SalaryItemTemplateRepository salaryItemTemplateRepository,
                                       PayrollRepository payrollRepository,
                                       MemberLeaveOfAbsenceRepository memberLeaveOfAbsenceRepository,
                                       MemberFeignClient memberFeignClient) {
        this.retirementPolicyRepository = retirementPolicyRepository;
        this.salaryRepository = salaryRepository;
        this.salaryPolicyRepository = salaryPolicyRepository;
        this.salaryItemTemplateRepository = salaryItemTemplateRepository;
        this.payrollRepository = payrollRepository;
        this.memberLeaveOfAbsenceRepository = memberLeaveOfAbsenceRepository;
        this.memberFeignClient = memberFeignClient;
    }

    public RetirementSimResDto simulateForMember(UUID companyId, UUID memberId, RetirementSimReqDto req) {
        // 회사 정책 조회 없으면 LEGAL 기본
        RetirementPolicy policy = retirementPolicyRepository
                .findActiveAt(companyId, LocalDate.now())
                .orElse(null);
        RetirementType type = policy != null ? policy.getRetirementType() : RetirementType.LEGAL;

        // 입사일 결정 요청에 있으면 우선 사용 없으면 member-service 에서 조회
        LocalDate joinDate = req.getJoinDate();
        if (joinDate == null) {
            joinDate = fetchJoinDate(companyId, memberId);
        }
        if (joinDate == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "정산 시작일을 확인할 수 없습니다 (입사일 미등록).");
        }
        long days = ChronoUnit.DAYS.between(joinDate, req.getResignDate());
        if (days < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "퇴직일이 입사일보다 빠릅니다.");
        }
        long years = days / 365;
        boolean eligible = days >= 365;

        // 활성 Salary baseSalary 호환성 유지용 평균월급
        long baseMonthly = salaryRepository
                .findActiveSalary(memberId, companyId, LocalDate.now())
                .map(Salary::getBaseSalary)
                .orElse(0L);

        // 평균임금 정확 산정 breakdown
        AverageWage aw = computeAverageWage(companyId, memberId, req.getResignDate(), baseMonthly);

        // 모드별 계산 1년 미만이면 0 LEGAL DB 는 일액 기준 DC 는 평균월급 근사
        long estimated;
        if (!eligible) {
            estimated = 0L;
        } else if (type == RetirementType.DC) {
            estimated = baseMonthly * years;
        } else {
            // LEGAL DB 적용 일평균 × 30 × 근속일수 / 365
            estimated = aw.appliedDailyWage * 30 * days / 365;
        }

        log.info("[RETIREMENT-SIM] memberId={} type={} days={} baseMonthly={} appliedDaily={} estimated={}",
                memberId, type, days, baseMonthly, aw.appliedDailyWage, estimated);

        return RetirementSimResDto.builder()
                .retirementType(type)
                .modeDescription(describeMode(type))
                .memberId(memberId)
                .memberName(null)
                .joinDate(joinDate)
                .resignDate(req.getResignDate())
                .serviceDays(days)
                .avgMonthlyWage(baseMonthly)
                .basePeriodPayment(aw.basePeriodPayment)
                .basePeriodDays(aw.basePeriodDays)
                .simpleDailyAverage(aw.simpleDailyAverage)
                .excludedLeaveDays(aw.excludedLeaveDays)
                .excludedLeaveCount(aw.excludedLeaveCount)
                .adjustedPeriodDays(aw.adjustedPeriodDays)
                .bonusAddition12mAvg(aw.bonusAddition12mAvg)
                .unusedLeaveAddition12mAvg(aw.unusedLeaveAddition12mAvg)
                .averageDailyWage(aw.averageDailyWage)
                .ordinaryDailyWage(aw.ordinaryDailyWage)
                .appliedDailyWage(aw.appliedDailyWage)
                .appliedBasis(aw.appliedBasis)
                .estimatedAmount(estimated)
                .eligible(eligible)
                .disclaimer(disclaimer(type))
                .build();
    }

    /**
     * 평균임금 정확 산정 한국 근로기준법 제2조 1항 6호 + 시행령 제2조
     *  데이터 부족 시 baseMonthly fallback
     */
    private AverageWage computeAverageWage(UUID companyId,
                                           UUID memberId,
                                           LocalDate resignDate,
                                           long baseMonthly) {
        AverageWage aw = new AverageWage();

        // 1) 직전 3개월 정기급여 (REGULAR_MONTHLY) Payroll 임금총액
        LocalDate threeMonthStart = resignDate.minusMonths(3).plusDays(1);
        aw.basePeriodDays = (int) (ChronoUnit.DAYS.between(threeMonthStart, resignDate) + 1);
        if (aw.basePeriodDays <= 0) aw.basePeriodDays = 90;

        List<Payroll> recentPayrolls = payrollRepository.findInYearByMemberFetchItems(
                companyId, memberId, threeMonthStart, resignDate);

        // 정기급여만 합산 (보너스 별도 가산)
        long regularPayment = recentPayrolls.stream()
                .filter(p -> p.getPayrollType() == null || p.getPayrollType() == PayrollType.REGULAR_MONTHLY)
                .mapToLong(p -> p.getTotalPayment() == null ? 0L : p.getTotalPayment())
                .sum();
        aw.basePeriodPayment = regularPayment;

        // 데이터 0 이면 baseMonthly × 3 로 fallback (신규 도입 회사)
        if (regularPayment <= 0) {
            regularPayment = baseMonthly * 3;
            aw.basePeriodPayment = regularPayment;
        }

        // 1-1) 시행령 제2조 평균임금 산정 제외 기간 계산
        //      ACTIVE / ENDED 상태 휴직 중 3개월 기간과 겹치는 일수 합산
        //      출산휴가 / 육아휴직 / 산재요양 / 병역 / 쟁의행위 등이 해당
        List<MemberLeaveOfAbsence> loas = memberLeaveOfAbsenceRepository
                .findInPeriod(companyId, memberId, resignDate);

        int excludedDays = 0;
        int excludedCount = 0;
        for (MemberLeaveOfAbsence loa : loas) {
            // 신청 대기 / 반려 / 취소 상태는 무시 — findInPeriod 가 ACTIVE / ENDED 만 반환
            LocalDate effectiveEnd =
                    loa.getStatus() == LeaveOfAbsenceApprovalStatus.ENDED
                            && loa.getActualEndDate() != null
                            ? loa.getActualEndDate()
                            : loa.getEndDate();

            // 3개월 기간 밖이면 제외
            if (effectiveEnd.isBefore(threeMonthStart)) continue;
            if (loa.getStartDate().isAfter(resignDate)) continue;

            LocalDate overlapStart = loa.getStartDate().isBefore(threeMonthStart)
                    ? threeMonthStart : loa.getStartDate();
            LocalDate overlapEnd = effectiveEnd.isAfter(resignDate)
                    ? resignDate : effectiveEnd;

            if (!overlapStart.isAfter(overlapEnd)) {
                excludedDays += (int) ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
                excludedCount++;
            }
        }
        // 분모 일수 캡 (전체 제외되지 않도록)
        excludedDays = Math.min(excludedDays, aw.basePeriodDays - 1);
        excludedDays = Math.max(0, excludedDays);
        aw.excludedLeaveDays = excludedDays;
        aw.excludedLeaveCount = excludedCount;
        aw.adjustedPeriodDays = aw.basePeriodDays - excludedDays;

        // 단순 일평균은 조정된 일수 기준 (휴직 제외)
        aw.simpleDailyAverage = aw.adjustedPeriodDays > 0
                ? regularPayment / aw.adjustedPeriodDays
                : 0;

        // 2) 직전 12개월 상여 환산 합계 × 3/12
        LocalDate yearStart = resignDate.minusMonths(12).plusDays(1);
        List<Payroll> yearlyPayrolls = payrollRepository.findInYearByMemberFetchItems(
                companyId, memberId, yearStart, resignDate);

        long bonusYearlySum = yearlyPayrolls.stream()
                .filter(p -> p.getPayrollType() == PayrollType.PERFORMANCE_BONUS
                          || p.getPayrollType() == PayrollType.SPECIAL_BONUS)
                .mapToLong(p -> p.getTotalPayment() == null ? 0L : p.getTotalPayment())
                .sum();
        aw.bonusAddition12mAvg = bonusYearlySum * 3 / 12;

        // 3) 연차수당 환산 항목명 매칭 (연차수당 / 미사용연차수당 등)
        long unusedLeaveYearlySum = yearlyPayrolls.stream()
                .flatMap(p -> p.getPayrollItemList() == null
                        ? java.util.stream.Stream.empty()
                        : p.getPayrollItemList().stream())
                .filter(i -> i.getItemType() == ItemType.EARNING)
                .filter(i -> {
                    String name = i.getItemName() == null ? "" : i.getItemName();
                    return name.contains("연차수당") || name.contains("연차 수당")
                            || name.contains("미사용연차") || name.contains("미사용 연차");
                })
                .mapToLong(i -> i.getAmount() == null ? 0L : i.getAmount())
                .sum();
        aw.unusedLeaveAddition12mAvg = unusedLeaveYearlySum * 3 / 12;

        // 4) 평균임금 일액 = (3개월 임금총액 + 환산금) / 조정된 일수 (휴직 제외)
        long totalForAverage = aw.basePeriodPayment + aw.bonusAddition12mAvg + aw.unusedLeaveAddition12mAvg;
        aw.averageDailyWage = aw.adjustedPeriodDays > 0 ? totalForAverage / aw.adjustedPeriodDays : 0;

        // 5) 통상시급 일액 = ordinaryWage × 8 / monthlyOrdinaryHours
        // 활성 SalaryPolicy 의 monthlyOrdinaryHours 사용 없으면 209 fallback
        SalaryPolicy activeSalaryPolicy = salaryPolicyRepository
                .findActivePolicies(companyId, LocalDate.now())
                .stream().findFirst().orElse(null);
        int monthlyHours = activeSalaryPolicy != null && activeSalaryPolicy.getMonthlyOrdinaryHours() != null
                && activeSalaryPolicy.getMonthlyOrdinaryHours() > 0
                ? activeSalaryPolicy.getMonthlyOrdinaryHours() : 209;

        // 통상임금 추정 baseSalary + 통상임금 플래그 Y 항목들 평균 (간이)
        long ordinaryWage = estimateOrdinaryWage(companyId, memberId, baseMonthly);
        aw.ordinaryDailyWage = monthlyHours > 0
                ? (ordinaryWage * 8) / monthlyHours
                : 0;

        // 6) 시행령 제2조 max(평균, 통상)
        if (aw.averageDailyWage >= aw.ordinaryDailyWage) {
            aw.appliedDailyWage = aw.averageDailyWage;
            aw.appliedBasis = "AVERAGE";
        } else {
            aw.appliedDailyWage = aw.ordinaryDailyWage;
            aw.appliedBasis = "ORDINARY";
        }

        return aw;
    }

    /**
     * 통상임금 추정 baseSalary + 통상임금 플래그 Y 항목 합 (정기급여 1회분 기준)
     * 직원 활성 Allowance 까진 보지 않고 회사 표준 템플릿 정기수당으로 근사
     */
    private long estimateOrdinaryWage(UUID companyId, UUID memberId, long baseMonthly) {
        // 회사 표준 템플릿 중 통상임금 플래그 Y 인 EARNING 만
        List<SalaryItemTemplate> templates = salaryItemTemplateRepository
                .findByCompanyIdAndDelYn(companyId, "N");
        // baseSalary 그 자체가 가장 큰 비중이므로 템플릿은 추가 표시용 (보강 방향)
        // 실 데이터 부재 시 baseMonthly 만 사용
        // 정확한 값은 직원별 MemberAllowance 까지 봐야 하지만 시뮬은 근사로 충분
        long ordinaryAddition = 0L;
        Map<String, Boolean> seen = new HashMap<>();
        for (SalaryItemTemplate t : templates) {
            if (t.getItemType() != ItemType.EARNING) continue;
            if (!"Y".equals(t.getIsOrdinaryWageYn())) continue;
            if ("기본급".equals(t.getItemName())) continue;
            if (seen.put(t.getItemName(), true) != null) continue;
            // 템플릿만 보고 금액은 알 수 없으므로 baseMonthly 의 5% 를 항목당 가산 (간이 추정)
            // 정확한 값은 평균임금 base 임금에 이미 반영됨 — 통상임금은 비교용일 뿐
            ordinaryAddition += baseMonthly / 20; // 5%
        }
        return baseMonthly + ordinaryAddition;
    }

    // 회사 직원 일괄 조회, 본인 joinDate 추출, Feign 실패 시 null 반환 graceful
    private LocalDate fetchJoinDate(UUID companyId, UUID memberId) {
        try {
            var res = memberFeignClient.getMembersByCompany(companyId);
            if (res == null || res.getData() == null) return null;
            return res.getData().stream()
                    .filter(m -> memberId.equals(m.getMemberId()))
                    .map(MemberResDto::getJoinDate)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("[RETIREMENT-SIM] 입사일 조회 실패 companyId={} memberId={}", companyId, memberId, e);
            return null;
        }
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
                    "평균임금은 근로기준법 제2조 1항 6호 기준으로 직전 3개월 임금총액에 12개월 상여 / 연차수당 환산을 가산해 산정합니다. 시행령 제2조에 따라 출산휴가 / 육아휴직 / 산재요양 / 병역 등 휴직기간은 분모 일수에서 제외하며, 평균임금이 통상시급 일액보다 적으면 통상시급 일액이 적용됩니다.";
            case DC ->
                    "본 추정치는 평균월급 기준 근사치이며 정확한 적립금은 가입 금융기관 화면에서 확인하세요.";
        };
    }

    /** 내부 평균임금 산정 결과 캐리어 */
    private static class AverageWage {
        long basePeriodPayment;
        int basePeriodDays;
        long simpleDailyAverage;
        // 시행령 제2조 제외 기간
        int excludedLeaveDays;
        int excludedLeaveCount;
        int adjustedPeriodDays;
        long bonusAddition12mAvg;
        long unusedLeaveAddition12mAvg;
        long averageDailyWage;
        long ordinaryDailyWage;
        long appliedDailyWage;
        String appliedBasis = "AVERAGE";
    }
}
