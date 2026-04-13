package com._team._team.salary.Service;

import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.domain.enums.AttendanceStatus;
import com._team._team.salary.Repository.SalaryPolicyRepository;
import com._team._team.salary.Repository.TaxRateRepository;
import com._team._team.salary.domain.SalaryPolicy;
import com._team._team.salary.domain.TaxRate;
import com._team._team.salary.domain.enums.*;
import com._team._team.salary.domain.vo.AutoPayrollItem;
import com._team._team.salary.domain.vo.SettlementPeriod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PayrollCalculationService {

    private final SalaryPolicyRepository salaryPolicyRepository;
    private final TaxRateRepository taxRateRepository;

    @Autowired
    public PayrollCalculationService(SalaryPolicyRepository salaryPolicyRepository, TaxRateRepository taxRateRepository) {
        this.salaryPolicyRepository = salaryPolicyRepository;
        this.taxRateRepository = taxRateRepository;
    }

    // 1. 정산기간 산출
    public SettlementPeriod calculateSettlementPeriod(UUID companyId, LocalDate payrollDate){

        // 활성 급여정책 조회
        List<SalaryPolicy> policies = salaryPolicyRepository.findActivePolicies(companyId, payrollDate);

        // 정책 없으면 fallback : 당월 1일 ~ 당월 말일
        if(policies.isEmpty()){
            return new SettlementPeriod(
                    payrollDate.withDayOfMonth(1),
                    payrollDate.withDayOfMonth(payrollDate.lengthOfMonth())
            );
        }
        // 가장 최신 정책
        SalaryPolicy policy = policies.get(0);
        int payDay = policy.getPayDay();

        // 정산 시작일 결정
        LocalDate from;
        if(policy.getPeriodStartType() == PeriodStartType.FIRST){
            // FIRST: 당월 1일
            from = payrollDate.withDayOfMonth(1);
        } else {
            // SPECIFIC: 전월 (payDay + 1)일
            LocalDate prevMonth = payrollDate.minusMonths(1);
            int startDay = payDay + 1;

            if(startDay > prevMonth.lengthOfMonth()){
                // payDay+1 이 전월 일수 초과 시 -> 당월 1일로 보정
                from = payrollDate.withDayOfMonth(1);
            }else {
                from = prevMonth.withDayOfMonth(startDay);
            }
        }

        // 정산 종료일 결정
        LocalDate to;
        if(policy.getPeriodEndType() == PeriodEndType.LAST) {
            // LAST : 당월 말일
            to = payrollDate.withDayOfMonth(payrollDate.lengthOfMonth());
        }else {
            // SPECIFIC : 당월 payDay (말일 보정)
            int endDay = Math.min(payDay, payrollDate.lengthOfMonth());
            to = payrollDate.withDayOfMonth(endDay);
        }

        return new SettlementPeriod(from, to);
    }

    // 2. 초과근무 인정 시간 계산 - FLOOR(내림) 방식
    private int calculateRecognizedOvertime(int rawMinutes, Integer unitMinutes){
        // unitMinutes가 null 또는 1이하면 절사 없음 (1분 단위)
        if(unitMinutes == null || unitMinutes <= 1){
            return rawMinutes;
        }
        // 정수 나눗셈 = 자동 내림
        return (rawMinutes / unitMinutes) * unitMinutes;
    }

    // 3. 활성 급여정책 단건 조회
    public SalaryPolicy findActivePolicy(UUID companyId, LocalDate payrollDate) {
        List<SalaryPolicy> policies = salaryPolicyRepository.findActivePolicies(companyId, payrollDate);
        return policies.isEmpty() ? null : policies.get(0);
    }

    // 4. 초과근무수당 계산 (포괄임금제와 비포괄임금제 분기처리)
    /**
     * 정산기간 내 초과근무수당 자동 계산
     * [통상시급 산출] == (한주 소정근로시간(40) + 주휴시간(8)) / 7 × 365 / 12
     * 한국 근로기준법: 월 소정근로시간 = 209시간
     * 시급 = baseSalary / 209
     * 근로기준법 제56조: 연장근로에 대해서는 통상임금의 50% 이상을 가산
     */
    public AutoPayrollItem calculateOvertimePay(
            List<DailyAttendance> attendances,
            Long baseSalary,
            SalaryPolicy policy){

        // 1) 인정 시간 합산
        Integer roundingUnit = policy != null ? policy.getOvertimeRoundingMinutes() : null;
        int totalApprovedOvertimeMinutes = 0;
        for(DailyAttendance da : attendances){
            if(da.getOvertimeMinutes() != null && da.getOvertimeMinutes() > 0){
                totalApprovedOvertimeMinutes += calculateRecognizedOvertime(
                        da.getOvertimeMinutes(), roundingUnit);
            }
        }

        // 초과근무가 없으면 항목 생성 안함
        if (totalApprovedOvertimeMinutes == 0){
            return null;
        }

        // 2) 임금제 분기: 포괄이면 고정초과근무수당 만큼 차감, 비포괄이면 그대로
        WageSystemType wageType = policy != null
                ? policy.getWageSystemType()
                : WageSystemType.NON_COMPREHENSIVE;

        int payableOvertimeMinutes;
        if (wageType == WageSystemType.COMPREHENSIVE) {
            int fixedOt = policy.getFixedOvertimeMinutes() == null
                    ? 0 : policy.getFixedOvertimeMinutes();
            payableOvertimeMinutes = Math.max(0, totalApprovedOvertimeMinutes - fixedOt);
        } else {
            payableOvertimeMinutes = totalApprovedOvertimeMinutes;
        }

        // 3) 포함 범위 내면 추가 지급 없음 → 항목 미생성
        if (payableOvertimeMinutes == 0) {
            return null;
        }

        // 4) 통상시급 × 추가지급분 × 1.5배 , 통상시급 : 기본급 / 209시간 (한국 근로기준법 소정근로시간)
        double hourlyRate = baseSalary / 209.0;

        // 초과근무수당 : 초과시간 x 시급 x 1.5 (50% 가산)
        double overtimePay = (payableOvertimeMinutes / 60.0) * hourlyRate * 1.5;

        return new AutoPayrollItem(
                "초과근무수당",
                ItemType.EARNING,
                (long) Math.floor(overtimePay), // 원 미만 절사
                90, // displayOrder: 기본급(10) 다음 그룹
                "Y"  // 초과근무수당은 과세 (근로의 대가)
        );
    }

    // 5. 공휴일근무수당 계산 (통상시급 × 공휴일 근무시간 × 1.5배)
    public AutoPayrollItem calculateHolidayWorkPay(
            List<DailyAttendance> attendances,
            Set<LocalDate> paidHolidayDates,
            Long baseSalary){

        // 유급공휴일이 없으면 계산 불필요
        if(paidHolidayDates == null || paidHolidayDates.isEmpty()){
            return null;
        }

        int holidayWorkedMinutes = 0;

        for(DailyAttendance da : attendances){
            // 해당일이 유급공휴일이고, 정상 출근해서 근무한 경우만 수당 발생
            if(paidHolidayDates.contains(da.getAttendanceDate())){
                if(da.getStatus() == AttendanceStatus.NORMAL
                        && da.getWorkedMinutes() != null
                        && da.getWorkedMinutes() > 0){
                    holidayWorkedMinutes += da.getWorkedMinutes();
                }
            }
        }

        // 공휴일 출근 실적 없으면 항목 생성 안함
        if(holidayWorkedMinutes == 0){
            return null;
        }

        // 통상시급 x 공휴일 근무시간 x 1.5배
        double hourlyRate = baseSalary / 209.0;
        double holidayPay = (holidayWorkedMinutes / 60.0) * hourlyRate * 1.5;

        return new AutoPayrollItem(
                "공휴일 근무수당",
                ItemType.EARNING,
                (long) Math.floor(holidayPay), // 원 미만 절사
                91, //  displayOrder: 초과근무수당(90) 바로 다음
                "Y"  // 공휴일근무수당도 과세
        );
    }

    // 6. 세율 기반 공제 계산
    /**
     * 4대보험 + 소득세 + 지방소득세 자동 계산
     */
    public List<AutoPayrollItem> calculateDeductions(long totalTaxableEarning, LocalDate payrollDate){

        int year = payrollDate.getYear();
        List<TaxRate> taxRates = taxRateRepository.findByApplyYear(year);

        // 세율 미등록 시 빈 리스트 반환
        if(taxRates.isEmpty()){
            return Collections.emptyList();
        }

        // TaxType -> TaxRate 매핑
        Map<TaxType, TaxRate> rateMap = taxRates.stream()
                .collect(Collectors.toMap(TaxRate::getTaxType, tr-> tr, (a,b) -> a));

        List<AutoPayrollItem> deductions = new ArrayList<>();
        int displayOrder = 200; // 공제 항목은 200번대부터

        // 국민연금
        long nationalPension = calculateSingleDeduction(
                rateMap, TaxType.NATIONAL_PENSION, totalTaxableEarning);
        if (nationalPension > 0){
            deductions.add(new AutoPayrollItem("국민연금", ItemType.DEDUCTION, nationalPension, displayOrder++, "Y"));
        }

        // 건강보험
        long healthInsurance = calculateSingleDeduction(
                rateMap, TaxType.HEALTH_INSURANCE, totalTaxableEarning);
        if (healthInsurance > 0) {
            deductions.add(new AutoPayrollItem("건강보험", ItemType.DEDUCTION, healthInsurance, displayOrder++, "Y"));
        }

        // 장기요양보험 : 건강보험료 기준
        long longTermCare = calculateSingleDeduction(
                rateMap, TaxType.LONG_TERM_CARE, healthInsurance);
        if (longTermCare > 0) {
            deductions.add(new AutoPayrollItem("장기요양보험", ItemType.DEDUCTION, longTermCare, displayOrder++, "Y"));
        }

        // 고용보험
        long employmentInsurance = calculateSingleDeduction(
                rateMap, TaxType.EMPLOYMENT_INSURANCE, totalTaxableEarning);
        if (employmentInsurance > 0) {
            deductions.add(new AutoPayrollItem("고용보험", ItemType.DEDUCTION, employmentInsurance, displayOrder++, "Y"));
        }

        // 소득세
        long incomeTax = calculateSingleDeduction(
                rateMap, TaxType.INCOME_TAX, totalTaxableEarning);
        if (incomeTax > 0) {
            deductions.add(new AutoPayrollItem("소득세", ItemType.DEDUCTION, incomeTax, displayOrder++, "Y"));
        }

        // 지방소득세 : 소득세 기준
        long localIncomeTax = calculateSingleDeduction(
                rateMap, TaxType.LOCAL_INCOME_TAX, incomeTax);  // ← 소득세 기준!
        if (localIncomeTax > 0) {
            deductions.add(new AutoPayrollItem("지방소득세", ItemType.DEDUCTION, localIncomeTax, displayOrder++, "Y"));
        }

        return deductions;
    }

    /**
     * 단일 세금 항목 공제액 계산
     */
    private long calculateSingleDeduction(Map<TaxType, TaxRate> rateMap, TaxType taxType, long baseAmount){
        TaxRate taxRate = rateMap.get(taxType);

        if(taxRate == null){
            return 0L;
        }

        double amount = taxRate.getRate().doubleValue() * baseAmount;
        return (long) Math.floor(amount);
    }
}
