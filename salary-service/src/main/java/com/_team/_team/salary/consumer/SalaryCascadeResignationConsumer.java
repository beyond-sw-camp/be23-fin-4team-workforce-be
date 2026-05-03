package com._team._team.salary.consumer;

import com._team._team.attendance.domain.enums.BalanceType;
import com._team._team.attendance.domain.enums.ScheduleApprovalStatus;
import com._team._team.attendance.repository.MemberBalanceRepository;
import com._team._team.attendance.repository.MemberScheduleSelectionRepository;
import com._team._team.event.ResignationApprovalEvent;
import com._team._team.salary.repository.MemberAllowanceRepository;
import com._team._team.salary.repository.SalaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 사직서 결재 결과 Kafka 수신 -> 급여/수당/스케줄/잔액 cascade 처리
 */
@Slf4j
@Component
public class SalaryCascadeResignationConsumer {

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    /** 퇴직월 이후의 미래 스케줄 선택을 CANCELLED 로 전환할 대상 상태들. */
    private static final List<ScheduleApprovalStatus> CANCELLABLE_FUTURE_STATUSES = List.of(
            ScheduleApprovalStatus.PENDING,
            ScheduleApprovalStatus.APPROVED,
            ScheduleApprovalStatus.AUTO
    );

    private final SalaryRepository salaryRepository;
    private final MemberAllowanceRepository memberAllowanceRepository;
    private final MemberScheduleSelectionRepository memberScheduleSelectionRepository;
    private final MemberBalanceRepository memberBalanceRepository;

    @Autowired
    public SalaryCascadeResignationConsumer(
            SalaryRepository salaryRepository,
            MemberAllowanceRepository memberAllowanceRepository,
            MemberScheduleSelectionRepository memberScheduleSelectionRepository,
            MemberBalanceRepository memberBalanceRepository) {
        this.salaryRepository = salaryRepository;
        this.memberAllowanceRepository = memberAllowanceRepository;
        this.memberScheduleSelectionRepository = memberScheduleSelectionRepository;
        this.memberBalanceRepository = memberBalanceRepository;
    }

    @KafkaListener(
            topics = ResignationApprovalEvent.TOPIC,
            groupId = "salary-service-resignation",
            properties = {
                    JsonDeserializer.VALUE_DEFAULT_TYPE +
                            "=com._team._team.event.ResignationApprovalEvent"
            }
    )
    @Transactional
    public void consume(ResignationApprovalEvent event) {
        log.info("[Resignation-Cascade] received. requestId={}, action={}, memberId={}, resignDate={}",
                event.getRequestId(), event.getAction(),
                event.getMemberId(), event.getResignDate());

        if (event.getAction() != ResignationApprovalEvent.Action.APPROVE) {
            log.info("[Resignation-Cascade] non-approve action - skipped. action={}", event.getAction());
            return;
        }

        if (event.getMemberId() == null || event.getCompanyId() == null) {
            log.warn("[Resignation-Cascade] memberId/companyId null - skipped. requestId={}", event.getRequestId());
            return;
        }

        LocalDate retireDate = event.getResignDate() != null
                ? event.getResignDate()
                : LocalDate.now();

        // 1) Salary 활성/미래 행 close
        int closedSalaries = salaryRepository.closeActiveByMemberOnRetire(
                event.getMemberId(), event.getCompanyId(), retireDate);
        log.info("[Resignation-Cascade] salary closed: {} rows. memberId={}, retireDate={}",
                closedSalaries, event.getMemberId(), retireDate);

        // 2) MemberAllowance 활성 수당 effectiveTo sync
        int syncedAllowances = memberAllowanceRepository.syncEffectiveToByMember(
                event.getMemberId(), event.getCompanyId(), retireDate);
        log.info("[Resignation-Cascade] allowance synced: {} rows. memberId={}",
                syncedAllowances, event.getMemberId());

        // 3) 미래 월 스케줄 선택 cancel (퇴직월 이후)
        String retireYearMonth = retireDate.format(YEAR_MONTH_FMT);
        int cancelledSelections = memberScheduleSelectionRepository
                .cancelFutureSelectionsByMember(
                        event.getMemberId(),
                        retireYearMonth,
                        ScheduleApprovalStatus.CANCELLED,
                        CANCELLABLE_FUTURE_STATUSES);
        log.info("[Resignation-Cascade] schedule selections cancelled: {} rows. memberId={}, after={}",
                cancelledSelections, event.getMemberId(), retireYearMonth);

        // 4) 잔여 연차/이월/월차 - 사용 불가 처리. remaining 값은 보존 (Phase 2 미사용 연차 수당 환산용)
        // 비활성화 직전 잔액을 로그로 기록 (정산 자료용)
        Double annualRemainingBeforeBlock = memberBalanceRepository
                .sumRemainingByMemberAndType(event.getMemberId(), event.getCompanyId(), BalanceType.ANNUAL);
        Double carryoverRemainingBeforeBlock = memberBalanceRepository
                .sumRemainingByMemberAndType(event.getMemberId(), event.getCompanyId(), BalanceType.CARRYOVER);
        int blockedBalances = memberBalanceRepository
                .markAllUnusableByMember(event.getMemberId(), event.getCompanyId());
        log.info("[Resignation-Cascade] balances blocked: {} rows. annualRemaining={}, carryoverRemaining={}",
                blockedBalances, annualRemainingBeforeBlock, carryoverRemainingBeforeBlock);

        log.info("[Resignation-Cascade] done. memberId={} salary={} allowance={} schedule={} balance={}",
                event.getMemberId(), closedSalaries, syncedAllowances, cancelledSelections, blockedBalances);
    }
}
