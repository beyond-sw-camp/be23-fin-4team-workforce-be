package com._team._team.salary.service;

import com._team._team.attendance.domain.MonthlyAttendanceLedger;
import com._team._team.dto.BusinessException;
import com._team._team.salary.repository.PayrollItemRepository;
import com._team._team.salary.repository.PayrollRepository;
import com._team._team.salary.repository.SalaryItemTemplateRepository;
import com._team._team.salary.repository.SalaryRepository;
import com._team._team.salary.domain.*;
import com._team._team.salary.domain.enums.ItemType;
import com._team._team.salary.domain.enums.PayrollStatus;
import com._team._team.salary.domain.vo.AutoPayrollItem;
import com._team._team.salary.domain.vo.SettlementPeriod;
import com._team._team.salary.dto.reqdto.PayrollCreateReqDto;
import com._team._team.salary.dto.reqdto.PayrollItemCreateReqDto;
import com._team._team.salary.dto.reqdto.PayrollItemUpdateReqDto;
import com._team._team.salary.dto.reqdto.PayrollUpdateReqDto;
import com._team._team.salary.dto.resdto.PayrollItemResDto;
import com._team._team.salary.dto.resdto.PayrollResDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Transactional
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final PayrollItemRepository payrollItemRepository;
    private final SalaryRepository salaryRepository;
    private final SalaryItemTemplateRepository salaryItemTemplateRepository;
    private final PayrollCalculationService payrollCalculationService;
    private final MemberAllowanceService memberAllowanceService;

    @Autowired
    public PayrollService(PayrollRepository payrollRepository,
                          PayrollItemRepository payrollItemRepository,
                          SalaryRepository salaryRepository,
                          SalaryItemTemplateRepository salaryItemTemplateRepository,
                          PayrollCalculationService payrollCalculationService, MemberAllowanceService memberAllowanceService){
        this.payrollRepository = payrollRepository;
        this.payrollItemRepository = payrollItemRepository;
        this.salaryRepository = salaryRepository;
        this.salaryItemTemplateRepository = salaryItemTemplateRepository;
        this.payrollCalculationService = payrollCalculationService;
        this.memberAllowanceService = memberAllowanceService;
    }

    /**
     * 급여대장 생성
     * [처리 흐름]
     * 1) 직원의 정산일 기준 적용 중인 Salary 자동 조회
     * 2) 동일 직원 + 동일 정산일 중복 체크
     * 3) 정산기간 산출 -> 월 장부(MonthlyAttendanceLedger) 조회
     * 4) Payroll 엔티티 생성 (금액 0으로 초기화)
     * 5) 회사 SalaryItemTemplate 목록 -> PayrollItem 일괄 생성
     * 6) 장부 기반으로 초과근무, 공휴일, 야간 수당 자동 계산
     * 7) 과세소득 기반 4대보험, 세금 자동 공제
     * 8) EARNING/DEDUCTION 합계 계산 -> Payroll.recalculate()
     */
    public PayrollResDto createPayroll(UUID companyId, PayrollCreateReqDto reqDto){

        /** 1. 직원의 정산일 기준 적용 중인 급여 조회 */
        Salary salary = salaryRepository.findActiveSalary(
                        reqDto.getMemberId(), companyId, reqDto.getPayrollYearMonthDay())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "해당 직원의 적용 중인 급여 정보를 찾을 수 없습니다."));

        /**
         * 2. 직원 + 정산일 기존 이력 체크
         */
        payrollRepository.findByCompanyIdAndMemberIdAndPayrollYearMonthDay(
                        companyId, reqDto.getMemberId(), reqDto.getPayrollYearMonthDay())
                .ifPresent(existing -> {
                    if ("N".equals(existing.getDelYn())) {
                        throw new BusinessException(HttpStatus.CONFLICT,
                                "해당 직원의 동일 정산일에 이미 급여대장이 존재합니다.");
                    } else {
                        throw new BusinessException(HttpStatus.CONFLICT,
                                "해당 직원의 동일 정산일에 이전 급여대장 이력이 존재합니다. 다른 정산일로 생성하거나 기존 대장을 수정해주세요.");
                    }
                });

        /** 3. 정산기간 계산 */
        SettlementPeriod period = payrollCalculationService.calculateSettlementPeriod(
                companyId, reqDto.getPayrollYearMonthDay());

        /** 4. Payroll 생성 (금액은 0으로 초기화, 항목 생성 후 재계산) */
        Payroll payroll = reqDto.toEntity(companyId, salary.getSalaryId());
        Payroll saved;
        try {
            saved = payrollRepository.saveAndFlush(payroll);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "동시에 동일한 급여대장 생성 요청이 감지되었습니다. 잠시 후 다시 시도해주세요.");
        }

        /** 5. 회사의 급여 항목 템플릿 조회 -> PayrollItem 일괄 생성 */
        List<SalaryItemTemplate> templates = salaryItemTemplateRepository
                .findByCompanyIdAndDelYn(companyId, "N");

        // 5-1. 정산일 기준 활성 수당(APPROVED, AUTO) 조회
        List<MemberAllowance> activeAllowances = memberAllowanceService.findActiveAt(
                reqDto.getMemberId(), companyId, reqDto.getPayrollYearMonthDay());
        Map<UUID, Long> allowanceByTemplate = activeAllowances.stream()
                .collect(Collectors.toMap(
                        MemberAllowance::getSalaryItemTemplateId,
                        MemberAllowance::getAmount,
                        (a, b) -> a));

        long totalPayment = 0L;
        long totalTaxableEarning = 0L;   // 과세 대상 EARNING 합계 (4대보험/소득세 기준액)
        long totalDeduction = 0L;
        List<PayrollItem> payrollItems = new ArrayList<>();

        for (SalaryItemTemplate template : templates) {
            /**
             * 기본급은 baseSalary, 그 외 지급 항목은 MemberAllowance 금액, 없으면 0원
             */
            long amount;
            if (template.getItemName().equals("기본급")) {
                amount = salary.getBaseSalary();
            } else {
                amount = allowanceByTemplate.getOrDefault(
                        template.getSalaryItemTemplateId(), 0L);
            }

            // payrollItem 생성
            PayrollItem payItem = PayrollItem.fromTemplate(saved, template, amount);
            payrollItems.add(payItem);

            // EARNING/DEDUCTION 분류하여 합계 누적
            if (template.getItemType() == ItemType.EARNING) {
                totalPayment += amount;
                // 과세 항목만 과세 총액에 누적 (비과세 식대, 비과세 교통비 등 제외)
                if ("Y".equals(template.getIsTaxableYn())) {
                    totalTaxableEarning += amount;
                }
            } else {
                totalDeduction += amount;
            }
        }

        /**
         * 6. 월 장부 조회
         */
        YearMonth targetMonth = YearMonth.from(period.from());
        MonthlyAttendanceLedger ledger = payrollCalculationService.findLedger(
                reqDto.getMemberId(), targetMonth);

        /** 7. 활성 급여정책 조회 (포괄, 비포괄 + fixed OT) */
        SalaryPolicy activePolicy = payrollCalculationService.findActivePolicy(
                companyId, reqDto.getPayrollYearMonthDay());

        /** 7-1. 초과근무수당 자동 계산 */
        AutoPayrollItem overtimeItem = payrollCalculationService.calculateOvertimePay(
                ledger,
                salary.getBaseSalary(),
                activePolicy);

        if (overtimeItem != null){
            PayrollItem payItem = buildPayrollItem(saved, overtimeItem);
            payrollItems.add(payItem);
            totalPayment += overtimeItem.amount();
            // 초과근무수당은 과세이므로 과세 총액에 누적
            if ("Y".equals(overtimeItem.isTaxableYn())) {
                totalTaxableEarning += overtimeItem.amount();
            }
        }

        /** 8. 공휴일근무수당 자동 계산 */
        AutoPayrollItem holidayItem = payrollCalculationService.calculateHolidayWorkPay(
                ledger, salary.getBaseSalary());

        if(holidayItem != null){
            PayrollItem payrollItem = buildPayrollItem(saved, holidayItem);
            payrollItems.add(payrollItem);
            totalPayment += holidayItem.amount();
            // 공휴일근무수당은 과세
            if ("Y".equals(holidayItem.isTaxableYn())) {
                totalTaxableEarning += holidayItem.amount();
            }
        }

        /** 8-1. 야간근무수당 자동 계산 (가산분 0.5배) */
        AutoPayrollItem nightItem = payrollCalculationService.calculateNightWorkPay(
                ledger, salary.getBaseSalary());

        if(nightItem != null){
            PayrollItem payrollItem = buildPayrollItem(saved, nightItem);
            payrollItems.add(payrollItem);
            totalPayment += nightItem.amount();
            // 야간근무수당은 과세
            if ("Y".equals(nightItem.isTaxableYn())) {
                totalTaxableEarning += nightItem.amount();
            }
        }

        /**
         * 9. 과세 총지급액 기준 4대보험 + 세금 자동 공제
         * totalTaxableEarning: 비과세 제외 -> 실제 과세 대상 금액
         */
        List<AutoPayrollItem> deductionItems = payrollCalculationService.calculateDeductions(
                totalTaxableEarning, reqDto.getPayrollYearMonthDay());

        for(AutoPayrollItem deduction : deductionItems){
            PayrollItem payrollItem = buildPayrollItem(saved ,deduction);
            payrollItems.add(payrollItem);
            totalDeduction += deduction.amount();
        }

        /** 10. saveAll로 배치 저장 */
        payrollItemRepository.saveAll(payrollItems);
        saved.recalculate(totalPayment, totalDeduction);

        return PayrollResDto.fromEntity(saved);
    }

    /**
     * AutoPayrollItem(값 객체) -> PayrollItem(엔티티) 변환
     */
    private PayrollItem buildPayrollItem(Payroll payroll, AutoPayrollItem autoItem) {
        return PayrollItem.builder()
                .payroll(payroll)
                .itemName(autoItem.itemName())
                .itemType(autoItem.itemType())
                .amount(autoItem.amount())
                .displayOrder(autoItem.displayOrder())
                .isTaxableYn(autoItem.isTaxableYn())
                .build();
    }

    /**
     * 급여대장 단건 조회
     */
    @Transactional(readOnly = true)
    public PayrollResDto findPayrollById(UUID companyId, UUID payrollId) {
        Payroll payroll = payrollRepository.findByPayrollIdAndCompanyIdAndDelYn(payrollId, companyId, "N")
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 대장을 찾을 수 없습니다."));
        return PayrollResDto.fromEntity(payroll);
    }

    /**
     * 직원별 급여대장 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PayrollResDto> findPayrollByMemberId(UUID companyId, UUID memberId) {
        return payrollRepository.findByMemberIdAndCompanyIdAndDelYnOrderByPayrollYearMonthDayDesc(memberId, companyId, "N").stream()
                .map(PayrollResDto::fromEntity)
                .toList();
    }

    /**
     * 급여대장 수정 (DRAFT 상태에서만)
     * 확정, 지급 상태에서는 수정 불가
     */
    public PayrollResDto updatePayroll(UUID companyId, UUID payrollId, PayrollUpdateReqDto reqDto){
        Payroll payroll = payrollRepository.findByPayrollIdAndCompanyIdAndDelYn(payrollId, companyId, "N")
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 대장을 찾을 수 없습니다."));

        if(!payroll.isModifiable()){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "확정된 급여대장은 수정할 수 없습니다.");
        }

        payroll.update(reqDto.getPayrollYearMonthDay(),
                reqDto.getTotalPayment(), reqDto.getTotalDeduction(), reqDto.getNetPay());

        return PayrollResDto.fromEntity(payroll);
    }

    /**
     * 급여대장 소프트 삭제 (DRAFT 상태에서만)
     */
    public void deletePayroll(UUID companyId, UUID payrollId){
        Payroll payroll = payrollRepository.findByPayrollIdAndCompanyIdAndDelYn(payrollId, companyId, "N")
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 대장을 찾을 수 없습니다."));

        if(!payroll.isModifiable()){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "확정된 급여대장은 삭제할 수 없습니다.");
        }

        payroll.delete();
    }

    /**
     * 급여 확정 (DRAFT -> CONFIRMED)
     * DRAFT 상태에서만 확정 가능, 확정 후에는 수정, 삭제 불가
     */
    public PayrollResDto confirmPayroll(UUID companyId, UUID payrollId){
        Payroll payroll = payrollRepository.findByPayrollIdAndCompanyIdAndDelYn(payrollId, companyId, "N")
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 대장을 찾을 수 없습니다."));

        if(payroll.getPayrollStatus() != PayrollStatus.DRAFT){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "확정 전인 상태에서만 수정할 수 있습니다.");
        }

        payroll.confirm();
        return PayrollResDto.fromEntity(payroll);
    }

    /**
     * 지급 완료 (CONFIRMED -> PAID)
     */
    public PayrollResDto payPayroll(UUID companyId, UUID payrollId){
        Payroll payroll = payrollRepository.findByPayrollIdAndCompanyIdAndDelYn(payrollId, companyId, "N")
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 대장을 찾을 수 없습니다."));

        if (payroll.getPayrollStatus() != PayrollStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "확정 상태에서만 지급처리 가능합니다.");
        }

        payroll.pay();
        return PayrollResDto.fromEntity(payroll);
    }

    // 급여대장 항목

    /**
     * 급여대장 항목 추가
     * DRAFT 상태에서만 항목 추가 가능
     */
    public PayrollItemResDto createPayrollItem(UUID companyId, UUID payrollId, PayrollItemCreateReqDto reqDto){
        Payroll payroll = payrollRepository.findByPayrollIdAndCompanyIdAndDelYn(payrollId, companyId, "N")
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "급여대장을 찾을 수 없습니다."));

        if(!payroll.isModifiable()){
            throw new BusinessException(HttpStatus.BAD_REQUEST,"확정된 급여대장에는 항목을 추가할 수 없습니다.");
        }

        /** 템플릿 조회 (항목명, 유형 스냅샷용) */
        SalaryItemTemplate template = salaryItemTemplateRepository.findById(reqDto.getSalaryItemTemplateId())
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "정의된 급여 항목을 찾을 수 없습니다."));

        /** 동일 항목명 중복 체크 (같은 급여대장 내 삭제되지 않은 항목 기준) */
        if (payrollItemRepository.existsByPayroll_PayrollIdAndItemNameAndDelYn(
                payrollId, template.getItemName(), "N")) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "이미 동일한 항목('" + template.getItemName() + "')이 존재합니다.");
        }

        /** PayrollItem 생성 (템플릿에서 항목명, 유형 복사) */
        PayrollItem payrollItem = reqDto.toEntity(payroll, template);
        PayrollItem saved = payrollItemRepository.save(payrollItem);

        /** 항목 추가 후 Payroll 합계 자동 재계산 */
        recalculatePayroll(payroll);

        return PayrollItemResDto.fromEntity(saved);
    }

    /**
     * 급여대장별 항목 목록 조회 (삭제되지 않은 것만)
     */
    @Transactional(readOnly = true)
    public List<PayrollItemResDto> findPayrollItems(UUID companyId, UUID payrollId) {
        payrollRepository.findByPayrollIdAndCompanyIdAndDelYn(payrollId, companyId, "N")
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여대장을 찾을 수 없습니다."));

        return payrollItemRepository.findByPayroll_PayrollIdAndDelYnOrderByDisplayOrder(payrollId, "N")
                .stream().map(PayrollItemResDto::fromEntity).toList();
    }

    /**
     * 급여대장 항목 수정
     * DRAFT 상태에서만 수정 가능, 금액,정렬순서 수정
     * 수정 후 Payroll 합계 자동 재계산
     */
    public PayrollItemResDto updatePayrollItem(UUID companyId, UUID payrollItemId, PayrollItemUpdateReqDto reqDto){
        PayrollItem item = payrollItemRepository.findById(payrollItemId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "급여대장 항목을 찾을 수 없습니다."));

        /** 소속 급여대장의 회사 검증 */
        if (!item.getPayroll().getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        if(!item.getPayroll().isModifiable()){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "확정된 급여대장의 항목은 수정 불가합니다.");
        }

        item.updateAmount(reqDto.getAmount());
        item.updateDisplayOrder(reqDto.getDisplayOrder());

        /** 항목 수정 후 Payroll 합계 자동 재계산 */
        recalculatePayroll(item.getPayroll());

        return PayrollItemResDto.fromEntity(item);
    }

    /**
     * 급여대장 항목 소프트 삭제 (DRAFT 상태에서만)
     */
    public void deletePayrollItem(UUID companyId, UUID payrollItemId){
        PayrollItem item = payrollItemRepository.findById(payrollItemId)
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "급여 대장을 찾을 수 없습니다."));

        /** 소속 급여대장의 회사 검증 */
        if (!item.getPayroll().getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        if(!item.getPayroll().isModifiable()){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "확정된 급여대장의 항목은 삭제 불가합니다.");
        }

        item.delete();

        /** 항목 삭제 후 Payroll 합계 자동 재계산 */
        recalculatePayroll(item.getPayroll());
    }

    /**
     * Payroll 합계 재계산
     */
    private void recalculatePayroll(Payroll payroll) {
        Long totalPayment = payrollItemRepository
                .sumAmountByPayrollIdAndItemType(payroll.getPayrollId(), ItemType.EARNING);
        Long totalDeduction = payrollItemRepository
                .sumAmountByPayrollIdAndItemType(payroll.getPayrollId(), ItemType.DEDUCTION);

        payroll.recalculate(totalPayment, totalDeduction);
    }
}