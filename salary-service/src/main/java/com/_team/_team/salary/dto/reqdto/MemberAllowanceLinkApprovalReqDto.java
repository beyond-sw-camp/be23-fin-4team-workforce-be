package com._team._team.salary.dto.reqdto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

// 수당신청 전자결재 생성 후 approvalRequestId 연결용
@Getter
@NoArgsConstructor
public class MemberAllowanceLinkApprovalReqDto {

    @NotNull
    private UUID approvalRequestId;
}