package com._team._team.batch.leave.worker;

import com._team._team.attendance.Service.DailyAttendanceCloseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 일일 근태 2단계 마감 워커
 * BatchJobConfig의 Draft/Final Job이 이 워커의 두 메서드를 각각 호출
 * targetDate는 항상 어제(LocalDate.now().minusDays(1)) - 당일 데이터는 아직 진행중이니까 건드리면 안 됨
 */
@Slf4j
@Component
public class DailyAttendanceCloseWorker {

    private final DailyAttendanceCloseService dailyAttendanceCloseService;

    @Autowired
    public DailyAttendanceCloseWorker(DailyAttendanceCloseService dailyAttendanceCloseService) {
        this.dailyAttendanceCloseService = dailyAttendanceCloseService;
    }

    // 06:00 DRAFT 실행 - 알림만 찍는 단계
    // 어제 퇴근 못찍은 사람/휴가일에 출근 찍은 사람 몇 명인지 로그 남김
    public void runDraft() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        DailyAttendanceCloseService.DraftResult result =
                dailyAttendanceCloseService.processDraft(targetDate);
        log.info("DailyAttendanceCloseWorker[DRAFT] date={} unclosed={} leaveButClockIn={}",
                targetDate, result.unclosedCount(), result.leaveButClockInCount());
    }

    // 14:00 FINAL 실행 - 실제 자동 마감
    // DRAFT 이후에도 수동 처리가 안된 건에 대해 퇴근시각을 채워 넣음
    public void runFinal() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        DailyAttendanceCloseService.FinalResult result =
                dailyAttendanceCloseService.processFinal(targetDate);
        log.info("DailyAttendanceCloseWorker[FINAL] date={} closed={} failed={}",
                targetDate, result.closedCount(), result.failedCount());
    }
}
