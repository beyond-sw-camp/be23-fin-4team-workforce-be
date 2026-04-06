package com._team._team.esg.dtos.reqdto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsgConfigUpdateReqDto {

    private String esgEnabledYn;
    private String rewardEnabledYn;
    private String campaignEnabledYn;
    private String shopEnabledYn;
    private int monthlyPointLimit;
}