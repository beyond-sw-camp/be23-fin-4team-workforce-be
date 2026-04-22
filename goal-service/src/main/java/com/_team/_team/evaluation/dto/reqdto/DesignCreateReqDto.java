package com._team._team.evaluation.dto.reqdto;

import com._team._team.evaluation.domain.converter.EvaluationSection;
import com._team._team.evaluation.domain.converter.GradeConfig;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

/**
 * [D-4] 평가 설계 생성 요청 DTO.
 *
 * 두 가지 입력 포맷 지원:
 *   - sections / gradeConfig      : 타입 세이프 VO (권장)
 *   - sectionsJson / gradeConfigJson : 기존 프론트 호환용 JSON 문자열
 *
 * 서비스 레이어에서 VO 우선 → 없으면 JSON 파싱 순으로 처리한다.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DesignCreateReqDto {
    @NotBlank private String name;

    private List<EvaluationSection> sections;
    private GradeConfig gradeConfig;

    /** @deprecated VO 포맷(sections) 사용 권장. 호환용 유지. */
    @Deprecated
    private String sectionsJson;

    /** @deprecated VO 포맷(gradeConfig) 사용 권장. 호환용 유지. */
    @Deprecated
    private String gradeConfigJson;
}
