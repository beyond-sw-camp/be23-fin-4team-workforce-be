package com._team._team.hr.batch.salary.worker;

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
            // 급여대장 한 장이 어떤 지급일을 기준으로 잡힌 대장인지 나타내는 날짜
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
                    // 중복 정산일·급여 없음 등은 스킵
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

    // 오늘 기준으로 이번에 만들 payrollYearMonthDay. payDay는 정책 지급일, 말일 보정은 PayrollCalculationService랑 동일
    private static LocalDate resolveSettlementDate(LocalDate today, int payDay) {

        // lengthOfMonth : today가 속한 달이 며칠까지 있는지 정수로 받음
        int dayThisMonth = Math.min(payDay, today.lengthOfMonth());
        // 오늘이 속한 달의 지급일 달력상 날짜
        LocalDate payThisMonth = today.withDayOfMonth(dayThisMonth);

        // 오늘이 지급일이거나, 지급일보다 뒤면
        if (!today.isBefore(payThisMonth)) {
            return payThisMonth;
        }

        // 한 달 전 같은 시점의 날짜 (월만 빼고, 일은 가능한 한 유지하되 말일 보정됨) , 예: 3/31에서 한 달 빼면 2/28(또는 29)
        LocalDate prevMonth = today.minusMonths(1);
        int dayPrevMonth = Math.min(payDay, prevMonth.lengthOfMonth());
        return prevMonth.withDayOfMonth(dayPrevMonth);
    }

    private List<MemberResDto> fetchMembers(UUID companyId) {
        var res = memberFeignClient.getMembersByCompany(companyId);
        if (res == null || res.getData() == null) {
            return Collections.emptyList();
        }
        return res.getData();
    }
}
