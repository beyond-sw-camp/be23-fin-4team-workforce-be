package com._team._team.attendance.Service;

import com._team._team.kafka.event.OvertimeApprovalEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OvertimeApprovalService {

    public void applyApproval(OvertimeApprovalEvent event) {
        // 엔티티 저장 없음.
        // 이벤트는 알림/로깅 용도로만 처리.
        // 실제 근태 계산 시에는 approval-service를 FeignClient로 조회.
        log.info("[Overtime] approved. memberId={}, {} ~ {}, workType={}",
                event.getMemberId(), event.getStartAt(), event.getEndAt(), event.getWorkType());

        // TODO: 실시간 알림 발행 (관리자/본인)
    }

    public void applyRejection(OvertimeApprovalEvent event) {
        log.info("[Overtime] rejected/canceled. memberId={}, requestId={}",
                event.getMemberId(), event.getRequestId());

        // TODO: 본인에게 반려 알림 + "즉시 퇴근 처리" 유도
    }
}
