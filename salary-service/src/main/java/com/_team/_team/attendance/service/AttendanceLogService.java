package com._team._team.attendance.service;


import com._team._team.attendance.domain.enums.LeaveApprovalStatus;
import com._team._team.attendance.domain.enums.LeaveInitiator;
import com._team._team.attendance.repository.AttendanceLogRepository;
import com._team._team.attendance.domain.AttendanceLog;
import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.domain.enums.AttendanceStatus;
import com._team._team.attendance.domain.enums.EventType;
import com._team._team.attendance.dto.reqDto.AttendanceLogCreateReqDto;
import com._team._team.attendance.dto.resDto.AttendanceLogResDto;
import com._team._team.attendance.dto.resDto.DailyAttendanceResDto;
import com._team._team.attendance.repository.LeaveRequestRepository;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 출퇴근 이벤트 처리 서비스
 *
 * 사원의 출퇴근 이벤트(CLOCK_IN, CLOCK_OUT)를
 * AttendanceLog에 기록하고, 동시에 DailyAttendance(집계 테이블)를 갱신
 *
 * 출근하면 로그 INSERT + DailyAttendance.firstClockIn 갱신
 * 퇴근하면 로그 INSERT + DailyAttendance.lastClockOut + workedMinutes 계산
 */
@Service
@Transactional
public class AttendanceLogService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final DailyAttendanceService dailyAttendanceService;
    private final LeaveRequestRepository leaveRequestRepository;

    @Autowired
    public AttendanceLogService(AttendanceLogRepository attendanceLogRepository, DailyAttendanceService dailyAttendanceService, LeaveRequestRepository leaveRequestRepository) {
        this.attendanceLogRepository = attendanceLogRepository;
        this.dailyAttendanceService = dailyAttendanceService;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    /**
     * 출근 처리 (CLOCK_IN)
     *
     * - 중복 출근 체크
     * - true면 "이미 출근했습니다" 409 에러
     * 사원이 실수로 출근 버튼을 두 번 누를 수 있음
     *
     * - 출근 로그 저장
     * - reqDto.toEntity()로 AttendanceLog 생성
     * - EventType.CLOCK_IN, 기기 ID 포함
     *
     * - DailyAttendance 갱신
     * - firstClockIn = 출근 시간 기록
     * - status = NORMAL (ABSENT → NORMAL로 변경)
     */
    public DailyAttendanceResDto clockIn(UUID companyId, UUID memberId, AttendanceLogCreateReqDto reqDto){
        // 출근 시간 결정 (서버 시간)
        LocalDateTime eventTime = resolveEventTime(reqDto.getEventTime());
        LocalDate today = eventTime.toLocalDate();

        // 노무 수령 거부 가드 회사가 강제 지정한 연차일이면 출근 차단
        boolean designatedToday = leaveRequestRepository.existsForcedDesignationOnDate(
                memberId, companyId, today,
                LeaveInitiator.ADMIN_DESIGNATION,
                LeaveApprovalStatus.APPROVED);
        if (designatedToday) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "오늘은 회사가 지정한 연차일입니다 노무 수령을 거부합니다");
        }

        // 당일 DailyAttendance 조회 or 생성
        DailyAttendance dailyAttendance = dailyAttendanceService.findOrCreateDaily(companyId, memberId, today);


        // 중복 출근 체크
        if(attendanceLogRepository.existsByDailyAttendanceAndEventType(dailyAttendance, EventType.CLOCK_IN)){
            throw new BusinessException(HttpStatus.CONFLICT, "이미 출근 처리되었습니다.");
        }

        // 출근 로그 저장
        AttendanceLog log = reqDto.toEntity(dailyAttendance, EventType.CLOCK_IN, eventTime);
        attendanceLogRepository.save(log);

        // DailyAttendance 갱신
        // firstClockIn: 출근 시간 기록
        // status: ABSENT → NORMAL로 변경
        dailyAttendance.updateClockIn(eventTime);
        dailyAttendance.updateStatus(AttendanceStatus.NORMAL);

        return DailyAttendanceResDto.fromEntity(dailyAttendance);
    }

    /**
     * 퇴근 처리 (CLOCK_OUT)
     *
     * 퇴근은 "근무시간 + 초과근무 판정"까지 계산 (급여 계산의 기반)
     *
     * - findDaily : 출근 기록 확인
     *
     * - CLOCK_IN 존재 확인
     * - DailyAttendance는 있는데 CLOCK_IN 로그가 없는 경우 방지
     *
     * - 중복 퇴근 체크
     * - 이미 CLOCK_OUT이 있으면 409 에러
     */
    public DailyAttendanceResDto clockOut(UUID companyId, UUID memberId, AttendanceLogCreateReqDto reqDto){
        // 퇴근 시간 결정
        LocalDateTime eventTime = resolveEventTime(reqDto.getEventTime());
        LocalDate today = eventTime.toLocalDate();

        // 당일 DailyAttendance 조회 (없으면 400 - 출근 기록 없음)
        DailyAttendance dailyAttendance = dailyAttendanceService.findDaily(companyId, memberId, today);

        // 출근 로그 존재 확인
        if(!attendanceLogRepository.existsByDailyAttendanceAndEventType(dailyAttendance, EventType.CLOCK_IN)){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "출근 기록이 없습니다. 출근 처리를 먼저 해주세요.");
        }

        // 중복 퇴근 체크
        if(attendanceLogRepository.existsByDailyAttendanceAndEventType(dailyAttendance, EventType.CLOCK_OUT)){
            throw new BusinessException(HttpStatus.CONFLICT, "이미 퇴근 처리되었습니다.");
        }

        // 퇴근 로그 저장
        AttendanceLog log = reqDto.toEntity(dailyAttendance, EventType.CLOCK_OUT, eventTime);
        attendanceLogRepository.save(log);

        // DailyAttendance 갱신 + 근무시간/초과근무 계산
        // 1. 퇴근 시간 기록
        dailyAttendance.updateClockOut(eventTime);

        // 2. 이 사원의 기준 근무시간 조회 (개인 스케줄 > 회사기본 > 기본 480분)
        int scheduleWorkMinutes = dailyAttendanceService.getScheduleWorkMinutes(companyId, memberId, today);

        // 3. 근무시간 재계산
        // workedMinutes = (퇴근 - 출근) - 점심시간
        // overtimeMinutes = workedMinutes가 기준 초과 시 초과분
        dailyAttendance.recalculateMinutes(scheduleWorkMinutes);

        return DailyAttendanceResDto.fromEntity(dailyAttendance);
    }

    /**
     * 특정 날짜의 출퇴근 로그 목록 조회 (시간순)
     *
     * findDailyAttendance()는 "요약"만 보여줌 (출근시간, 퇴근시간, 총 근무시간).
     * 사원이 상세 이력을 보고 싶을 때
     * 이 메서드로 개별 로그를 시간순으로 조회
     */
    @Transactional(readOnly = true)
    public List<AttendanceLogResDto> findLogs(UUID companyId, UUID memberId, LocalDate date){

        DailyAttendance dailyAttendance = dailyAttendanceService.findDaily(companyId, memberId, date);

        // daily_attendance_id로 해당일 로그 전체 조회 (시간순)
        List<AttendanceLog> logs = attendanceLogRepository
                .findByDailyAttendanceDailyAttendanceIdOrderByEventTime(dailyAttendance.getDailyAttendanceId());

        return logs.stream()
                .map(AttendanceLogResDto::fromEntity)
                .toList();
    }

    /**
     * 이벤트 시간 결정
     */
    private LocalDateTime resolveEventTime(LocalDateTime eventTime) {
        return eventTime != null ? eventTime : LocalDateTime.now();
    }
}
