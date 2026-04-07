package com._team._team.esg.dtos.resdto;

import com._team._team.esg.domain.EsgCampaign;
import com._team._team.esg.domain.enums.CampaignStatus;
import com._team._team.esg.domain.enums.EsgCategory;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsgCampaignResDto {

    private UUID esgCampaignId;
    private String title;
    private String description;
    private EsgCategory category;
    private String categoryDescription;
    private CampaignStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private int rewardPoints;
    private Integer maxParticipants;
    private LocalDateTime createdAt;

    public static EsgCampaignResDto fromEntity(EsgCampaign campaign) {
        return EsgCampaignResDto.builder()
                .esgCampaignId(campaign.getEsgCampaignId())
                .title(campaign.getTitle())
                .description(campaign.getDescription())
                .category(campaign.getCategory())
                .categoryDescription(campaign.getCategory().getDescription())
                .status(campaign.getStatus())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .rewardPoints(campaign.getRewardPoints())
                .maxParticipants(campaign.getMaxParticipants())
                .createdAt(campaign.getCreatedAt())
                .build();
    }
}