package com._team._team.esg.domain;

import com._team._team.company.domain.Company;
import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EsgCompanyConfig extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID esgCompanyConfigId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT),
            nullable = false)
    private Company company;

    @Column(nullable = false)
    @Builder.Default
    private String esgEnabledYn = "NO";

    @Column(nullable = false)
    @Builder.Default
    private String rewardEnabledYn = "NO";

    @Column(nullable = false)
    @Builder.Default
    private String campaignEnabledYn = "NO";

    @Column(nullable = false)
    @Builder.Default
    private int monthlyPointLimit = 1000;

    @Column(nullable = false)
    @Builder.Default
    private String shopEnabledYn = "NO";

    // 도메인 메서드
    public void enableEsg()        { this.esgEnabledYn      = "YES"; }
    public void disableEsg()       { this.esgEnabledYn      = "NO";  }
    public void enableReward()     { this.rewardEnabledYn   = "YES"; }
    public void enableCampaign()   { this.campaignEnabledYn = "YES"; }
    public void enableShop() { this.shopEnabledYn = "YES"; }

    public void updatePointPolicy(int halfVacation, int fullVacation, int monthlyLimit) {
        this.monthlyPointLimit    = monthlyLimit;
    }



    public boolean isEsgEnabled()      { return "YES".equals(esgEnabledYn);      }
    public boolean isRewardEnabled()   { return "YES".equals(rewardEnabledYn);   }
    public boolean isCampaignEnabled() { return "YES".equals(campaignEnabledYn); }
    public boolean isShopEnabled() { return "YES".equals(shopEnabledYn); }

    public void update(String esgEnabledYn, String rewardEnabledYn,
                       String campaignEnabledYn, String shopEnabledYn,
                     int monthlyPointLimit) {
        this.esgEnabledYn      = esgEnabledYn;
        this.rewardEnabledYn   = rewardEnabledYn;
        this.campaignEnabledYn = campaignEnabledYn;
        this.shopEnabledYn     = shopEnabledYn;
        this.monthlyPointLimit = monthlyPointLimit;
    }
}
