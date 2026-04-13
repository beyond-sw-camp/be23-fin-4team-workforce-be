package com._team._team.batch.leave.worker;

import com._team._team.attendance.Repository.LeavePolicyRepository;
import com._team._team.attendance.Service.AnnualLeaveGrantService;
import com._team._team.attendance.domain.LeavePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 휴가 부여 배치의 오케스트레이션 진입점
 * 처리 순서:
 * 1. 입사기념일 기준 연차
 * 2. 회계연도 기준 연차
 * 3. 삭제되지 않은 모든 {LeavePolicy}에 대해 월차
 */
@Slf4j
@Component
public class LeaveGrantWorker {

    private final AnnualLeaveGrantService annualLeaveGrantService;
    private final MonthlyLeaveGrantWorker monthlyLeaveGrantWorker;
    private final LeavePolicyRepository leavePolicyRepository;

    @Autowired
    public LeaveGrantWorker(AnnualLeaveGrantService annualLeaveGrantService, MonthlyLeaveGrantWorker monthlyLeaveGrantWorker, LeavePolicyRepository leavePolicyRepository) {
        this.annualLeaveGrantService = annualLeaveGrantService;
        this.monthlyLeaveGrantWorker = monthlyLeaveGrantWorker;
        this.leavePolicyRepository = leavePolicyRepository;
    }

    /**
     * baseDate를 기준으로 연차·월차 부여 전체 플로우를 실행하고 로그에 집계 결과를 남김
     */
    public void run(LocalDate baseDate) {
        log.info("LeaveGrantWorker start baseDate={}", baseDate);

        AnnualLeaveGrantService.BatchResult hire = annualLeaveGrantService.grantHireDate(baseDate);
        log.info("Leave grant HIRE_DATE: success={}, skip={}, errors={}", hire.successCount, hire.skipCount, hire.errorCount);

        AnnualLeaveGrantService.BatchResult fiscal = annualLeaveGrantService.grantFiscal(baseDate);
        log.info("Leave grant FISCAL: success={}, skip={}, errors={}", fiscal.successCount, fiscal.skipCount, fiscal.errorCount);

        AnnualLeaveGrantService.BatchResult monthlyTotal = new AnnualLeaveGrantService.BatchResult();
        for (LeavePolicy policy : leavePolicyRepository.findAll().stream()
                .filter(p -> "N".equals(p.getDelYn()))
                .toList()) {
            try {
                monthlyTotal.merge(monthlyLeaveGrantWorker.grantForCompanyMonthly(policy, baseDate));
            } catch (Exception e) {
                log.error("Monthly leave grant failed companyId={}", policy.getCompanyId(), e);
                monthlyTotal.errorCount++;
            }
        }
        log.info("Leave grant MONTHLY: success={}, skip={}, errors={}", monthlyTotal.successCount, monthlyTotal.skipCount, monthlyTotal.errorCount);
    }
}
