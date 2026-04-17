package com._team._team.attendance.domain;

import com._team._team.attendance.domain.enums.AttendanceStatus;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 일별 근태 집계
 * - attendance_log의 부모 테이블 (1:N)
 * - 월간 근태 조회 시 이 테이블만 조회 (성능 최적화)
 */
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Builder
public class DailyAttendance extends BaseTimeEntity {

    /** 점심시간(분) - 회사마다 실제 점심시간은 다를 수 있으나 우리 방식은 1시간으로 고정 */
    private static final int LUNCH_BREAK_MINUTES = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID dailyAttendanceId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private UUID companyId;

    /** 근무일 */
    @Column(nullable = false)
    private LocalDate attendanceDate;

    /** 근태 상태 (NORMAL/ABSENT/LEAVE/HALF) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.NORMAL;

    /** 적용된 근무 스케줄(overtime 계산 기준) */
    private UUID workScheduleId;

    /** 출근 시각 (하루 1회) */
    private LocalDateTime firstClockIn;

    /** 퇴근 시각 (하루 1회) */
    private LocalDateTime lastClockOut;

    /** 실 근무 시간(분) - (퇴근-출근) - 점심시간(1시간) */
    private Integer workedMinutes;

    /** 초과 근무 시간(분) - workMinutes가 스케줄 workMinutes 초과 시 */
    private Integer overtimeMinutes;

    /** 출퇴근 이벤트 로그 (상세 조회 시에만 LAZY 로딩) */
    @OneToMany(mappedBy = "dailyAttendance", fetch = FetchType.LAZY)
    @Builder.Default
    private List<AttendanceLog> logs = new ArrayList<>();

    /** 출장/외근 내역 */
    @OneToMany(mappedBy = "dailyAttendance", fetch = FetchType.LAZY)
    @Builder.Default
    private List<WorkTripDetail> trips = new ArrayList<>();

    //  집계 메서드

    /** 출근 기록 (하루 1회) */
    public void updateClockIn(LocalDateTime time) {
        this.firstClockIn = time;
    }

    /** 퇴근 기록 (하루 1회) */
    public void updateClockOut(LocalDateTime time) {
        this.lastClockOut = time;
    }

    /** 근무/초과 시간 재계산
     * 근무시간 = (퇴근 - 출근) - 점심시간(1시간 고정)
     * 체류시간이 점심시간보다 짧은 경우(조퇴 등) 0으로 처리
     */
    public void recalculateMinutes(int scheduleWorkMinutes){
        if(firstClockIn == null || lastClockOut == null) return;

        long totalMinutes = Duration.between(firstClockIn, lastClockOut).toMinutes();
        this.workedMinutes = (int) Math.max(0, totalMinutes - LUNCH_BREAK_MINUTES);

        if(this.workedMinutes > scheduleWorkMinutes){
            this.overtimeMinutes = this.workedMinutes - scheduleWorkMinutes;
        } else{
            this.overtimeMinutes = 0;
        }
    }

    /** 근태 상태 변경 (휴가 승인, 수동 수정 등) */
    public void updateStatus(AttendanceStatus status){
        this.status = status;
    }
}
