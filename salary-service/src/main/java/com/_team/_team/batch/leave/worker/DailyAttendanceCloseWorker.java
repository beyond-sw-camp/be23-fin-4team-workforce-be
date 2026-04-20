package com._team._team.batch.leave.worker;

import com._team._team.attendance.service.DailyAttendanceCloseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 일일 근태 2단계 마감 워커
 * 당일 데이터는 아직 진행중이라 어제까지의 일일 근태 마감함
 */
@Slf4j
@Component
public class DailyAttendanceCloseWorker {

    private final DailyAttendanceCloseService dailyAttendanceCloseService;

    @Autowired
    public DailyAttendanceCloseWorker(DailyAttendanceCloseService dailyAttendanceCloseService) {
        this.dailyAttendanceCloseService = dailyAttendanceCloseService;
    }

    // DRAFT 실행, OPEN -> DRAFT 전이 + 재계산 + 이상감지
    public void runDraft() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        DailyAttendanceCloseService.DraftResult result =
                dailyAttendanceCloseService.processDraft(targetDate);
        log.info("DailyAttendanceCloseWorker[DRAFT] date={} drafted={} failed={} leaveButClockIn={}",
                targetDate, result.drafted(), result.failed(), result.leaveButClockIn());
    }

    // FINAL 실행, DRAFT -> FINALIZED 전이 + 미퇴근 자동 마감
    public void runFinal() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        DailyAttendanceCloseService.FinalResult result =
                dailyAttendanceCloseService.processFinal(targetDate);
        log.info("DailyAttendanceCloseWorker[FINAL] date={} finalized={} autoClockOut={} failed={}",
                targetDate, result.finalized(), result.autoClockOut(), result.failed());
    }
}