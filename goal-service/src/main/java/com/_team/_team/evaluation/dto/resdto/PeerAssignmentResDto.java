package com._team._team.evaluation.dto.resdto;

import com._team._team.evaluation.domain.PeerAssignment;
import com._team._team.evaluation.domain.enums.PeerAssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeerAssignmentResDto {

    private UUID peerAssignmentId;
    private UUID evaluationId;
    private UUID evaluateeId;
    private UUID peerMemberId;
    private UUID assignedBy;
    private PeerAssignmentStatus status;
    private LocalDateTime notifiedAt;
    private LocalDateTime createdAt;

    public static PeerAssignmentResDto from(PeerAssignment peerAssignment) {
        return PeerAssignmentResDto.builder()
                .peerAssignmentId(peerAssignment.getPeerAssignmentId())
                .evaluationId(peerAssignment.getEvaluation().getEvaluationId())
                .evaluateeId(peerAssignment.getEvaluateeId())
                .peerMemberId(peerAssignment.getPeerMemberId())
                .assignedBy(peerAssignment.getAssignedBy())
                .status(peerAssignment.getStatus())
                .notifiedAt(peerAssignment.getNotifiedAt())
                .createdAt(peerAssignment.getCreatedAt())
                .build();
    }
}
