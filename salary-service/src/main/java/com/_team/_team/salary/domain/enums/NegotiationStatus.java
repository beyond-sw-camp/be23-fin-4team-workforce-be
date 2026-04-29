package com._team._team.salary.domain.enums;

/**
 * 연봉 협상 상태 전이
 *  DRAFT       관리자 입력 중
 *  SUBMITTED   직원에게 통보
 *  APPROVED    승인 완료 (자체 워크플로) 적용 대기
 *  REJECTED    반려 종료
 *  APPLIED     Salary 새 행 생성 적용 완료 잠금
 */
public enum NegotiationStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    APPLIED
}
