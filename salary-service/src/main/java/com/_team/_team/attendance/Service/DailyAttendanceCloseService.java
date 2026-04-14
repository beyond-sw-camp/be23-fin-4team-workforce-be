package com._team._team.attendance.Service;

import com._team._team.attendance.FeignClients.ApprovalServiceClient;
import com._team._team.attendance.Repository.DailyAttendanceRepository;
import com._team._team.attendance.Repository.WorkScheduleRepository;
import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.domain.WorkSchedule;
import com._team._team.attendance.domain.enums.AttendanceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 일일 근태 2단계 마감 서비스
 * 새벽에 한번에 닫으면 밤늦게 퇴근 못찍은 사람이 영문도 모르고 결근 처리되니까 두 번에 나눠서 돌린다.
 * DRAFT(06:00)는 데이터 안 건드리고 알림만, FINAL(14:00)은 그때도 안 고쳐졌으면 자동 마감.
 */
@Slf4j
@Service
public class DailyAttendanceCloseService {

    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final ApprovalServiceClient approvalServiceClient;

    @Autowired
    public DailyAttendanceCloseService(
            DailyAttendanceRepository dailyAttendanceRepository,
            WorkScheduleRepository workScheduleRepository,
            ApprovalServiceClient approvalServiceClient) {
        this.dailyAttendanceRepository = dailyAttendanceRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.approvalServiceClient = approvalServiceClient;
    }

    // DRAFT 단계 - readOnly로 미퇴근/휴가일출근 건수만 집계해서 로그로 남긴다.
    // 본인이 직접 수정하거나 관리자가 처리할 시간을 버는 용도. 수치는 추후 알림 연동에도 활용 가능.
    @Transactional(readOnly = true)
    public DraftResult processDraft(LocalDate targetDate) {
        List<DailyAttendance> unclosed =
                dailyAttendanceRepository.findCloseCandidates(targetDate);

        List<DailyAttendance> leaveButClockIn =
                dailyAttendanceRepository.findLeaveDateWithClockIn(
                        targetDate,
                        List.of(AttendanceStatus.LEAVE, AttendanceStatus.HALF));

        log.info("[DRAFT] targetDate={} 미퇴근={}건, 휴가일출근={}건",
                targetDate, unclosed.size(), leaveButClockIn.size());

        return new DraftResult(unclosed.size(), leaveButClockIn.size());
    }

    // FINAL 단계 - DRAFT 이후에도 안 고쳐진 건을 자동 마감한다.
    // 한 건 터져도 전체 롤백되면 안되니까 건별로 closeOne 호출하고 예외는 먹어서 카운트만.
    public FinalResult processFinal(LocalDate targetDate) {
        List<DailyAttendance> unclosed =
                dailyAttendanceRepository.findCloseCandidates(targetDate);

        int closed = 0;
        int failed = 0;
        for (DailyAttendance da : unclosed) {
            try {
                closeOne(da.getDailyAttendanceId(), targetDate);
                closed++;
            } catch (Exception e) {
                failed++;
                log.error("[FINAL] close 실패 dailyAttendanceId={}, msg={}",
                        da.getDailyAttendanceId(), e.getMessage(), e);
            }
        }
        log.info("[FINAL] targetDate={} 마감 성공={}건, 실패={}건", targetDate, closed, failed);
        return new FinalResult(closed, failed);
    }

    // 레코드 한 건 마감 - REQUIRES_NEW로 건별 독립 트랜잭션.
    // 그 사이에 본인이 직접 퇴근 찍었을 수도 있으니 한 번 더 확인하고 진행.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void closeOne(java.util.UUID dailyAttendanceId, LocalDate targetDate) {
        DailyAttendance da = dailyAttendanceRepository.findById(dailyAttendanceId)
                .orElseThrow(() -> new IllegalStateException(
                        "DailyAttendance not found: " + dailyAttendanceId));

        if (da.getLastClockOut() != null) return;

        LocalDateTime clockOut = resolveClockOut(da, targetDate);

        da.updateClockOut(clockOut);

        WorkSchedule schedule = findSchedule(da.getCompanyId(), da.getMemberId(), targetDate);
        int scheduleWorkMinutes = (schedule != null && schedule.getWorkMinutes() != null)
                ? schedule.getWorkMinutes() : 480; // 기본 8시간
        da.recalculateMinutes(scheduleWorkMinutes);
    }

    // 자동 마감 시 퇴근시각 결정
    // 우선순위: 승인된 연장근무 종료시각 - 근무일정 endTime - 출근+8시간 (최후수단)
    private LocalDateTime resolveClockOut(DailyAttendance da, LocalDate targetDate) {
        try {
            LocalDateTime approvedEnd = approvalServiceClient
                    .findLatestApprovedEndAt(da.getMemberId(), targetDate);
            if (approvedEnd != null) return approvedEnd;
        } catch (Exception e) {
            log.warn("[FINAL] approval-service 조회 실패 memberId={} date={} → schedule fallback",
                    da.getMemberId(), targetDate);
        }

        WorkSchedule schedule = findSchedule(da.getCompanyId(), da.getMemberId(), targetDate);
        if (schedule != null) {
            return targetDate.atTime(schedule.getEndTime());
        }

        return da.getFirstClockIn().plusHours(8);
    }

    // 해당 날짜에 유효한 근무일정 1건 꺼내기 (없으면 null)
    private WorkSchedule findSchedule(java.util.UUID companyId, java.util.UUID memberId, LocalDate date) {
        List<WorkSchedule> schedules = workScheduleRepository
                .findActiveSchedules(companyId, memberId, date);
        return schedules.isEmpty() ? null : schedules.get(0);
    }

    // processDraft/processFinal 결과 집계용 간이 record (로그/모니터링에서 재사용)
    public record DraftResult(int unclosedCount, int leaveButClockInCount) {}
    public record FinalResult(int closedCount, int failedCount) {}
}
