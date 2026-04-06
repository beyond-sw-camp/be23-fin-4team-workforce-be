package com._team._team.evaluation.domain.enums;

// 동료 평가 배정(요청)의 처리 상태.
public enum PeerAssignmentStatus {
    // 응답 대기
    PENDING,
    // 수락·완료
    COMPLETED,
    // 거절
    DECLINED
}
