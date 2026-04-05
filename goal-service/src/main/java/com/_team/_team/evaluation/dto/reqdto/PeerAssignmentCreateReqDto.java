package com._team._team.evaluation.dto.reqdto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class PeerAssignmentCreateReqDto {

    @NotNull
    private UUID evaluationId;

    @NotNull
    private UUID evaluateeId;

    @NotNull
    private UUID peerMemberId;

    @NotNull
    private UUID assignedBy;
}
