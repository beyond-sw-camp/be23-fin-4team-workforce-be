package com._team._team.hr.batch.salary.worker;

import com._team._team.salary.Repository.PayrollRepository;
import com._team._team.salary.domain.Payroll;
import com._team._team.salary.domain.enums.PayrollStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 급여명세서 발송 배치용 워커
 * 확정된 Payroll 대상 조회, 템플릿 렌더링, 발송 채널 연동, 실패 재시도 정책.
 */
@Slf4j
@Component
public class PayslipSendWorker {

    private final PayrollRepository payrollRepository;

    @Autowired
    public PayslipSendWorker(PayrollRepository payrollRepository) {
        this.payrollRepository = payrollRepository;
    }

    /** 명세서 발송 배치 실행 */
    @Transactional(readOnly = true)
    public void run() {
        // CONFIRMED = 확정만. 실제 PDF 붙일 때 여기서 루프 돌리기
        List<Payroll> targets = payrollRepository.findByPayrollStatusAndDelYn(PayrollStatus.CONFIRMED, "N");
        log.info("PayslipSendWorker: 명세서 발송 대상(CONFIRMED) {}건 — 발송 연동 전이라 로그만", targets.size());
    }
}
