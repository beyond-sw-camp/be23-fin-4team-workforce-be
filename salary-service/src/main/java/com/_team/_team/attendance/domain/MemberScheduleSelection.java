package com._team._team.attendance.domain;

import com._team._team.attendance.domain.enums.ScheduleApprovalStatus;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
/**
 * 개인별 월 단위 시차출퇴근 스케줄 선택 이력
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Table(
        indexes = {
                // 개인 월별 최신 유효 선택 조회
                @Index(name = "idx_member_month_status",
                        columnList = "memberId, targetYearMonth, approvalStatus"),

                // 회사 전체 월별 현황 조회 (관리자 대시보드)
                @Index(name = "idx_company_month",
                        columnList = "companyId, targetYearMonth"),

                // 결재 대기 목록 (관리자 승인 화면)
                @Index(name = "idx_approval_status",
                        columnList = "companyId, approvalStatus")
        }
)
public class MemberScheduleSelection extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID selectionId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private UUID companyId;

    /**
     * 적용 대상 월, "YYYY-MM" 형식
     */
    @Column(nullable = false, length = 7)
    private String targetYearMonth;

    /**
     * 선택한 스케줄 ID
     */
    @Column(nullable = false)
    private UUID slotId;

    /**
     * 결재 상태
     * PENDING 상태는 실제 근무에 반영되지 않음
     * APPROVED 또는 AUTO 상태만 Resolver가 조회
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleApprovalStatus approvalStatus;

    /**
     * 변경 사유 (월 중 변경 시 필수)
     * 첫 선택 시에는 null 허용
     */
    @Column(length = 500)
    private String requestReason;

    /**
     * 요청자 UUID (본인 또는 관리자 대리)
     * 시스템 자동 할당이면 시스템 UUID
     */
    @Column(nullable = false)
    private UUID requestedBy;

    /**
     * 요청 시각
     */
    @Column(nullable = false)
    private LocalDateTime requestedAt;

    /**
     * 연동된 결재 요청 ID
     * AUTO 상태는 결재가 없으므로 null
     */
    private UUID approvalRequestId;

    /**
     * 승인 또는 반려 결정 시각
     */
    private LocalDateTime decidedAt;

    /**
     * 결정자 UUID (관리자)
     */
    private UUID decidedBy;

    /**
     * 반려 사유
     */
    @Column(length = 500)
    private String decisionNote;

    // 결재 ID 연결 (approval-service 결재 생성 후 회신된 UUID)
    public void linkApprovalRequest(UUID approvalRequestId) {
        this.approvalRequestId = approvalRequestId;
    }

    // 승인 처리
    public void approve(UUID approverId, LocalDateTime decidedAt) {
        if (this.approvalStatus != ScheduleApprovalStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 승인할 수 있습니다.");
        }
        this.approvalStatus = ScheduleApprovalStatus.APPROVED;
        this.decidedBy = approverId;
        this.decidedAt = decidedAt;
    }

    // 반려 처리
    public void reject(UUID approverId, LocalDateTime decidedAt, String note) {
        if (this.approvalStatus != ScheduleApprovalStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 반려할 수 있습니다.");
        }
        this.approvalStatus = ScheduleApprovalStatus.REJECTED;
        this.decidedBy = approverId;
        this.decidedAt = decidedAt;
        this.decisionNote = note;
    }

    // 취소 처리 (제출 후 본인이 철회)
    public void cancel() {
        if (this.approvalStatus != ScheduleApprovalStatus.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 취소할 수 있습니다.");
        }
        this.approvalStatus = ScheduleApprovalStatus.CANCELLED;
    }
}