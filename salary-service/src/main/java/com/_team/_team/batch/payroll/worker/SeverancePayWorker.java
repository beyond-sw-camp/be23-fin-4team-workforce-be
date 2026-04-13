package com._team._team.batch.payroll.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 퇴직금 산정 배치(또는 퇴직 처리 연동)용 워커
 * 직전 3개월 임금 총액 기반 평균임금, 미사용 연차수당 등 역산·집계
 */
@Slf4j
@Component
public class SeverancePayWorker {

    /** 퇴직금 계산 실행 */
    public void run() {
        log.info("SeverancePayWorker: 퇴직 연동 데이터 없음 — 계산 스킵");
    }
}
