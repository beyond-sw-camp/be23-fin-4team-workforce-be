package com._team._team.goal.domain.enums;

/**
 * 진행 상태(좋음/보통/어려움 등). 생명주기 {@link GoalStatus} 와 별개로 KPI 헬스를 표현합니다.
 */
public enum GoalHealthStatus {
    NOT_STARTED,
    ON_TRACK,
    AT_RISK,
    BEHIND,
    COMPLETED
}
