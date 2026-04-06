package com._team._team.attendance.domain;

import com._team._team.attendance.domain.enums.ExpenseStatus;
import com._team._team.attendance.domain.enums.ExpenseType;
import com._team._team.attendance.domain.enums.WorkTripType;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Getter
public class WorkTripDetail extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID workTripDetailId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private UUID companyId;

    /** 출장/외근 구분 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkTripType workTripType;

    /** 목적지 주소 */
    @Column
    private String destination;

    /** 목적지 GPS 위도 */
    private Double destinationLat;

    /** 목적지 GPS 경도 */
    private Double destinationLng;

    /** 출장/외근 목적 */
    @Column
    private String purpose;

    /** 경비 금액 (원) */
    private Long expenseAmount;

    /** 경비 유형 (TRANSPORT/ACCOMMODATION/MEAL/ETC) */
    @Enumerated(EnumType.STRING)
    @Column
    private ExpenseType expenseType;

    /** 경비 처리 상태 - approval-service 결재 경과 반영 */
    @Enumerated(EnumType.STRING)
    @Column
    @Builder.Default
    private ExpenseStatus expenseStatus = ExpenseStatus.PENDING;

    /** 소속 일별 근태 - 해당일과 연결 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_attendance_id")
    private DailyAttendance dailyAttendance;

    @Column(nullable = false)
    @Builder.Default
    private String delYn = "N";
}
