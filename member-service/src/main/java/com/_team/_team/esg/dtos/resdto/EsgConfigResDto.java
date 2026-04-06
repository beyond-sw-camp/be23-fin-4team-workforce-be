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
    private int monthlyPointLimit;

    public static EsgConfigResDto fromEntity(EsgCompanyConfig config) {
        return EsgConfigResDto.builder()
                .esgEnabledYn(config.getEsgEnabledYn())
                .rewardEnabledYn(config.getRewardEnabledYn())
                .campaignEnabledYn(config.getCampaignEnabledYn())
                .shopEnabledYn(config.getShopEnabledYn())
                .monthlyPointLimit(config.getMonthlyPointLimit())
                .build();
    }
}