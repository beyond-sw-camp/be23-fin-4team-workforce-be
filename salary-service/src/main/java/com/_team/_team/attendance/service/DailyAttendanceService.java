package com._team._team.attendance.service;

import com._team._team.attendance.domain.AttendanceLog;
import com._team._team.attendance.repository.AttendanceLogRepository;
import com._team._team.attendance.repository.DailyAttendanceRepository;
import com._team._team.attendance.repository.WorkScheduleRepository;
import com._team._team.attendance.domain.DailyAttendance;
import com._team._team.attendance.domain.WorkSchedule;
import com._team._team.attendance.domain.enums.AttendanceStatus;
import com._team._team.attendance.domain.enums.ClosureStatus;
import com._team._team.attendance.domain.enums.CorrectionState;
import com._team._team.attendance.dto.resDto.DailyAttendanceResDto;
import com._team._team.dto.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@Service
@Transactional
public class DailyAttendanceService {
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final AttendanceLogRepository attendanceLogRepository;

    @Autowired
    public DailyAttendanceService(DailyAttendanceRepository dailyAttendanceRepository,
                                  WorkScheduleRepository workScheduleRepository,
                                  AttendanceLogRepository attendanceLogRepository) {
        this.dailyAttendanceRepository = dailyAttendanceRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.attendanceLogRepository = attendanceLogRepository;
    }

    /**
     * 해당 날짜의 DailyAttendance 조회, 없으면 새로 생성
     * - 출근(clockIn) 시 호출되어 당일 근태 레코드를 보장
     */
    public DailyAttendance findOrCreateDaily(UUID companyId, UUID memberId, LocalDate date){
        // 1. 당일 근태 기록이 이미 있으면 그대로 반환
        Optional<DailyAttendance> existing = dailyAttendanceRepository
                .findByCompanyIdAndMemberIdAndAttendanceDate(companyId, memberId, date);

        if (existing.isPresent()) {
            return existing.get();
        }

        // 2. 없으면 신규 생성
        // 2-1. 유효한 근무 스케줄 조회 (개인 스케줄 우선, 없으면 회사 기본)
        UUID scheduleId = resolveWorkScheduleId(companyId, memberId, date);

        /**
        * 2-2. DailyAttendance 생성
        * status = ABSENT: 아직 출근 로그가 안 찍힌 상태
        * -> 이후 clockIn()에서 NORMAL로 즉시 변경됨
        * workScheduleId: 이 사원에게 적용되는 스케줄 (overtime 계산 기준)
        */
        DailyAttendance dailyAttendance = DailyAttendance.builder()
                .companyId(companyId)
                .memberId(memberId)
                .attendanceDate(date)
                .status(AttendanceStatus.ABSENT)
                .workScheduleId(scheduleId)        // 개인 스케줄 우선, 없으면 회사 기본
                .build();

        return dailyAttendanceRepository.save(dailyAttendance);
    }

    /**
     * 해당 날짜의 DailyAttendance 조회 (없으면 예외)
     * 퇴근시 사용 (clockOut에서 호출)
     * 출근 기록 없이 호출되면 에러 (퇴근은 "출근한 상태"에서만 가능)
     */
    @Transactional(readOnly = true)
    public DailyAttendance findDaily(UUID companyId, UUID memberId, LocalDate date){
        return dailyAttendanceRepository
                .findByCompanyIdAndMemberIdAndAttendanceDate(companyId, memberId, date)
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
                .findByCompanyIdAndMemberIdAndAttendanceDate(companyId, memberId, date)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "해당 날짜의 근태 기록이 없습니다."));
        return DailyAttendanceResDto.fromEntity(daily);
    }

    /**
     * 개인 월간 근태 조회 (페이징)
     * - 사원 본인이 한 달 근태 내역 확인
     * - correctionState 채워서 응답 (정정 컬럼 분기에 사용)
     */
    @Transactional(readOnly = true)
    public Page<DailyAttendanceResDto> findMonthlyByMember(UUID companyId, UUID memberId, LocalDate from, LocalDate to, Pageable pageable) {
        Page<DailyAttendance> page = dailyAttendanceRepository
                .findMonthlyByMember(memberId, from, to, pageable);
        Map<UUID, CorrectionState> stateMap = computeCorrectionStates(page.getContent());
        return page.map(da -> DailyAttendanceResDto.fromEntity(
                da, stateMap.getOrDefault(da.getDailyAttendanceId(), CorrectionState.NORMAL)));
    }

    /**
     * 회사 전체 특정일 근태 조회 - 관리자 대시보드 (페이징)
     * - 오늘 출근 현황, 결근자 파악
     */
    @Transactional(readOnly = true)
    public Page<DailyAttendanceResDto> findDailyByCompany(UUID companyId, LocalDate date, Pageable pageable) {
        Page<DailyAttendance> page = dailyAttendanceRepository
                .findDailyByCompany(companyId, date, pageable);
        Map<UUID, CorrectionState> stateMap = computeCorrectionStates(page.getContent());
        return page.map(da -> DailyAttendanceResDto.fromEntity(
                da, stateMap.getOrDefault(da.getDailyAttendanceId(), CorrectionState.NORMAL)));
    }

    /**
     * 회사 전체 월간 근태 조회 (관리자가 월간 리포트 확인)
     * - 100명 × 22일 = 2,200건/월 → 페이징 필수
     * - daily_attendance만 조회
     */
    @Transactional(readOnly = true)
    public Page<DailyAttendanceResDto> findMonthlyByCompany(UUID companyId, LocalDate from, LocalDate to, Pageable pageable) {
        Page<DailyAttendance> page = dailyAttendanceRepository
                .findMonthlyByCompany(companyId, from, to, pageable);
        Map<UUID, CorrectionState> stateMap = computeCorrectionStates(page.getContent());
        return page.map(da -> DailyAttendanceResDto.fromEntity(
                da, stateMap.getOrDefault(da.getDailyAttendanceId(), CorrectionState.NORMAL)));
    }

    /**
     * 페이지의 모든 DA 에 대한 CorrectionState 일괄 계산 (N+1 회피)
     *
     *  PENDING    closureStatus = UNDER_REVIEW
     *  COMPLETED  ADMIN_MANUAL 정정 로그 중 isCorrectedYn=Y + correctedBy not null 1건이라도 있음
     *  ABNORMAL   status = NORMAL + 과거 일자 + clock 한쪽 이상 누락
     *  NORMAL     그 외
     */
    private Map<UUID, CorrectionState> computeCorrectionStates(List<DailyAttendance> page) {
        Map<UUID, CorrectionState> result = new HashMap<>();
        if (page == null || page.isEmpty()) return result;

        // ADMIN_MANUAL 로그를 가진 DA 중 "관리자 승인된 정정" 보유 daId 모음
        Set<UUID> daIds = new HashSet<>();
        for (DailyAttendance da : page) daIds.add(da.getDailyAttendanceId());

        List<AttendanceLog> adminManual = attendanceLogRepository
                .findAdminManualByDailyAttendanceIds(daIds);
        Set<UUID> completedDaIds = new HashSet<>();
        for (AttendanceLog log : adminManual) {
            if ("Y".equals(log.getIsCorrectedYn()) && log.getCorrectedBy() != null) {
                completedDaIds.add(log.getDailyAttendance().getDailyAttendanceId());
            }
        }

        LocalDate today = LocalDate.now();
        for (DailyAttendance da : page) {
            UUID daId = da.getDailyAttendanceId();
            if (da.getClosureStatus() == ClosureStatus.UNDER_REVIEW) {
                result.put(daId, CorrectionState.PENDING);
            } else if (completedDaIds.contains(daId)) {
                result.put(daId, CorrectionState.COMPLETED);
            } else if (isAbnormal(da, today)) {
                result.put(daId, CorrectionState.ABNORMAL);
            } else {
                result.put(daId, CorrectionState.NORMAL);
            }
        }
        return result;
    }

    /** 과거 일자 + 정상 출근 상태인데 clock 한 쪽 이상 누락 = 이상 */
    private boolean isAbnormal(DailyAttendance da, LocalDate today) {
        if (da.getAttendanceDate() == null) return false;
        if (da.getAttendanceDate().isAfter(today.minusDays(1))) return false;  // 미래·오늘 제외
        if (da.getStatus() != AttendanceStatus.NORMAL) return false;            // 휴가·결근 제외
        return da.getFirstClockIn() == null || da.getLastClockOut() == null;
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
