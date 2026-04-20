package com._team._team.batch.attendance.worker;

import com._team._team.attendance.service.MonthlyAttendanceCloseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 월 장부 마감 워커
 */
@Slf4j
@Component
public class MonthlyAttendanceCloseWorker {

    private final MonthlyAttendanceCloseService monthlyAttendanceCloseService;

    @Autowired
    public MonthlyAttendanceCloseWorker(MonthlyAttendanceCloseService service) {
        this.monthlyAttendanceCloseService = service;
    }

    // 오늘 기준 전월 대상으로 마감 실행
    public void run() {
        YearMonth targetMonth = YearMonth.from(LocalDate.now()).minusMonths(1);
        MonthlyAttendanceCloseService.CloseResult result =
                monthlyAttendanceCloseService.processForMonth(targetMonth);

        log.info("MonthlyAttendanceCloseWorker month={} companies={} created={} skipped={} failed={}",
                targetMonth,
                result.companiesProcessed(),
                result.ledgersCreated(),
                result.ledgersSkipped(),
                result.failed());
    }
}