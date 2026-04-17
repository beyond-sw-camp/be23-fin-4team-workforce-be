package com._team._team.attendance.service;

import com._team._team.attendance.repository.DailyAttendanceRepository;
import com._team._team.attendance.repository.WorkScheduleRepository;
import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.domain.WorkSchedule;
import com._team._team.attendance.domain.enums.AttendanceStatus;
import com._team._team.dto.NotificationMessage;
import com._team._team.notification.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// 근태 이상 감지기
// 지각/조퇴/결근의심/미퇴근/휴가일출근을 뽑아서 본인한테 알림
@Slf4j
@Component
public class AttendanceAnomalyDetector {

    // 허용오차(분) - 이 이내로 들어오면 정상 취급
    private static final int GRACE_MINUTES = 10;

    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public AttendanceAnomalyDetector(DailyAttendanceRepository dailyAttendanceRepository, WorkScheduleRepository workScheduleRepository, ApplicationEventPublisher eventPublisher) {
        this.dailyAttendanceRepository = dailyAttendanceRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.eventPublisher = eventPublisher;
    }

    // 대상일 근태를 훑어서 이상 건마다 NotificationMessage 이벤트 발행
    @Transactional(readOnly = true)
    public AnomalyResult detect(LocalDate targetDate) {
        List<DailyAttendance> all = dailyAttendanceRepository.findAllByAttendanceDate(targetDate);

        int late = 0, early = 0, missing = 0, absent = 0, leaveIn = 0;

        for (DailyAttendance da : all) {
            AttendanceStatus status = da.getStatus();

            // 휴가일 출근 - LEAVE/HALF인데 clockIn 찍힌 경우
            if ((status == AttendanceStatus.LEAVE || status == AttendanceStatus.HALF)
                    && da.getFirstClockIn() != null) {
                publish(da, NotificationType.ATTENDANCE_LEAVE_CLOCKIN,
                        "휴가일에 출근 기록이 있습니다. (" + targetDate + ")");
                leaveIn++;
                continue;
            }

            // 여기서부터는 NORMAL만 대상 - ABSENT/LEAVE/HALF는 이미 상태 확정된 것
            if (status != AttendanceStatus.NORMAL) continue;

            WorkSchedule schedule = findSchedule(da.getCompanyId(), da.getMemberId(), targetDate);

            // 결근 의심 - 평일인데 스케줄은 있고 clockIn 없음
            if (da.getFirstClockIn() == null) {
                if (isWorkday(targetDate) && schedule != null) {
                    publish(da, NotificationType.ATTENDANCE_ABSENT_SUSPECT,
                            "출근 기록이 없습니다. 출근 상태를 확인해주세요. (" + targetDate + ")");
                    absent++;
                }
                continue;
            }

            // 미퇴근 - clockIn은 있는데 clockOut 없음
            if (da.getLastClockOut() == null) {
                publish(da, NotificationType.ATTENDANCE_MISSING_CLOCK_OUT,
                        "퇴근 기록이 없습니다. 퇴근 처리해주세요. (" + targetDate + ")");
                missing++;
                continue;
            }

            // 지각/조퇴는 스케줄 있어야 판정 가능
            if (schedule == null) continue;

            LocalDateTime scheduledStart = targetDate.atTime(schedule.getStartTime());
            LocalDateTime scheduledEnd = targetDate.atTime(schedule.getEndTime());

            long lateMin = Duration.between(scheduledStart, da.getFirstClockIn()).toMinutes();
            if (lateMin > GRACE_MINUTES) {
                publish(da, NotificationType.ATTENDANCE_LATE,
                        "지각이 감지되었습니다. (" + lateMin + "분 지각)");
                late++;
            }

            long earlyMin = Duration.between(da.getLastClockOut(), scheduledEnd).toMinutes();
            if (earlyMin > GRACE_MINUTES) {
                publish(da, NotificationType.ATTENDANCE_EARLY_LEAVE,
                        "조퇴가 감지되었습니다. (" + earlyMin + "분 조퇴)");
                early++;
            }
        }

        log.info("[ANOMALY] targetDate={} 지각={} 조퇴={} 미퇴근={} 결근의심={} 휴가출근={}",
                targetDate, late, early, missing, absent, leaveIn);
        return new AnomalyResult(late, early, missing, absent, leaveIn);
    }

    // 해당일 유효 스케줄 1건 (개인 우선, 없으면 회사 기본) 없으면 null
    private WorkSchedule findSchedule(UUID companyId, UUID memberId, LocalDate date) {
        List<WorkSchedule> list = workScheduleRepository.findActiveSchedules(companyId, memberId, date);
        return list.isEmpty() ? null : list.get(0);
    }

    // 주말이면 결근 판정에서 제외 (공휴일은 일단 미고려 - 추후 달력 연동 시 보완)
    private boolean isWorkday(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY;
    }

    // 본인한테 알림 senderId는 시스템 발송이라 null
    private void publish(DailyAttendance da, NotificationType type, String content) {
        eventPublisher.publishEvent(NotificationMessage.builder()
                .receiverId(da.getMemberId())
                .senderId(null)
                .notificationType(type)
                .content(content)
                .targetId(da.getDailyAttendanceId())
                .targetType("DAILY_ATTENDANCE")
                .build());
    }

    public record AnomalyResult(int lateCount, int earlyLeaveCount,
                                int missingClockOutCount, int absentSuspectCount,
                                int leaveClockInCount) {}
}
