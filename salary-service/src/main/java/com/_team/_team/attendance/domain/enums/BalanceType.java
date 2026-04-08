package com._team._team.attendance.domain.enums;
/**
 * 휴가 잔여 유형
 * - ANNUAL: 당해 연차
 * - CARRYOVER: 전년도 이월 연차 (leave_policy.isCarryoverYn에 따라 발생)
 */
public enum BalanceType {
    ANNUAL,
    CARRYOVER
}