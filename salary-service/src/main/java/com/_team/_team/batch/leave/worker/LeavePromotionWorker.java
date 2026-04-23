package com._team._team.batch.leave.worker;

import com._team._team.attendance.domain.LeavePolicy;
import com._team._team.attendance.domain.LeavePromotionLog;
import com._team._team.attendance.domain.MemberBalance;
import com._team._team.attendance.domain.enums.PromotionStage;
import com._team._team.attendance.repository.LeavePolicyRepository;
import com._team._team.attendance.repository.LeavePromotionLogRepository;
import com._team._team.attendance.repository.MemberBalanceRepository;
import com._team._team.dto.NotificationMessage;
import com._team._team.notification.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 연차사용촉진제 알림 배치 (근로기준법 61조)
 * 매일 만료일 기준 1차/2차 촉진 대상 잔고에 알림 발송, 이력 기록
 */
@Slf4j
@Component
public class LeavePromotionWorker {

    private final LeavePolicyRepository leavePolicyRepository;
    private final MemberBalanceRepository memberBalanceRepository;
    private final LeavePromotionLogRepository promotionLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public LeavePromotionWorker(LeavePolicyRepository leavePolicyRepository,
                                MemberBalanceRepository memberBalanceRepository,
                                LeavePromotionLogRepository promotionLogRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.leavePolicyRepository = leavePolicyRepository;
        this.memberBalanceRepository = memberBalanceRepository;
        this.promotionLogRepository = promotionLogRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result run(LocalDate today) {
        Result result = new Result();

        List<LeavePolicy> policies = leavePolicyRepository
                .findByIsPromotionYnAndDelYn("Y", "N");

        for (LeavePolicy policy : policies) {
            processStage(policy, today, PromotionStage.FIRST,
                    policy.getPromotion1stBeforeDays(), result);
            processStage(policy, today, PromotionStage.SECOND,
                    policy.getPromotion2ndBeforeDays(), result);
        }

        log.info("[LeavePromotion] date={} 1차={} 2차={} skip={}",
                today, result.firstSent, result.secondSent, result.skipped);
        return result;
    }

    // 단계별 촉진 대상 조회 후 알림 + 로그 저장
    private void processStage(LeavePolicy policy, LocalDate today,
                              PromotionStage stage, Integer beforeDays, Result result) {
        if (beforeDays == null || beforeDays <= 0) return;

        LocalDate targetExpiration = today.plusDays(beforeDays);
        List<MemberBalance> balances = memberBalanceRepository
                .findForPromotion(policy.getCompanyId(), targetExpiration);

        for (MemberBalance b : balances) {
            if (promotionLogRepository.existsByMemberBalanceIdAndStage(
                    b.getMemberBalanceId(), stage)) {
                result.skipped++;
                continue;
            }
            publishPromotion(b, stage);
            promotionLogRepository.save(LeavePromotionLog.builder()
                    .memberBalanceId(b.getMemberBalanceId())
                    .memberId(b.getMemberId())
                    .companyId(b.getCompanyId())
                    .stage(stage)
                    .sentOn(today)
                    .build());
            if (stage == PromotionStage.FIRST) result.firstSent++;
            else result.secondSent++;
        }
    }

    private void publishPromotion(MemberBalance b, PromotionStage stage) {
        NotificationType type = stage == PromotionStage.FIRST
                ? NotificationType.LEAVE_PROMOTION_FIRST
                : NotificationType.LEAVE_PROMOTION_SECOND;
        String content = (stage == PromotionStage.FIRST ? "[연차 촉진 1차] " : "[연차 촉진 2차] ")
                + "남은 연차 " + b.getRemaining() + "일, 만료일 "
                + b.getExpirationDate() + ". 사용 계획을 확인해주세요.";
        eventPublisher.publishEvent(NotificationMessage.builder()
                .receiverId(b.getMemberId())
                .senderId(null)
                .notificationType(type)
                .content(content)
                .targetId(b.getMemberBalanceId())
                .targetType("MEMBER_BALANCE")
                .build());
    }

    public static class Result {
        public int firstSent = 0;
        public int secondSent = 0;
        public int skipped = 0;
    }
}