package com._team._team.attendance.service;

import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.domain.OvertimePolicy;
import com._team._team.attendance.domain.enums.ViolationType;
import com._team._team.attendance.dto.vo.LaborLawViolation;
import com._team._team.attendance.dto.vo.ResolvedSchedule;
import com._team._team.attendance.repository.DailyAttendanceRepository;
import com._team._team.attendance.repository.OvertimePolicyRepository;
import com._team._team.attendance.repository.OvertimeRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 법정 근로시간 한도 검증
 */
@Service
@Transactional(readOnly = true)
public class LaborLawValidator {

    // 근로기준법 54조 법정 휴게시간 기준
    private static final int FOUR_HOURS = 240;
    private static final int EIGHT_HOURS = 480;
    private static final int MIN_BREAK_OVER_FOUR = 30;
    private static final int MIN_BREAK_OVER_EIGHT = 60;

    private final OvertimePolicyRepository overtimePolicyRepository;
    private final OvertimeRequestRepository overtimeRequestRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;

    @Autowired
    public LaborLawValidator(OvertimePolicyRepository overtimePolicyRepository,
                             OvertimeRequestRepository overtimeRequestRepository,
                             DailyAttendanceRepository dailyAttendanceRepository) {
        this.overtimePolicyRepository = overtimePolicyRepository;
        this.overtimeRequestRepository = overtimeRequestRepository;
        this.dailyAttendanceRepository = dailyAttendanceRepository;
    }

    /**
     * 초과근무 신청 시간을 기존 승인분에 더했을 때 한도 초과가 예상되는지 체크
     */
    public List<LaborLawViolation> validateBeforeOvertimeSubmit(
            UUID memberId,
            UUID companyId,
            LocalDate targetDate,
            int requestedMinutes) {

        List<LaborLawViolation> violations = new ArrayList<>();

        OvertimePolicy policy = overtimePolicyRepository
                .findEffective(companyId, targetDate)
                .orElse(null);
        if (policy == null) {
            return violations;
        }

        // 일 연장근로 한도 체크 (회사 정책)
        checkDailyOvertimeLimit(memberId, targetDate, requestedMinutes, policy, violations);

        // 주 연장근로 한도 체크 (법정 12시간)
        checkWeeklyOvertimeLimit(memberId, targetDate, requestedMinutes, policy, violations);

        // 월 연장근로 한도 체크 (회사 정책)
        checkMonthlyOvertimeLimit(memberId, targetDate, requestedMinutes, policy, violations);

        return violations;
    }

    /**
     * 특정 주의 누적 근로시간이 법정 한도를 넘었는지 사후 검증
     */
    public List<LaborLawViolation> validateWeeklyLimit(UUID memberId,
                                                       UUID companyId,
                                                       LocalDate date) {

        List<LaborLawViolation> violations = new ArrayList<>();

        OvertimePolicy policy = overtimePolicyRepository
                .findEffective(companyId, date)
                .orElse(null);
        if (policy == null) {
            return violations;
        }

        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = date.with(DayOfWeek.SUNDAY);

        // 주 총 근로시간 체크 (52시간)
        int totalWorked = sumWorkedMinutesInRange(memberId, weekStart, weekEnd);
        if (totalWorked > policy.getWeeklyTotalLimitMinutes()) {
            violations.add(new LaborLawViolation(
                    ViolationType.WEEKLY_TOTAL_EXCEEDED,
                    String.format("주 총 근로시간 초과 (%d분 / 한도 %d분)",
                            totalWorked, policy.getWeeklyTotalLimitMinutes()),
                    totalWorked,
                    policy.getWeeklyTotalLimitMinutes()
            ));
        }

        // 주 연장근로 체크 (12시간)
        int weeklyOt = overtimeRequestRepository
                .sumApprovedMinutesInRange(memberId, weekStart, weekEnd);
        if (weeklyOt > policy.getWeeklyOvertimeLimitMinutes()) {
            violations.add(new LaborLawViolation(
                    ViolationType.WEEKLY_OVERTIME_EXCEEDED,
                    String.format("주 연장근로 한도 초과 (%d분 / 한도 %d분)",
                            weeklyOt, policy.getWeeklyOvertimeLimitMinutes()),
                    weeklyOt,
                    policy.getWeeklyOvertimeLimitMinutes()
            ));
        }

        return violations;
    }

    /**
     * 법정 휴게시간 미달 검증
     * 실제 근무시간이 4h/8h 를 넘었는데 휴게시간이 부족하면 위반
     */
    public Optional<LaborLawViolation> validateDailyBreak(DailyAttendance daily,
                                                          ResolvedSchedule schedule) {
        if (daily.getWorkedMinutes() == null) {
            return Optional.empty();
        }

        int worked = daily.getWorkedMinutes();
        int breakMin = schedule.breakMinutes();

        int required;
        if (worked >= EIGHT_HOURS) {
            required = MIN_BREAK_OVER_EIGHT;
        } else if (worked >= FOUR_HOURS) {
            required = MIN_BREAK_OVER_FOUR;
        } else {
            // 4시간 미만 근무는 휴게 의무 없음
            return Optional.empty();
        }

        if (breakMin < required) {
            return Optional.of(new LaborLawViolation(
                    ViolationType.BREAK_TIME_INSUFFICIENT,
                    String.format("법정 휴게시간 부족 (현재 %d분 / 필요 %d분)", breakMin, required),
                    breakMin,
                    required
            ));
        }
        return Optional.empty();
    }

    /**
     * 월 연장근로 한도 검증
     * 관리자 대시보드에서 "이번 달 한도 초과자" 리스트
     */
    public Optional<LaborLawViolation> validateMonthlyOvertimeLimit(UUID memberId,
                                                                    UUID companyId,
                                                                    LocalDate yearMonth) {
        OvertimePolicy policy = overtimePolicyRepository
                .findEffective(companyId, yearMonth)
                .orElse(null);
        if (policy == null || policy.getMonthlyOvertimeLimitMinutes() == null) {
            return Optional.empty();
        }

        LocalDate monthStart = yearMonth.withDayOfMonth(1);
        LocalDate monthEnd = yearMonth.withDayOfMonth(yearMonth.lengthOfMonth());

        int monthlyOt = overtimeRequestRepository
                .sumApprovedMinutesInRange(memberId, monthStart, monthEnd);

        if (monthlyOt > policy.getMonthlyOvertimeLimitMinutes()) {
            return Optional.of(new LaborLawViolation(
                    ViolationType.MONTHLY_OVERTIME_EXCEEDED,
                    String.format("월 연장근로 한도 초과 (%d분 / 한도 %d분)",
                            monthlyOt, policy.getMonthlyOvertimeLimitMinutes()),
                    monthlyOt,
                    policy.getMonthlyOvertimeLimitMinutes()
            ));
        }
        return Optional.empty();
    }

    // 일 연장근로 한도 체크
    private void checkDailyOvertimeLimit(UUID memberId,
                                         LocalDate date,
                                         int requestedMinutes,
                                         OvertimePolicy policy,
                                         List<LaborLawViolation> violations) {
        if (policy.getDailyOvertimeLimitMinutes() == null) {
            return;
        }
        int existing = overtimeRequestRepository.sumApprovedMinutes(memberId, date);
        int projected = existing + requestedMinutes;

        if (projected > policy.getDailyOvertimeLimitMinutes()) {
            violations.add(new LaborLawViolation(
                    ViolationType.DAILY_OVERTIME_EXCEEDED,
                    String.format("일 연장근로 한도 초과 예상 (%d분 / 한도 %d분)",
                            projected, policy.getDailyOvertimeLimitMinutes()),
                    projected,
                    policy.getDailyOvertimeLimitMinutes()
            ));
        }
    }

    // 주 연장근로 한도 체크
    private void checkWeeklyOvertimeLimit(UUID memberId,
                                          LocalDate date,
                                          int requestedMinutes,
                                          OvertimePolicy policy,
                                          List<LaborLawViolation> violations) {
        LocalDate weekStart = date.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = date.with(DayOfWeek.SUNDAY);

        int existing = overtimeRequestRepository
                .sumApprovedMinutesInRange(memberId, weekStart, weekEnd);
        int projected = existing + requestedMinutes;

        if (projected > policy.getWeeklyOvertimeLimitMinutes()) {
            violations.add(new LaborLawViolation(
                    ViolationType.WEEKLY_OVERTIME_EXCEEDED,
                    String.format("주 연장근로 한도 초과 예상 (%d분 / 한도 %d분)",
                            projected, policy.getWeeklyOvertimeLimitMinutes()),
                    projected,
                    policy.getWeeklyOvertimeLimitMinutes()
            ));
        }
    }

    // 월 연장근로 한도 체크
    private void checkMonthlyOvertimeLimit(UUID memberId,
                                           LocalDate date,
                                           int requestedMinutes,
                                           OvertimePolicy policy,
                                           List<LaborLawViolation> violations) {
        if (policy.getMonthlyOvertimeLimitMinutes() == null) {
            return;
        }
        LocalDate monthStart = date.withDayOfMonth(1);
        LocalDate monthEnd = date.withDayOfMonth(date.lengthOfMonth());

        int existing = overtimeRequestRepository
                .sumApprovedMinutesInRange(memberId, monthStart, monthEnd);
        int projected = existing + requestedMinutes;

        if (projected > policy.getMonthlyOvertimeLimitMinutes()) {
            violations.add(new LaborLawViolation(
                    ViolationType.MONTHLY_OVERTIME_EXCEEDED,
                    String.format("월 연장근로 한도 초과 예상 (%d분 / 한도 %d분)",
                            projected, policy.getMonthlyOvertimeLimitMinutes()),
                    projected,
                    policy.getMonthlyOvertimeLimitMinutes()
            ));
        }
    }

    // 주, 월 범위의 workedMinutes 합계
    private int sumWorkedMinutesInRange(UUID memberId, LocalDate from, LocalDate to) {
        return dailyAttendanceRepository
                .findAllByMemberInRange(memberId, from, to)
                .stream()
                .mapToInt(d -> d.getWorkedMinutes() == null ? 0 : d.getWorkedMinutes())
                .sum();
    }
}