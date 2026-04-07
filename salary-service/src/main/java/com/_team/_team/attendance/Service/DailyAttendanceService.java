package com._team._team.attendance.Service;

import com._team._team.attendance.Repository.DailyAttendanceRepository;
import com._team._team.attendance.Repository.WorkScheduleRepository;
import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.domain.WorkSchedule;
import com._team._team.attendance.domain.enums.AttendanceStatus;
import com._team._team.attendance.dto.resDto.DailyAttendanceResDto;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@Transactional
public class DailyAttendanceService {
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final WorkScheduleRepository workScheduleRepository;

    @Autowired
    public DailyAttendanceService(DailyAttendanceRepository dailyAttendanceRepository, WorkScheduleRepository workScheduleRepository) {
        this.dailyAttendanceRepository = dailyAttendanceRepository;
        this.workScheduleRepository = workScheduleRepository;
    }

    /**
     * 해당 날짜의 DailyAttendance 조회, 없으면 새로 생성
     * - 출근(clockIn) 시 호출되어 당일 근태 레코드를 보장
     *
     * [성능]
     * - (member_id, attendance_date) UNIQUE 인덱스 → SELECT 1회
     * - 최초 출근 시에만 INSERT 발생 (대부분 조회로 끝남)
     */
    public DailyAttendance findOrCreateDaily(UUID companyId, UUID memberId, LocalDate date){
        // 1. 당일 근태 기록이 이미 있으면 그대로 반환
        Optional<DailyAttendance> existing = dailyAttendanceRepository
                .findByMemberIdAndAttendanceDate(memberId, date);

        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. 없으면 신규 생성
        // 2-1. 유효한 근무 스케줄 조회 (개인 스케줄 우선, 없으면 회사 기본)
        UUID scheduleId = resolveWorkScheduleId(companyId, memberId, date);

        /**
        * 2-2. DailyAttendance 생성
        * status = ABSENT: 아직 출근 로그가 안 찍힌 상태
        * → 이후 clockIn()에서 NORMAL로 즉시 변경됨
        * totalBreakMinutes = 0: 휴게시간은 BREAK_END 이벤트 시 누적됨
        * workScheduleId: 이 사원에게 적용되는 스케줄 (overtime 계산 기준)
        */
        DailyAttendance dailyAttendance = DailyAttendance.builder()
                .companyId(companyId)
                .memberId(memberId)
                .attendanceDate(date)
                .status(AttendanceStatus.ABSENT)
                .workScheduleId(scheduleId)        // 개인 스케줄 우선, 없으면 회사 기본
                .totalBreakMinutes(0)
                .build();

        return dailyAttendanceRepository.save(dailyAttendance);
    }

    /**
     * 해당 날짜의 DailyAttendance 조회 (없으면 예외)
     * 퇴근, 휴게 시작/종료시 사용 (clockOut, breakStart, breakEnd에서 호출)
     * 출근 기록 없이 호출되면 400 에러 (퇴근/휴게 시작/휴게 종료는 "출근한 상태"에서만 가능)
     */
    @Transactional(readOnly = true)
    public DailyAttendance findDaily(UUID companyId, UUID memberId, LocalDate date){
        return dailyAttendanceRepository
                .findByMemberIdAndAttendanceDate(memberId, date)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST, "오늘 출근 기록이 없습니다. 출근을 먼저 해주세요."));
    }

    /**
     * 특정 날짜 근태 조회 (응답 DTO 변환)
     * - 사원이 자기 근태 확인할 때 사용
     */
    @Transactional(readOnly = true)
    public DailyAttendanceResDto findDailyAttendance(UUID companyId, UUID memberId, LocalDate date) {
        DailyAttendance daily = dailyAttendanceRepository
                .findByMemberIdAndAttendanceDate(memberId, date)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "해당 날짜의 근태 기록이 없습니다."));
        return DailyAttendanceResDto.fromEntity(daily);
    }

    /**
     * 개인 월간 근태 조회 (페이징)
     * - 사원 본인이 한 달 근태 내역 확인
     * - daily_attendance만 조회 (attendance_log JOIN 없음)
     * - 페이징 적용으로 한 번에 최대 31건 (한 달 근무일)
     */
    @Transactional(readOnly = true)
    public Page<DailyAttendanceResDto> findMonthlyByMember(UUID companyId, UUID memberId, LocalDate from, LocalDate to, Pageable pageable) {
        return dailyAttendanceRepository
                .findMonthlyByMember(memberId, from, to, pageable)
                .map(DailyAttendanceResDto::fromEntity);
    }

    /**
     * 회사 전체 특정일 근태 조회 — 관리자 대시보드 (페이징)
     * - 오늘 출근 현황, 결근자 파악
     * - (company_id, attendance_date) 인덱스 활용
     * - 직원 500명이어도 한 페이지 20~50건 단위로 조회
     */
    @Transactional(readOnly = true)
    public Page<DailyAttendanceResDto> findDailyByCompany(UUID companyId, LocalDate date, Pageable pageable) {
        return dailyAttendanceRepository
                .findDailyByCompany(companyId, date, pageable)
                .map(DailyAttendanceResDto::fromEntity);
    }

    /**
     * 회사 전체 월간 근태 조회 — (관리자가 월간 리포트 확인)
     * - 100명 × 22일 = 2,200건/월 → 페이징 필수
     * - daily_attendance만 조회
     */
    @Transactional(readOnly = true)
    public Page<DailyAttendanceResDto> findMonthlyByCompany(UUID companyId, LocalDate from, LocalDate to, Pageable pageable) {
        return dailyAttendanceRepository
                .findMonthlyByCompany(companyId, from, to, pageable)
                .map(DailyAttendanceResDto::fromEntity);
    }

    /**
     * 해당 날짜에 유효한 WorkSchedule의 기준 근무시간(분) 조회
     *
     * 퇴근 시 recalculateMinutes(scheduleWorkMinutes)를 호출하는데
     *  이때 "이 사원이 오늘 몇 분 근무해야 정상인지" 기준값이 필요
     *  예: 8시간 근무 스케줄 → 480분, 이 이상이면 overtimeMinutes로 잡힘
     *
     * ===== 스케줄 우선순위 =====
     * findActiveSchedules() 결과가 memberId DESC NULLS LAST로 정렬되어 있어서:
     * 1순위: 개인 스케줄 (memberId != null) → 이 사원 전용 근무시간
     * 2순위: 회사 기본 스케줄 (memberId = null) → 전사 공통 근무시간
     * 둘 다 없으면: 법정 기준 480분(8시간) 적용
     *
     * ===== 호출 시점 =====
     * AttendanceLogService.clockOut() 내
     * → 퇴근 시간 기록 후
     * → 이 메서드로 기준 근무시간 조회
     * → daily.recalculateMinutes(기준시간) 호출
     */
    @Transactional(readOnly = true)
    public int getScheduleWorkMinutes(UUID companyId, UUID memberId, LocalDate date){
        List<WorkSchedule> workSchedules = workScheduleRepository
                .findActiveSchedules(companyId, memberId, date);

        if (workSchedules.isEmpty()) {
            // 스케줄 미등록 시 근로기준법 기준 8시간(480분) 적용
            return 480;
        }

        // 첫 번째 = 최우선 스케줄 (개인 > 회사기본)
        return workSchedules.get(0).getWorkMinutes();

    }

    /**
     * 출근 시점에 유효한 WorkScheduleID 결정
     * DailyAttendance를 생성할 때 workScheduleId를 넣어두면,
     * 나중에 퇴근 시 getScheduleWorkMinutes()에서 또 스케줄 조회 안 해도 됨.
     *
     * ===== 호출 시점 =====
     * findOrCreateDaily() → 신규 DailyAttendance 생성 시에만 호출됨
     */
    private UUID resolveWorkScheduleId(UUID companyId, UUID memberId, LocalDate date) {
        List<WorkSchedule> schedules = workScheduleRepository
                .findActiveSchedules(companyId, memberId, date);

        if (schedules.isEmpty()) {
            return null;
        }
        return schedules.get(0).getWorkScheduleId();
    }
}
