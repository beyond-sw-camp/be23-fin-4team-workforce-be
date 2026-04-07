package com._team._team.evaluation.dto.reqdto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class EvaluationScoreUpdateReqDto {

    private BigDecimal quantScore;
    private BigDecimal qualScore;
    private String goalScoresJson;
    private String rubricScoresJson;
    private String comment;
    private String strengthComment;
    private String improveComment;
}
