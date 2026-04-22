package com._team._team.attendance.dto.reqDto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class LeaveRequestLinkApprovalReqDto {

    @NotNull(message = "결재 요청 ID 는 필수입니다.")
    private UUID approvalRequestId;
}