package com._team._team.evaluation.dto.resdto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

// 회사 + 시즌 단위 직원별 최종 등급 - 성과급 발행 용
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberFinalGradeResDto {
    private UUID memberId;
    private String finalGrade;
    private LocalDateTime confirmedAt;
}
