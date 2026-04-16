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
    APPROVAL_CANCELED,      // 결재 회수됨
    APPROVAL_REFERENCED,     // 결재 참조자
    APPROVAL_CIRCULATED,     // 결재 공람자

    // 급여
    SALARY_PUBLISHED,        // 급여 명세서 발행

    // 목표
    GOAL_EVALUATED,          // 목표 평가 완료

    // 평가
    EVALUATION_REMINDER,     // 미제출 평가 리마인드
    EVALUATION_REOPENED,     // 제출된 평가 재작성 허용

    // 인사
    MEMBER_DORMANT,          // 휴직 처리
    MEMBER_RETURN,            // 복직 처리

    //ESG
    ESG_ACTIVITY_APPROVED,   // 활동 승인됨
    ESG_ACTIVITY_REJECTED,   // 활동 반려됨
    ESG_POINT_EARNED,        // 포인트 적립됨
    ESG_CAMPAIGN_STARTED,    // 캠페인 시작
    ESG_CAMPAIGN_CLOSED,     // 캠페인 종료
    ESG_SHOP_ORDER_COMPLETE, // 물품 구매 완료

    // 캘린더
    CALENDAR_TEAM_EVENT_CREATED,  // 팀 일정 등록됨

    // 직원 관리
    MEMBER_ROLE_CHANGED,     // 역할 변경됨
    MEMBER_INFO_UPDATED,     // 인사 정보 수정됨
}

