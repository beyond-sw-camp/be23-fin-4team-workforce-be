package com._team._team.evaluation.dto.reqdto;

import com._team._team.evaluation.domain.enums.SeasonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SeasonCreateReqDto {
    @NotBlank private String name;
    @NotNull private SeasonType type;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    private LocalDate resultPublishDate;
    private String scheduleJson;
}
