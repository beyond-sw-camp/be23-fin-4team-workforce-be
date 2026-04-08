package com._team._team.attendance.domain.enums;

/**
 * 출퇴근 이벤트 유형
 * - CLOCK_IN/CLOCK_OUT: 하루 1회만 허용
 * - BREAK_START/BREAK_END: 여러 번 가능 (점심 등)
 */
public enum EventType {
    CLOCK_IN,
    CLOCK_OUT,
    BREAK_START,
    BREAK_END
}
