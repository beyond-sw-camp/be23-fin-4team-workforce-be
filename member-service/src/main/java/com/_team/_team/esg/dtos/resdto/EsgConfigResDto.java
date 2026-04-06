package com._team._team.esg.dtos.resdto;

import com._team._team.esg.domain.EsgCompanyConfig;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsgConfigResDto {

    private String esgEnabledYn;
    private String rewardEnabledYn;
    private String campaignEnabledYn;
    private String shopEnabledYn;
    private String goalSyncEnabledYn;
    private int monthlyPointLimit;
    private int goalWeightRatio;

    public static EsgConfigResDto fromEntity(EsgCompanyConfig config) {
        return EsgConfigResDto.builder()
                .esgEnabledYn(config.getEsgEnabledYn())
                .rewardEnabledYn(config.getRewardEnabledYn())
                .campaignEnabledYn(config.getCampaignEnabledYn())
                .shopEnabledYn(config.getShopEnabledYn())
                .goalSyncEnabledYn(config.getGoalSyncEnabledYn())
                .monthlyPointLimit(config.getMonthlyPointLimit())
                .goalWeightRatio(config.getGoalWeightRatio())
                .build();
    }
}