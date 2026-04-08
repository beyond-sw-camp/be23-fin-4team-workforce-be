package com._team._team.attendance.Service;


import com._team._team.attendance.Repository.AttendanceLogRepository;
import com._team._team.attendance.domain.AttendanceLog;
import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.domain.enums.AttendanceStatus;
import com._team._team.attendance.domain.enums.EventType;
import com._team._team.attendance.dto.reqDto.AttendanceLogCreateReqDto;
import com._team._team.attendance.dto.resDto.AttendanceLogResDto;
import com._team._team.attendance.dto.resDto.DailyAttendanceResDto;
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
 * 출퇴근/휴게 이벤트 처리 서비스
 *
 * ===== 이 서비스의 역할 =====
 * 사원의 출퇴근·휴게 이벤트(CLOCK_IN, CLOCK_OUT, BREAK_START, BREAK_END)를
 * AttendanceLog에 기록하고, 동시에 DailyAttendance(집계 테이블)를 갱신.
 *
 * 출근하면 → 로그 INSERT + DailyAttendance.firstClockIn 갱신
 * 퇴근하면 → 로그 INSERT + DailyAttendance.lastClockOut + workedMinutes 계산
 *
 * ===== 이벤트별 DailyAttendance 갱신 정리 =====
 * CLOCK_IN    → firstClockIn 세팅, status → NORMAL
 * CLOCK_OUT   → lastClockOut 세팅, workedMinutes + overtimeMinutes 계산
 * BREAK_START → 갱신 없음 (로그만 저장)
 * BREAK_END   → totalBreakMinutes에 휴게시간 누적
 */
@Service
@Transactional
public class AttendanceLogService {

    private final AttendanceLogRepository attendanceLogRepository;
    private final DailyAttendanceService dailyAttendanceService;

    @Autowired
    public AttendanceLogService(AttendanceLogRepository attendanceLogRepository, DailyAttendanceService dailyAttendanceService) {
        this.attendanceLogRepository = attendanceLogRepository;
        this.dailyAttendanceService = dailyAttendanceService;
    }

    /**
     * 출근 처리 (CLOCK_IN)
     *
     * ===== 사용자 시나리오 =====
     * 08:50 사원이 모바일 앱에서 "출근" 버튼 터치
     * → 프론트가 POST /attendance/clock-in 호출
     * → 이 메서드 실행
     *
     * ===== 워크플로우 (단계별) =====
     *
     * [단계 1] resolveEventTime
     * - 프론트에서 eventTime을 보냈으면 그 시간 사용
     * - 안 보냈으면 (null) 서버 현재 시간 사용
     * → 모바일에서 네트워크 지연으로 시간이 다를 수 있음
     *
     * [단계 2] findOrCreateDaily
     * - 오늘 DailyAttendance가 있으면 가져옴
     * - 없으면 새로 만듦 (아침 첫 출근이면 없으니까)
     * - 이때 근무 스케줄도 자동 매핑됨
     *
     * [단계 3] 중복 출근 체크
     * - existsBy(CLOCK_IN) → EXISTS 서브쿼리로 빠르게 확인
     * - true면 "이미 출근했습니다" 409 에러
     * → 사원이 실수로 출근 버튼을 두 번 누를 수 있음
     *
     * [단계 4] 출근 로그 저장
     * - reqDto.toEntity()로 AttendanceLog 생성
     * - EventType.CLOCK_IN, GPS 좌표, 기기 ID 포함
     *
     * [단계 5] DailyAttendance 갱신
     * - firstClockIn = 출근 시간 기록
     * - status = NORMAL (ABSENT → NORMAL로 변경)
     */
    public DailyAttendanceResDto clockIn(UUID companyId, UUID memberId, AttendanceLogCreateReqDto reqDto){
        // 출근 시간 결정 (서버 시간)
        LocalDateTime eventTime = resolveEventTime(reqDto.getEventTime());
        LocalDate today = eventTime.toLocalDate();

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
     * ===== 사용자 시나리오 =====
     * 18:00 사원이 "퇴근" 버튼 터치
     * → POST /attendance/clock-out
     * → 이 메서드 실행
     *
     * 퇴근은 "근무시간 + 초과근무 판정"까지 계산 (급여 계산의 기반)
     *
     * ===== 워크플로우 (단계별) =====
     *
     * [단계 1] resolveEventTime — 시간 결정
     *
     * [단계 2] findDaily — 출근 기록 확인
     * - 없으면 "출근을 먼저 해주세요" 400 에러
     *
     * [단계 3] CLOCK_IN 존재 확인
     * - DailyAttendance는 있는데 CLOCK_IN 로그가 없는 경우 방지
     *  (관리자가 수동으로 DailyAttendance만 만든 경우 등)
     *
     * [단계 4] 중복 퇴근 체크
     * - 이미 CLOCK_OUT이 있으면 409 에러
     *
     * [단계 5] 퇴근 로그 저장
     *
     * [단계 6] DailyAttendance 갱신 + 근무시간 계산
     * - lastClockOut = 퇴근 시간 기록
     * - getScheduleWorkMinutes() → 이 사원의 기준 근무시간(분) 조회
     * - recalculateMinutes(기준시간):
     *   workedMinutes = (퇴근 - 출근) - breakMinutes - totalBreakMinutes
     *   overtimeMinutes = workedMinutes - 기준시간 (초과분만)
     *
     * ===== 계산 예시 =====
     * 출근 09:00, 퇴근 18:00, 점심 휴게 60분, 기준 480분(8시간)
     * workedMinutes = 540 - 0 - 60 = 480분
     * overtimeMinutes = 480 - 480 = 0분 (초과 없음)
     *
     * 출근 09:00, 퇴근 20:00, 점심 휴게 60분, 기준 480분
     * workedMinutes = 660 - 0 - 60 = 600분
     * overtimeMinutes = 600 - 480 = 120분 (2시간 초과근무)
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
        int scheduleWorkMinutes = dailyAttendanceService
                .getScheduleWorkMinutes(companyId, memberId, today);

        // 3. 근무시간 재계산
        // workedMinutes = (퇴근 - 출근) - breakMinutes - totalBreakMinutes
        // overtimeMinutes = workedMinutes가 기준 초과 시 초과분
        dailyAttendance.recalculateMinutes(scheduleWorkMinutes);

        return DailyAttendanceResDto.fromEntity(dailyAttendance);
    }

    /**
     * 휴게 시작 (BREAK_START)
     *
     * ===== 사용자 시나리오 =====
     * 12:00 사원이 "휴게 시작" 버튼 터치 (점심시간)
     * → POST /attendance/break-start
     * → 이 메서드 실행
     *
     * ===== 출근/퇴근과 다른 점 =====
     * - 출근/퇴근은 하루 1회지만, 휴게는 여러 번 가능 (점심, 오후 휴식 등)
     * - 그래서 existsBy가 아닌 countBy로 개수를 비교함
     * - DailyAttendance 갱신 없음 → BREAK_END에서 한 번에 휴게시간 누적
     *
     * ===== 워크플로우 =====
     *
     * [단계 1] resolveEventTime — 시간 결정
     *
     * [단계 2] findDaily — 출근 상태 확인
     * - 출근 안 했으면 400 에러
     *
     * [단계 3] 진행 중인 휴게 체크
     * - BREAK_START 수 > BREAK_END 수 → 아직 이전 휴게가 안 끝남
     * - 예: BREAK_START 2개, BREAK_END 1개 → 현재 휴게 중 → 409
     *
     * [단계 4] 로그 저장
     * - DailyAttendance 갱신 없음!
     * → 휴게 시간은 BREAK_END에서 "시작~종료 차이"를 계산해서 누적하니까
     *         시작 시점에서는 아직 몇 분인지 모름
     *
     */
    public AttendanceLogResDto breakStart(UUID companyId, UUID memberId, AttendanceLogCreateReqDto reqDto){
        // 휴게 시간 결정
        LocalDateTime eventTime = resolveEventTime(reqDto.getEventTime());
        LocalDate today = eventTime.toLocalDate();

        // 당일 DailyAttendance 조회 (출근 안 했으면 400)
        DailyAttendance dailyAttendance = dailyAttendanceService.findDaily(companyId, memberId, today);

        // 진행 중인 휴게 체크
        Long breakStartCount = attendanceLogRepository
                .countByDailyAttendanceAndEventType(dailyAttendance, EventType.BREAK_START);
        Long breakEndCount = attendanceLogRepository
                .countByDailyAttendanceAndEventType(dailyAttendance, EventType.BREAK_END);
        if(breakStartCount > breakEndCount){
            throw new BusinessException(HttpStatus.CONFLICT, "이미 휴게 중입니다. 휴게 종료를 먼저 해주세요.");
        }

        // 휴게 시작 로그 저장
        AttendanceLog log = reqDto.toEntity(dailyAttendance, EventType.BREAK_START, eventTime);
        AttendanceLog savedLog = attendanceLogRepository.save(log);

        return AttendanceLogResDto.fromEntity(savedLog);
    }

    /** 휴게 종료 (BREAK_END)
     *
     * ===== 사용자 시나리오 =====
     * 13:00 사원이 "휴게 종료" 버튼 터치 (점심 끝)
     * → POST /attendance/break-end
     * → 이 메서드 실행
     *
     * ===== 워크플로우 =====
     *
     * [단계 1] resolveEventTime — 시간 결정
     *
     * [단계 2] findDaily — 출근 상태 확인
     *
     * [단계 3] 매칭되는 BREAK_START 확인
     * - BREAK_START 수 <= BREAK_END 수 → 시작 없이 종료 누름 → 400
     *
     * [단계 4] 로그 저장
     *
     * [단계 5] 휴게 시간 계산 후 DailyAttendance에 누적
     * - 마지막 BREAK_START 시간 조회
     * - Duration.between(시작, 종료) → 분 계산
     * - daily.addBreakMinutes()로 누적
     *
     * ===== 계산 예시 =====
     * BREAK_START 12:00, BREAK_END 13:00 → 60분
     * BREAK_START 15:00, BREAK_END 15:15 → 15분
     * totalBreakMinutes = 60 + 15 = 75분 (누적)
     *
     * 이 75분이 퇴근 시 recalculateMinutes()에서 근무시간에서 차감됨:
     * workedMinutes = (퇴근-출근) - breakMinutes - 75
     */
    public AttendanceLogResDto breakEnd(UUID companyId, UUID memberId, AttendanceLogCreateReqDto reqDto){
        // 휴게 종료 시간 결정
        LocalDateTime eventTime = resolveEventTime(reqDto.getEventTime());
        LocalDate today = eventTime.toLocalDate();

        // DailyAttendance 조회
        DailyAttendance dailyAttendance = dailyAttendanceService.findDaily(companyId, memberId, today);

        // 매칭되는 BREAK_START 있는지 확인
        long breakStartCount = attendanceLogRepository
                .countByDailyAttendanceAndEventType(dailyAttendance, EventType.BREAK_START);
        long breakEndCount = attendanceLogRepository
                .countByDailyAttendanceAndEventType(dailyAttendance, EventType.BREAK_END);

        if (breakStartCount <= breakEndCount) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "휴게 시작 기록이 없습니다.");
        }

        // 휴게 종료 로그 저장
        AttendanceLog log = reqDto.toEntity(dailyAttendance, EventType.BREAK_END, eventTime);
        AttendanceLog savedLog = attendanceLogRepository.save(log);

        //이번 휴게 시간 계산 → DailyAttendance에 누적
        // 1. 가장 최근 BREAK_START 시간 조회
        AttendanceLog lastBreakStart = attendanceLogRepository
                .findTop1ByDailyAttendanceAndEventTypeOrderByEventTimeDesc(dailyAttendance, EventType.BREAK_START)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "휴게 시작 기록을 찾을 수 없습니다."));

        // 2. 휴게 시간(분) 계산
          // 예: BREAK_START 12:00, BREAK_END 13:00 → 60분
        int breakMinutes = (int) Duration.between(lastBreakStart.getEventTime(), eventTime).toMinutes();

        // 3. DailyAttendance.totalBreakMinutes에 누적
        // 여러 번 휴게해도 계속 더해짐 (60 + 15 + 10 = 85분)
        dailyAttendance.addBreakMinutes(breakMinutes);

        return AttendanceLogResDto.fromEntity(savedLog);
    }

    /**
     * 특정 날짜의 출퇴근 로그 목록 조회 (시간순)
     *
     * findDailyAttendance()는 "요약"만 보여줌 (출근시간, 퇴근시간, 총 근무시간).
     * 사원이 "오늘 내가 언제 휴게 시작하고 끝냈지?" 같은 상세 이력을 보고 싶을 때
     * 이 메서드로 개별 로그를 시간순으로 조회
     *
     * ===== 호출 시점 =====
     * 사원이 "오늘 출퇴근 상세" 탭 클릭
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
