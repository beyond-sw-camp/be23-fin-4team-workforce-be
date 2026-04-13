package com._team._team.evaluation.dto.reqdto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DesignCreateReqDto {
    @NotBlank private String name;
    private String sectionsJson;
    private String gradeConfigJson;
}
