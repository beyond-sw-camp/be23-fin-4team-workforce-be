package com._team._team.evaluation.dto.reqdto;

import com._team._team.evaluation.domain.enums.SeasonType;
import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SeasonUpdateReqDto {
    private String name;
    private SeasonType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate resultPublishDate;
    private String scheduleJson;
}
