package com._team._team.notification;

public enum NotificationType {

    // 근태
    ATTENDANCE_MODIFIED,     // 근태 수정
    LEAVE_REQUESTED,         // 연차 신청
    LEAVE_APPROVED,          // 연차 승인
    LEAVE_REJECTED,          // 연차 반려

    // 결재
    APPROVAL_REQUESTED,      // 결재 요청
    APPROVAL_APPROVED,       // 결재 승인
    APPROVAL_REJECTED,       // 결재 반려

    // 급여
    SALARY_PUBLISHED,        // 급여 명세서 발행

    // 목표
    GOAL_EVALUATED,          // 목표 평가 완료

    // 인사
    MEMBER_DORMANT,          // 휴직 처리
    MEMBER_RETURN            // 복직 처리
}

