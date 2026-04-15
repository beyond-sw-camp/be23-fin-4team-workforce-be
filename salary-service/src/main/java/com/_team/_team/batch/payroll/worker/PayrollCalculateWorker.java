package com._team._team.batch.payroll.worker;

import com._team._team.dto.BusinessException;
import com._team._team.salary.FeignClients.MemberFeignClient;
import com._team._team.salary.FeignClients.dto.MemberResDto;
import com._team._team.salary.Repository.SalaryRepository;
import com._team._team.salary.Service.PayrollCalculationService;
import com._team._team.salary.Service.PayrollService;
import com._team._team.salary.domain.SalaryPolicy;
import com._team._team.salary.dto.reqdto.PayrollCreateReqDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

// 월 급여 일괄 산정 배치 워커
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

    // 급여 계산 배치 실행
    public void run() {
        Instant startedAt = Instant.now();
        LocalDate today = LocalDate.now();
        log.info("[PAYROLL-BATCH] 시작 today={}", today);

        List<UUID> companyIds = salaryRepository.findDistinctCompanyIds();
        if (companyIds.isEmpty()) {
            log.info("[PAYROLL-BATCH] Salary 있는 회사 없음 -> 종료");
            return;
        }

        int totalOk = 0;
        int totalDup = 0;      // 이미 존재 (CONFLICT) - 정상 스킵
        int totalNoSalary = 0; // 활성 Salary 없음 (NOT_FOUND) - 경고
        int totalBadReq = 0;   // 그 외 BusinessException
        int totalFail = 0;     // 예기치 못한 예외

        for (UUID companyId : companyIds) {
            SalaryPolicy policy = payrollCalculationService.findActivePolicy(companyId, today);
            if (policy == null) {
                log.warn("[PAYROLL-BATCH] companyId={} 활성 SalaryPolicy 없음 건너뜀", companyId);
                continue;
            }
            LocalDate settlement = resolveSettlementDate(today, policy.getPayDay());

            List<MemberResDto> members = fetchMembers(companyId);
            int ok = 0, dup = 0, noSalary = 0, badReq = 0, fail = 0;

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
                    HttpStatus status = e.getStatus();
                    if (status == HttpStatus.CONFLICT) {
                        dup++;
                    } else if (status == HttpStatus.NOT_FOUND) {
                        noSalary++;
                        log.warn("[PAYROLL-BATCH] 활성 Salary 없음 companyId={} memberId={} settlement={}",
                                companyId, member.getMemberId(), settlement);
                    } else {
                        badReq++;
                        log.warn("[PAYROLL-BATCH] BusinessException companyId={} memberId={} settlement={} status={} msg={}",
                                companyId, member.getMemberId(), settlement, status, e.getMessage());
                    }
                } catch (Exception e) {
                    fail++;
                    log.error("[PAYROLL-BATCH] 실패 companyId={} memberId={} settlement={} type={} msg={}",
                            companyId, member.getMemberId(), settlement,
                            e.getClass().getSimpleName(), e.getMessage());
                }
            }

            log.info("[PAYROLL-BATCH] 회사 요약 companyId={} settlement={} 대상={} ok={} 중복스킵={} 급여없음={} 기타={} 실패={}",
                    companyId, settlement, members.size(), ok, dup, noSalary, badReq, fail);

            totalOk += ok;
            totalDup += dup;
            totalNoSalary += noSalary;
            totalBadReq += badReq;
            totalFail += fail;
        }

        long elapsedMs = Duration.between(startedAt, Instant.now()).toMillis();
        log.info("[PAYROLL-BATCH] 종료 today={} 회사수={} ok={} 중복스킵={} 급여없음={} 기타={} 실패={} 소요={}ms",
                today, companyIds.size(), totalOk, totalDup, totalNoSalary, totalBadReq, totalFail, elapsedMs);
    }

    // 오늘 이후 가장 가까운 미래 payDay 반환
    // 말일 배치 기준: 이번 달 payDay가 미래면 이번 달, 아니면 다음 달 payDay
    private static LocalDate resolveSettlementDate(LocalDate today, int payDay) {
        int dayThisMonth = Math.min(payDay, today.lengthOfMonth());
        LocalDate payThisMonth = today.withDayOfMonth(dayThisMonth);

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
