package com._team._team.batch.payroll.worker;

import com._team._team.dto.BusinessException;
import com._team._team.salary.FeignClients.MemberFeignClient;
import com._team._team.salary.FeignClients.dto.MemberResDto;
import com._team._team.salary.Repository.SalaryRepository;
import com._team._team.salary.Service.PayrollCalculationService;
import com._team._team.salary.Service.PayrollService;
import com._team._team.salary.dto.reqdto.PayrollCreateReqDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 월 급여 일괄 산정 배치용 워커
 * 정산월·회사·직원 단위 Payroll 생성 또는 갱신, 근태·수당 연동, 멱등 키(정산월 등) 설계.
 */
@Slf4j
@Component
public class PayrollCalculateWorker {

    private final PayrollService payrollService;
    private final PayrollCalculationService payrollCalculationService;
    private final MemberFeignClient memberFeignClient;
    private final SalaryRepository salaryRepository;

    @Autowired
    public PayrollCalculateWorker(
            PayrollService payrollService,
            PayrollCalculationService payrollCalculationService,
            MemberFeignClient memberFeignClient,
            SalaryRepository salaryRepository) {
        this.payrollService = payrollService;
        this.payrollCalculationService = payrollCalculationService;
        this.memberFeignClient = memberFeignClient;
        this.salaryRepository = salaryRepository;
    }

    /** 급여 계산 배치 실행 */
    public void run() {
        LocalDate today = LocalDate.now();
        List<UUID> companyIds = salaryRepository.findDistinctCompanyIds();
        if (companyIds.isEmpty()) {
            log.info("PayrollCalculateWorker: Salary 있는 회사 없음");
            return;
        }

        int ok = 0;
        int skip = 0;
        int fail = 0;

        for (UUID companyId : companyIds) {
            var policy = payrollCalculationService.findActivePolicy(companyId, today);
            if (policy == null) {
                log.warn("PayrollCalculateWorker: companyId={} 활성 SalaryPolicy 없음 — 건너뜀", companyId);
                continue;
            }
            LocalDate settlement = resolveSettlementDate(today, policy.getPayDay());

            List<MemberResDto> members = fetchMembers(companyId);
            for (MemberResDto member : members) {
                try {
                    payrollService.createPayroll(
                            companyId,
                            PayrollCreateReqDto.builder()
                                    .memberId(member.getMemberId())
                                    .payrollYearMonthDay(settlement)
                                    .build());
                    ok++;
                } catch (BusinessException e) {
                    skip++;
                } catch (Exception e) {
                    fail++;
                    log.warn(
                            "PayrollCalculateWorker: companyId={} memberId={} — {}",
                            companyId,
                            member.getMemberId(),
                            e.getMessage());
                }
            }
        }

        log.info(
                "PayrollCalculateWorker: 기준일={} 회사수={} 생성ok={} 스킵={} 실패={} (정산일=회사별 payDay)",
                today,
                companyIds.size(),
                ok,
                skip,
                fail);
    }

    /**
     * 오늘 이후 가장 가까운 미래 payDay 반환.
     * 말일 배치 기준: 이번 달 payDay가 미래면 이번 달, 아니면 다음 달 payDay.
     */
    private static LocalDate resolveSettlementDate(LocalDate today, int payDay) {
        int dayThisMonth = Math.min(payDay, today.lengthOfMonth());
        LocalDate payThisMonth = today.withDayOfMonth(dayThisMonth);

        // 이번 달 payDay가 아직 안 왔으면 이번 달, 이미 지났으면 다음 달
        if (today.isBefore(payThisMonth)) {
            return payThisMonth;
        }

        LocalDate nextMonth = today.plusMonths(1);
        int dayNextMonth = Math.min(payDay, nextMonth.lengthOfMonth());
        return nextMonth.withDayOfMonth(dayNextMonth);
    }

    private List<MemberResDto> fetchMembers(UUID companyId) {
        var res = memberFeignClient.getMembersByCompany(companyId);
        if (res == null || res.getData() == null) {
            return Collections.emptyList();
        }
        return res.getData();
    }
}
