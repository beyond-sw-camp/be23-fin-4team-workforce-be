package com._team._team.hr.batch.salary.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 퇴직금 산정 배치(또는 퇴직 처리 연동)용 워커
 * 직전 3개월 임금 총액 기반 평균임금, 미사용 연차수당 등 역산·집계 쿼리(QueryDSL 검토)
 */
@Slf4j
@Component
public class SeverancePayWorker {

    /** 퇴직금 계산 실행 */
    public void run() {
        // 퇴사/퇴직 확정 API·테이블 붙으면 여기서 대상 조회 → 평균임금 산출
        log.info("SeverancePayWorker: 퇴직 연동 데이터 없음 — 계산 스킵");
    }
}
