package com._team._team.evaluation.service.scoring;

import com._team._team.evaluation.domain.enums.SectionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * [L-1] 섹션별 채점 결과 VO.
 *
 * 저장되지 않고, 응답 조회 시 design + answers + goalSnapshot 로부터 재계산된다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SectionScore {

    private String sectionId;
    private String title;
    private SectionType type;

    /** 섹션 가중치 (원본). 합산 100 기준 가정. */
    private BigDecimal weight;

    /** 0~100 스케일 점수. 데이터가 부족해 채점 불가능한 경우 null. */
    private BigDecimal score;

    /** 채점이 스킵되었는지 여부. 스킵된 섹션은 총점 가중 평균에서 제외된다. */
    private boolean skipped;

    /** 스킵된 이유 — 디버깅/UI 표시용. */
    private String reason;

    /** 섹션에 사용된 신호 개수(답변 수 / 스냅샷 수). UI 에서 근거 표시용. */
    private Integer sampleSize;
}
