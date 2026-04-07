package com._team._team.goal.domain.enums;

// 목표 달성 여부를 판단할 때, 지표값이 어떤 방향이 더 좋은지.
public enum MeasureType {
    // 클수록 좋음
    HIGHER_BETTER,
    // 작을수록 좋음
    LOWER_BETTER,
    // 목표값과 일치할수록 좋음
    TARGET_MATCH
}
