package com._team._team.evaluation.dto.reqdto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class PeerAssignmentCreateReqDto {

    @NotNull(message = "평가 ID는 필수입니다.")
    private UUID evaluationId;

    @NotNull(message = "피평가자 ID는 필수입니다.")
    private UUID evaluateeId;

    @NotNull(message = "동료 평가자 ID는 필수입니다.")
    private UUID peerMemberId;

    @NotNull(message = "배정자 ID는 필수입니다.")
    private UUID assignedBy;
}
