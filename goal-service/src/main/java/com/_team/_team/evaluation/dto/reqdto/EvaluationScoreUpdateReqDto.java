package com._team._team.evaluation.dto.reqdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationScoreUpdateReqDto {

    private BigDecimal quantScore;
    private BigDecimal qualScore;
    private String goalScoresJson;
    private String rubricScoresJson;
    private String comment;
    private String strengthComment;
    private String improveComment;
}
