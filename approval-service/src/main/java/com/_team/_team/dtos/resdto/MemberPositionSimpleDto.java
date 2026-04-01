package com._team._team.dtos.resdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MemberPositionSimpleDto {
    private UUID memberPositionId;
    private UUID memberId;
    private UUID organizationId;
    private UUID jobGradeId;
    private UUID jobTitleId;
}
