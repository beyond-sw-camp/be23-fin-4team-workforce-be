package com._team._team.dtos.resdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApproverCandidateDto {
    private UUID memberPositionId;
    private UUID memberId;
    private String memberName;
    private String organizationName;    // 부서명
    private String jobTitleName;        // 직책명 (팀장, 부장 등)
    private String jobGradeName;        // 직급명 (과장, 차장 등)
}
