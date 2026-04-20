package com._team._team.attendance.service;

import com._team._team.attendance.repository.MemberBalanceRepository;
import com._team._team.attendance.domain.MemberBalance;
import com._team._team.attendance.domain.enums.BalanceType;
import com._team._team.event.LeaveApprovalEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 휴가 결재 이벤트 처리 서비스
 * USE는 승인 시 차감, RESTORE는 반려/취소 시 원복
 */
@Slf4j
@Service
public class LeaveApprovalService {

    private final MemberBalanceRepository memberBalanceRepository;

    @Autowired
    public LeaveApprovalService(MemberBalanceRepository memberBalanceRepository) {
        this.memberBalanceRepository = memberBalanceRepository;
    }

    // 승인된 휴가 차감
    // 잔여가 모자라면 그냥 경고 로그만 찍고 넘어감 (차감은 안 함)
    @Transactional
    public void applyUse(LeaveApprovalEvent event) {
        MemberBalance balance = loadBalanceWithLock(event);

        if (balance.getRemaining() == null || balance.getRemaining() < event.getDays()) {
            log.warn("[Leave] 잔여 부족 - skip. requestId={}, remaining={}, requested={}",
                    event.getRequestId(), balance.getRemaining(), event.getDays());
            return;
        }
        balance.use(event.getDays());
        log.info("[Leave] USE applied. requestId={}, memberId={}, days={}, remaining={}",
                event.getRequestId(), event.getMemberId(), event.getDays(), balance.getRemaining());
    }

    // 반려/취소 시 복구
    // 승인 전 차감이 없는 구조라서 현재는 실제로 호출될 일 거의 없음 (추후 확장 대비)
    @Transactional
    public void applyRestore(LeaveApprovalEvent event) {
        MemberBalance balance = loadBalanceWithLock(event);
        balance.restore(event.getDays());
        log.info("[Leave] RESTORE applied. requestId={}, memberId={}, days={}, remaining={}",
                event.getRequestId(), event.getMemberId(), event.getDays(), balance.getRemaining());
    }

    // 동시 차감 방지용 비관적 락으로 잔액 로딩
    // 없으면 예외 던져서 컨슈머가 롤백/재시도 하도록 맡김
    private MemberBalance loadBalanceWithLock(LeaveApprovalEvent event) {
        BalanceType type = BalanceType.valueOf(event.getBalanceType());
        return memberBalanceRepository
                .findWithLock(event.getCompanyId(), event.getMemberId(), type)
                .orElseThrow(() -> new IllegalStateException(
                        "MemberBalance not found: companyId=" + event.getCompanyId()
                                + ", memberId=" + event.getMemberId() + ", type=" + type));
    }
}
