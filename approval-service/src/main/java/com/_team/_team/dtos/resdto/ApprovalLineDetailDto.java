package com._team._team.dtos.resdto;

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
public class ApprovalLineDetailDto {
    private UUID approvalLineId;
    private Integer stepOrder;
    private UUID memberPositionId;
    private String lineStatus;
    private String comment;
    private String isSignedYn;
    private LocalDateTime signedAt;
}
