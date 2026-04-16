package com._team._team.attendance.Controller;

import com._team._team.attendance.Service.AttendanceContextResolver;
import com._team._team.attendance.Service.AttendanceLogService;
import com._team._team.attendance.Service.DailyAttendanceService;
import com._team._team.attendance.dto.reqDto.AttendanceLogCreateReqDto;
import com._team._team.attendance.dto.resDto.AttendanceLogResDto;
import com._team._team.attendance.dto.resDto.DailyAttendanceResDto;
import com._team._team.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 출퇴근/휴게 Controller
 * ===== 엔드포인트 목록 =====
 *
 * [출퇴근 이벤트 — 사원 본인만 호출]
 * POST /attendance/clock-in       출근 (하루 1회)
 * POST /attendance/clock-out      퇴근 (하루 1회)
 * POST /attendance/break-start    휴게 시작 (여러 번 가능)
 * POST /attendance/break-end      휴게 종료
 *
 * [개인 조회 — 사원 본인]
 * GET  /attendance/daily/{date}   특정일 근태 요약 조회
 * GET  /attendance/logs/{date}    특정일 이벤트 로그 목록 조회
 * GET  /attendance/monthly        월간 근태 조회 (페이징)
 *
 * [관리자 조회 — HR 관리자]
 * GET  /attendance/company/daily     전 직원 특정일 근태 (페이징)
 * GET  /attendance/company/monthly   전 직원 월간 근태 (페이징)
 */
@RestController
@RequestMapping("/attendance")
public class AttendanceLogController {
    private final AttendanceLogService attendanceLogService;
    private final DailyAttendanceService dailyAttendanceService;
    private final AttendanceContextResolver attendanceContextResolver;

    @Autowired
    public AttendanceLogController(AttendanceLogService attendanceLogService,
                                   DailyAttendanceService dailyAttendanceService,
                                   AttendanceContextResolver attendanceContextResolver) {
        this.attendanceLogService = attendanceLogService;
        this.dailyAttendanceService = dailyAttendanceService;
        this.attendanceContextResolver = attendanceContextResolver;
    }

    // 출근 처리 - IP/UA/좌표→주소 태그 붙여서 서비스로 넘긴다
    @PostMapping("/clock-in")
    public ResponseEntity<?> clockIn(
            @RequestHeader("X-User-CompanyId")UUID companyId,
            @RequestHeader("X-User-UUID") UUID memberId,
            @Valid @RequestBody AttendanceLogCreateReqDto reqDto,
            HttpServletRequest httpRequest){
        tagContext("CLOCK_IN", memberId, reqDto, httpRequest);
        DailyAttendanceResDto resDto = attendanceLogService.clockIn(companyId, memberId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "출근 처리되었습니다."),
                HttpStatus.CREATED
        );
    }

    // 퇴근 처리
    @PostMapping("/clock-out")
    public ResponseEntity<?> clockOut(
            @RequestHeader("X-User-CompanyId")UUID companyId,
            @RequestHeader("X-User-UUID") UUID memberId,
            @Valid @RequestBody AttendanceLogCreateReqDto reqDto,
            HttpServletRequest httpRequest) {
        tagContext("CLOCK_OUT", memberId, reqDto, httpRequest);
        DailyAttendanceResDto resDto = attendanceLogService.clockOut(companyId, memberId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "퇴근 처리되었습니다."),
                HttpStatus.CREATED
        );
    }

    // 휴게 시작
    @PostMapping("/break-start")
    public ResponseEntity<?> breakStart(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestHeader("X-User-UUID") UUID memberId,
            @Valid @RequestBody AttendanceLogCreateReqDto reqDto,
            HttpServletRequest httpRequest){
        tagContext("BREAK_START", memberId, reqDto, httpRequest);
        AttendanceLogResDto resDto = attendanceLogService.breakStart(companyId, memberId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "휴게가 시작되었습니다."),
                HttpStatus.CREATED
        );
    }

    // 휴게 종료
    @PostMapping("/break-end")
    public ResponseEntity<?> breakEnd(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestHeader("X-User-UUID") UUID memberId,
            @Valid @RequestBody AttendanceLogCreateReqDto reqDto,
            HttpServletRequest httpRequest) {
        tagContext("BREAK_END", memberId, reqDto, httpRequest);
        AttendanceLogResDto resDto = attendanceLogService.breakEnd(companyId, memberId, reqDto);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "휴게가 종료되었습니다."),
                HttpStatus.CREATED
        );
    }

    // 컨텍스트 해석 → reqDto.deviceId 덮어쓰기 + 감사 로그 한 줄.
    // 스키마 건드리지 않으려고 IP/UA는 deviceId 안에 패킹해서 저장한다.
    private void tagContext(String eventType,
                            UUID memberId,
                            AttendanceLogCreateReqDto reqDto,
                            HttpServletRequest httpRequest) {
        AttendanceContextResolver.Context ctx = attendanceContextResolver.resolve(
                httpRequest,
                reqDto.getDeviceId(),
                reqDto.getLatitude(),
                reqDto.getLongitude());
        reqDto.setDeviceId(ctx.packedDeviceId());
        attendanceContextResolver.writeAuditLog(eventType, memberId, ctx);
    }

    // ==================== 개인 조회 ====================

    // 개인 조회 - 사원 본인이 자기 근태 확인
    @GetMapping("/daily/{date}")
    public ResponseEntity<?> findDailyAttendance(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestHeader("X-User-UUID") UUID memberId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyAttendanceResDto resDto = dailyAttendanceService.findDailyAttendance(companyId, memberId, date);
        return new ResponseEntity<>(
                ApiResponse.success(resDto, "일별 근태 조회 성공"),
                HttpStatus.OK
        );
    }

    // 개인 조회 - 특정 날짜 출퇴근 로그 목록 조회
    @GetMapping("/logs/{date}")
    public ResponseEntity<?> findLogs(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestHeader("X-User-UUID") UUID memberId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AttendanceLogResDto> resDtoList = attendanceLogService.findLogs(companyId, memberId, date);
        return new ResponseEntity<>(
                ApiResponse.success(resDtoList, "출퇴근 로그 조회 성공"),
                HttpStatus.OK
        );
    }

    // 개인 조회 - 월간 근태 조회 (페이징)
    @GetMapping("/monthly")
    public ResponseEntity<?> findMonthlyByMember(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 31) Pageable pageable) {
        Page<DailyAttendanceResDto> resDtoPage = dailyAttendanceService
                .findMonthlyByMember(companyId, memberId, from, to, pageable);
        return new ResponseEntity<>(
                ApiResponse.success(resDtoPage, "월간 근태 조회 성공"),
                HttpStatus.OK
        );
    }

    // ==================== 관리자 조회 ====================

    /**
     * 회사 전체 특정일 근태 조회 - 관리자용 (페이징)
     */
    @GetMapping("/company/daily")
    public ResponseEntity<?> findDailyByCompany(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<DailyAttendanceResDto> resDtoPage = dailyAttendanceService
                .findDailyByCompany(companyId, date, pageable);
        return new ResponseEntity<>(
                ApiResponse.success(resDtoPage, "회사 일별 근태 조회 성공"),
                HttpStatus.OK
        );
    }

    /**
     * 회사 전체 월간 근태 조회 — 관리자용 (페이징)
     */
    @GetMapping("/company/monthly")
    public ResponseEntity<?> findMonthlyByCompany(
            @RequestHeader("X-User-CompanyId") UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<DailyAttendanceResDto> resDtoPage = dailyAttendanceService
                .findMonthlyByCompany(companyId, from, to, pageable);
        return new ResponseEntity<>(
                ApiResponse.success(resDtoPage, "회사 월간 근태 조회 성공"),
                HttpStatus.OK
        );
    }
}
