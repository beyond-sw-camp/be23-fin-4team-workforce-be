package com._team._team.salary.Service;

import com._team._team.dto.BusinessException;
import com._team._team.salary.Repository.PayrollItemRepository;
import com._team._team.salary.Repository.PayrollRepository;
import com._team._team.salary.Repository.SalaryItemTemplateRepository;
import com._team._team.salary.Repository.SalaryRepository;
import com._team._team.salary.domain.Payroll;
import com._team._team.salary.domain.PayrollItem;
import com._team._team.salary.domain.Salary;
import com._team._team.salary.domain.SalaryItemTemplate;
import com._team._team.salary.domain.enums.ItemType;
import com._team._team.salary.domain.enums.PayrollStatus;
import com._team._team.salary.dto.reqdto.PayrollCreateReqDto;
import com._team._team.salary.dto.reqdto.PayrollItemCreateReqDto;
import com._team._team.salary.dto.reqdto.PayrollItemUpdateReqDto;
import com._team._team.salary.dto.reqdto.PayrollUpdateReqDto;
import com._team._team.salary.dto.resdto.PayrollItemResDto;
import com._team._team.salary.dto.resdto.PayrollResDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PayrollService {

    private final PayrollRepository payrollRepository;
    private final PayrollItemRepository payrollItemRepository;
    private final SalaryRepository salaryRepository;
    private final SalaryItemTemplateRepository salaryItemTemplateRepository;

    @Autowired
    public PayrollService(PayrollRepository payrollRepository,
                          PayrollItemRepository payrollItemRepository,
                          SalaryRepository salaryRepository,
                          SalaryItemTemplateRepository salaryItemTemplateRepository){
        this.payrollRepository = payrollRepository;
        this.payrollItemRepository = payrollItemRepository;
        this.salaryRepository = salaryRepository;
        this.salaryItemTemplateRepository = salaryItemTemplateRepository;
    }

    // ===================== 급여대장 =====================

    /**
     * 급여대장 생성
     * [처리 흐름]
     * 1) 직원의 정산일 기준 적용 중인 Salary 자동 조회
     * 2) 동일 직원 + 동일 정산일 중복 체크
     * 3) Payroll 엔티티 생성 (금액 0으로 초기화)
     * 4) 회사의 SalaryItemTemplate 목록 → PayrollItem 일괄 생성 (saveAll 배치)
     *    - "기본급" 항목은 Salary.baseSalary로 금액 자동 세팅
     *    - 그 외 항목은 0원으로 초기 생성 (이후 DRAFT 상태에서 수정)
     * 5) EARNING/DEDUCTION 합계 계산 → Payroll.recalculate()
     */
    public PayrollResDto createPayroll(UUID companyId, PayrollCreateReqDto reqDto){

        /** 1. 직원의 정산일 기준 적용 중인 급여 조회 */
        Salary salary = salaryRepository.findActiveSalary(
                reqDto.getMemberId(), companyId, reqDto.getPayrollYearMonthDay())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "해당 직원의 적용 중인 급여 정보를 찾을 수 없습니다."));

        /** 2. 직원 + 정산일 중복 체크 (삭제되지 않은 것만) */
        if(payrollRepository.existsByMemberIdAndPayrollYearMonthDayAndDelYn(
                reqDto.getMemberId(), reqDto.getPayrollYearMonthDay(), "N")){
            throw new BusinessException(HttpStatus.CONFLICT,
                    "해당 직원의 동일 정산일에 이미 급여대장이 존재합니다.");
        }

        /** 3. Payroll 생성 (금액은 0으로 초기화, 항목 생성 후 재계산) */
        Payroll payroll = reqDto.toEntity(companyId, salary.getSalaryId());
        Payroll saved = payrollRepository.save(payroll);

        /** 4. 회사의 급여 항목 템플릿 조회 → PayrollItem 일괄 생성 */
        List<SalaryItemTemplate> templates = salaryItemTemplateRepository
                .findByCompanyIdAndDelYn(companyId, "N");

        long totalPayment = 0L;
        long totalDeduction = 0L;
        List<PayrollItem> payrollItems = new ArrayList<>();

        for (SalaryItemTemplate template : templates) {
            /**
             * "기본급" 항목이면 Salary의 baseSalary를 금액으로 자동 세팅
             * 그 외 항목(식대, 교통비, 소득세 등)은 0원으로 생성 → DRAFT 상태에서 수정
             * 추후 각 직원들의 항목 금액 계산식 구축 예정
             */
            long amount = template.getItemName().equals("기본급")
                    ? salary.getBaseSalary() : 0L;

            /** payrollItem 생성 */
            PayrollItem payItem = PayrollItem.fromTemplate(saved, template, amount);
            payrollItems.add(payItem);

            /** EARNING/DEDUCTION 분류하여 합계 누적 */
            if (template.getItemType() == ItemType.EARNING) {
                totalPayment += amount;
            } else {
                totalDeduction += amount;
            }
        }

        /** saveAll로 배치 저장 (개별 save 대비 INSERT 횟수 최소화) */
        payrollItemRepository.saveAll(payrollItems);

        /** 5. 합계 자동 계산 (이미 Java에서 계산했으므로 DB 재조회 없음) */
        saved.recalculate(totalPayment, totalDeduction);

        return PayrollResDto.fromEntity(saved);
    }

    /**
     * 급여대장 단건 조회
     * - companyId + delYn 조건으로 타사 접근 차단
     */
    @Transactional(readOnly = true)
    public PayrollResDto findPayrollById(UUID companyId, UUID payrollId) {
        Payroll payroll = payrollRepository.findByPayrollIdAndCompanyIdAndDelYn(payrollId, companyId, "N")
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 대장을 찾을 수 없습니다."));
        return PayrollResDto.fromEntity(payroll);
    }

    /**
     * 직원별 급여대장 목록 조회
     * - companyId 조건으로 자사 직원의 삭제되지 않은 급여대장만, 최신 정산일순으로 조회
     */
    @Transactional(readOnly = true)
    public List<PayrollResDto> findPayrollByMemberId(UUID companyId, UUID memberId) {
        return payrollRepository.findByMemberIdAndCompanyIdAndDelYnOrderByPayrollYearMonthDayDesc(memberId, companyId, "N").stream()
                .map(PayrollResDto::fromEntity)
                .toList();
    }

    /**
     * 급여대장 수정 (DRAFT 상태에서만)
     * - 확정(CONFIRMED) 또는 지급(PAID) 상태에서는 수정 불가
     * - 정산일, 지급합계, 공제합계, 실수령액 수정
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
     * - delYn = "Y"로 변경하여 논리 삭제 처리
     * - 확정 이후에는 삭제 불가
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
     * 급여 확정 (DRAFT → CONFIRMED)
     * - DRAFT 상태에서만 확정 가능
     * - 확정 후에는 수정/삭제 불가
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
     * 지급 완료 (CONFIRMED → PAID)
     * - CONFIRMED 상태에서만 지급처리 가능
     * - 지급 완료 후 paidAt 날짜 기록
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

    // ===================== 급여대장 항목 =====================

    /**
     * 급여대장 항목 추가
     * - DRAFT 상태에서만 항목 추가 가능
     * - SalaryItemTemplate에서 항목명, 유형, 정렬순서 스냅샷 복사
     * - 금액만 DTO에서 입력받음
     * - 항목 추가 후 Payroll 합계 자동 재계산 (DB SUM 쿼리)
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
     * - delYn = "N" 조건으로 소프트 삭제된 항목 제외
     * - displayOrder 기준 오름차순 정렬
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
     * - DRAFT 상태에서만 수정 가능
     * - 금액과 정렬순서 수정
     * - 수정 후 Payroll 합계 자동 재계산 (DB SUM 쿼리)
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
     * - delYn = "Y"로 변경하여 논리 삭제 처리
     * - 삭제 후 Payroll 합계 자동 재계산 (DB SUM 쿼리)
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

    // ===================== 내부 헬퍼 메서드 =====================

    /**
     * Payroll 합계 재계산 (DB SUM 쿼리 방식)
     *
     * - 전체 항목을 Java로 가져와서 stream 합산하는 대신
     *   DB에서 SUM 쿼리 2번으로 합계 계산 (네트워크 전송량 최소화)
     * - 삭제되지 않은(delYn="N") PayrollItem만 대상
     */
    private void recalculatePayroll(Payroll payroll) {
        Long totalPayment = payrollItemRepository
                .sumAmountByPayrollIdAndItemType(payroll.getPayrollId(), ItemType.EARNING);
        Long totalDeduction = payrollItemRepository
                .sumAmountByPayrollIdAndItemType(payroll.getPayrollId(), ItemType.DEDUCTION);

        payroll.recalculate(totalPayment, totalDeduction);
    }
}
