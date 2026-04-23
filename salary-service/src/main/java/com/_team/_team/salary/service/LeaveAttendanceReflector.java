package com._team._team.salary.service;

import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.domain.LeaveRequest;
import com._team._team.attendance.domain.enums.AttendanceStatus;
import com._team._team.attendance.repository.DailyAttendanceRepository;
import com._team._team.attendance.repository.LeaveRequestRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 승인된 휴가신청을 일일근태에 반영
 * 근태 마감 배치에서 호출, 휴가일 근태 상태를 LEAVE/HALF 로 확정
 */
@Slf4j
@Component
public class LeaveAttendanceReflector {

    private final LeaveRequestRepository leaveRequestRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;

    @Autowired
    public LeaveAttendanceReflector(LeaveRequestRepository leaveRequestRepository,
                                    DailyAttendanceRepository dailyAttendanceRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.dailyAttendanceRepository = dailyAttendanceRepository;
    }
    /**
     * 특정 날짜의 승인 휴가를 모든 직원에 대해 일괄 반영
     */
    @Transactional
    public int reflectForDate(LocalDate targetDate) {

        // 해당 날짜 범위에 속하는 모든 승인 휴가 조회 (전 직원)
        List<LeaveRequest> activeLeaves = leaveRequestRepository
                .findAllActiveOnDate(targetDate);

        int count = 0;
        for (LeaveRequest leave : activeLeaves) {
            try {
                applyLeaveToAttendance(leave, targetDate);
                count++;
            } catch (Exception e) {
                log.error("[LeaveReflect] 반영 실패 leaveRequestId={}, date={}",
                        leave.getLeaveRequestId(), targetDate, e);
            }
        }
        log.info("[LeaveReflect] 완료 date={}, count={}", targetDate, count);
        return count;
    }

    /**
     * 한 건의 LeaveRequest 를 DailyAttendance 에 반영
     * [로직]
     * 1. usageDays 로 status 결정 (1.0 미만이면 HALF, 이상이면 LEAVE)
     * 2. DailyAttendance(memberId, targetDate) 기존 레코드 조회
     * 3-A. 있으면: 기존 출근 기록 보존, status 만 변경
     *              예, 오전 반차 후 14시 출근 -> status=HALF, workedMinutes=240 유지
     * 3-B. 없으면: LEAVE 상태로 새로 생성 (workedMinutes=0)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void applyLeaveToAttendance(LeaveRequest leave, LocalDate targetDate) {

        // 1. usageDays 기준 status 결정
        // 반차-> 1.0 미만 = HALF, 전일 휴가 = LEAVE
        AttendanceStatus newStatus = leave.getUsageDays() < 1.0
                ? AttendanceStatus.HALF
                : AttendanceStatus.LEAVE;

        // 2. 해당 날짜 기존 DailyAttendance 존재 여부 확인
        Optional<DailyAttendance> existing = dailyAttendanceRepository
                .findByMemberIdAndAttendanceDate(leave.getMemberId(), targetDate);


        if (existing.isPresent()) {
            // 3-A. 이미 출근 기록 있음, status 만 업데이트
            // 반차 + 오후 출근 케이스, 무단 출근 후 사후 휴가 처리 케이스 등 대응
            existing.get().updateStatus(newStatus);
        } else {
            // 3-B. 출근 기록 없음, LEAVE/HALF 로 신규 생성
            DailyAttendance newAttendance = DailyAttendance.builder()
                    .memberId(leave.getMemberId())
                    .companyId(leave.getCompanyId())
                    .attendanceDate(targetDate)
                    .status(newStatus)
                    .workedMinutes(0)
                    .overtimeMinutes(0)
                    .build();
            dailyAttendanceRepository.save(newAttendance);

            log.debug("[LeaveReflect] 신규 휴가 근태 생성, " +
                            "memberId={}, date={}, status={}",
                    leave.getMemberId(), targetDate, newStatus);
        }
    }
}
