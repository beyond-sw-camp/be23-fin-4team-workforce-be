package com._team._team.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 휴가 결재 이벤트 - approval-service -> salary-service
 * - 반려 시: approval-service가 RESTORE 이벤트 발행
 * - 취소 시: approval-service가 RESTORE 이벤트 발행
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class LeaveApprovalEvent {

    public static final String TOPIC = "leave-approval";

    private UUID companyId;
    private UUID memberId;
    private UUID requestId; // 멱등성 보장용
    private String balanceType;
    private Double days;
    private LocalDate leaveDate;
    private Action action;        // USE / RESTORE

    public enum Action {
        USE, RESTORE
    }
}
