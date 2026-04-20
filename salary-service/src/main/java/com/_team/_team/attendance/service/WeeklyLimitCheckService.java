package com._team._team.attendance.service;

import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.dto.vo.LaborLawViolation;
import com._team._team.attendance.repository.DailyAttendanceRepository;
import com._team._team.dto.NotificationMessage;
import com._team._team.notification.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 주 52시간 초과 감지 서비스
 * 어제 근무 기록 있는 직원 대상으로 주간 한도 검증 후 알림 발송
 */
@Slf4j
@Service
public class WeeklyLimitCheckService {

    private static final String TARGET_TYPE = "WEEKLY_LIMIT_CHECK";

    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final LaborLawValidator laborLawValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public WeeklyLimitCheckService(DailyAttendanceRepository dailyAttendanceRepository,
                                   LaborLawValidator laborLawValidator,
                                   ApplicationEventPublisher eventPublisher) {
        this.dailyAttendanceRepository = dailyAttendanceRepository;
        this.laborLawValidator = laborLawValidator;
        this.eventPublisher = eventPublisher;
    }

    // 특정 일자 기준으로 해당 주의 근로시간 한도 직원 감지 + 알림
    @Transactional(readOnly = true)
    public CheckResult checkForDate(LocalDate targetDate) {
        List<DailyAttendance> attendanceList = dailyAttendanceRepository
                .findAllByAttendanceDate(targetDate);

        int checked = 0;
        int violated = 0;

        for (DailyAttendance da : attendanceList) {
            List<LaborLawViolation> violations = laborLawValidator
                    .validateWeeklyLimit(da.getMemberId(), da.getCompanyId(), targetDate);
            checked++;

            if (!violations.isEmpty()) {
                publishViolationNotice(da, violations);
                violated++;
            }
        }

        log.info("[WeeklyLimitCheck] targetDate={} 검사={} 위반={}",
                targetDate, checked, violated);

        return new CheckResult(checked, violated);
    }

    // 위반 직원에게 알림 발송, 메시지에 첫 위반 유형 노출
    private void publishViolationNotice(DailyAttendance da, List<LaborLawViolation> violations) {
        LaborLawViolation first = violations.get(0);
        String content = String.format(
                "주간 근로시간 한도를 초과했습니다. %s",
                first.message());

        eventPublisher.publishEvent(NotificationMessage.builder()
                .receiverId(da.getMemberId())
                .senderId(null)
                .notificationType(NotificationType.LABOR_LAW_WEEKLY_VIOLATION)
                .content(content)
                .targetId(da.getDailyAttendanceId())
                .targetType(TARGET_TYPE)
                .build());
    }

    // 결과 요약 record
    public record CheckResult(int checked, int violated) {}
}
