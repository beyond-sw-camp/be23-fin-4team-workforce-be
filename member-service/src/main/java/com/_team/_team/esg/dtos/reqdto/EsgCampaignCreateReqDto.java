package com._team._team.esg.dtos.reqdto;

import com._team._team.esg.domain.enums.EsgCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsgCampaignCreateReqDto {

    @NotBlank(message = "캠페인 제목은 필수입니다.")
    private String title;

    private String description;

    @NotNull(message = "카테고리는 필수입니다.")
    private EsgCategory category;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;

    @Positive(message = "보상 포인트는 0보다 커야 합니다.")
    private int rewardPoints;

    private Integer maxParticipants;
}