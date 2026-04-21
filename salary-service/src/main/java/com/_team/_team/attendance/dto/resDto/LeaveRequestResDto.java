package com._team._team.attendance.dto.resDto;

import com._team._team.attendance.domain.LeaveRequest;
import com._team._team.attendance.domain.enums.BalanceType;
import com._team._team.attendance.domain.enums.LeaveApprovalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class LeaveRequestResDto {

    private UUID leaveRequestId;
    private UUID memberId;
    private UUID companyId;
    private UUID companyLeaveTypeId;

    private LocalDate startDate;
    private LocalDate endDate;
    private Double usageDays;

    private String reason;
    private String evidenceFileUrl;

    private UUID approvalRequestId;
    private LeaveApprovalStatus approvalStatus;
    private BalanceType deductedBalanceType;

    private LocalDateTime requestedAt;
    private LocalDateTime decidedAt;
    private UUID decidedBy;
    private String decisionNote;

    public static LeaveRequestResDto fromEntity(LeaveRequest leaveRequest) {
        return LeaveRequestResDto.builder()
                .leaveRequestId(leaveRequest.getLeaveRequestId())
                .memberId(leaveRequest.getMemberId())
                .companyId(leaveRequest.getCompanyId())
                .companyLeaveTypeId(leaveRequest.getCompanyLeaveTypeId())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .usageDays(leaveRequest.getUsageDays())
                .reason(leaveRequest.getReason())
                .evidenceFileUrl(leaveRequest.getEvidenceFileUrl())
                .approvalRequestId(leaveRequest.getApprovalRequestId())
                .approvalStatus(leaveRequest.getApprovalStatus())
                .deductedBalanceType(leaveRequest.getDeductedBalanceType())
                .requestedAt(leaveRequest.getRequestedAt())
                .decidedAt(leaveRequest.getDecidedAt())
                .decidedBy(leaveRequest.getDecidedBy())
                .decisionNote(leaveRequest.getDecisionNote())
                .build();
    }
}
