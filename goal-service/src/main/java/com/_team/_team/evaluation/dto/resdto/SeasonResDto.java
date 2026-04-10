package com._team._team.evaluation.dto.resdto;

import com._team._team.evaluation.domain.EvaluationSeason;
import com._team._team.evaluation.domain.enums.SeasonStatus;
import com._team._team.evaluation.domain.enums.SeasonType;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SeasonResDto {
    private UUID seasonId;
    private UUID companyId;
    private String name;
    private SeasonType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private SeasonStatus status;
    private LocalDate resultPublishDate;
    private String scheduleJson;

    public static SeasonResDto from(EvaluationSeason e) {
        return SeasonResDto.builder()
                .seasonId(e.getSeasonId())
                .companyId(e.getCompanyId())
                .name(e.getName())
                .type(e.getType())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .status(e.getStatus())
                .resultPublishDate(e.getResultPublishDate())
                .scheduleJson(e.getScheduleJson())
                .build();
    }
}
