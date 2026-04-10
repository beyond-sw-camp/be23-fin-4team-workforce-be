package com._team._team.evaluation.dto.reqdto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DesignUpdateReqDto {
    private String name;
    private String sectionsJson;
    private String gradeConfigJson;
}
