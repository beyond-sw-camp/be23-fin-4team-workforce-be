package com._team._team.hr.batch.attendance.worker;

import com._team._team.attendance.Repository.DailyAttendanceRepository;
import com._team._team.attendance.domain.DailyAttendance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 일일 근태 마감 배치용 워커
 * 예정 기능: 출·퇴근 미태깅자 식별, 지각·조퇴·결근 분류, DailyAttendance 확정 및 마감 상태 반영 등.
 */
@Slf4j
@Component
public class DailyAttendanceCloseWorker {

    private final DailyAttendanceRepository dailyAttendanceRepository;

    @Autowired
    public DailyAttendanceCloseWorker(DailyAttendanceRepository dailyAttendanceRepository) {
        this.dailyAttendanceRepository = dailyAttendanceRepository;
    }

    /** 일일 마감 로직 실행 */
    @Transactional(readOnly = true)
    public void run() {
        LocalDate targetDate = LocalDate.now().minusDays(1);
        List<DailyAttendance> open = dailyAttendanceRepository.findByAttendanceDateAndFirstClockInIsNotNullAndLastClockOutIsNull(targetDate);
        // 퇴근만 안 찍은 건 집계. 이후 정책 나오면 상태/알림 붙일 예정
        log.info("DailyAttendanceCloseWorker: targetDate={} 미퇴근(출근만 있음) {}건", targetDate, open.size());
    }
}
