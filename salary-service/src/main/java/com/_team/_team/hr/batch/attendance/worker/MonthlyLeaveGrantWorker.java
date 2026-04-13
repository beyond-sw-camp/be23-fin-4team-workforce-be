package com._team._team.hr.batch.attendance.worker;

import com._team._team.attendance.Repository.MemberBalanceRepository;
import com._team._team.attendance.Service.AnnualLeaveGrantService;
import com._team._team.attendance.domain.LeavePolicy;
import com._team._team.attendance.domain.MemberBalance;
import com._team._team.attendance.domain.enums.BalanceType;
import com._team._team.dto.ApiResponse;
import com._team._team.salary.FeignClients.MemberFeignClient;
import com._team._team.salary.FeignClients.dto.MemberResDto;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 근속 1년 미만 직원에게 발생하는 월차(월 단위 누적)를 부여하는 워커
 * 회사별 월차 누적 부여량을 한 번에 조회하여 Map으로 보관한 뒤, Feign으로 조회한 직원 목록과 결합하여 N+1을 방지
 * 회사 단위 트랜잭션은 (propagation = REQUIRES_NEW)}로 분리
 */
@Component
public class MonthlyLeaveGrantWorker {

    /** 배치 flush 단위 */
    private static final int BATCH_SIZE = 500;

    private final MemberBalanceRepository memberBalanceRepository;
    private final MemberFeignClient memberFeignClient;
    private final EntityManager entityManager;

    @Autowired
    public MonthlyLeaveGrantWorker(MemberBalanceRepository memberBalanceRepository,
                                   MemberFeignClient memberFeignClient,
                                   EntityManager entityManager) {
        this.memberBalanceRepository = memberBalanceRepository;
        this.memberFeignClient = memberFeignClient;
        this.entityManager = entityManager;
    }

    /**
     * 기준일까지 발생해야 할 월차 개월 수와 DB상 누적 부여량을 비교하여,
     * 부족분만큼 기존 행을 UPDATE하거나 신규 행을 INSERT
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnnualLeaveGrantService.BatchResult grantForCompanyMonthly(LeavePolicy policy, LocalDate baseDate) {

        UUID companyId = policy.getCompanyId();
        AnnualLeaveGrantService.BatchResult batchResult = new AnnualLeaveGrantService.BatchResult();

        // 회사 소속 직원 전체 조회
        List<MemberResDto> members = fetchMembers(companyId);

        // 기존 MONTHLY(월차) 잔여 조회
        Map<UUID, MemberBalance> existingMap = memberBalanceRepository
                .findMonthlyBalancesByCompany(companyId)
                .stream()
                .collect(Collectors.toMap(
                        MemberBalance::getMemberId,
                        Function.identity(),
                        (a, b) -> a
                ));

        // 직원별 부여 판정
        List<MemberBalance> insertBuffer = new ArrayList<>(BATCH_SIZE);

        // 입사일 없거나 1년 이상 → 월차 대상 아님
        for (MemberResDto member : members) {
            LocalDate joinDate = member.getJoinDate();

            if (joinDate == null || !baseDate.isBefore(joinDate.plusYears(1))) {
                batchResult.skipCount++;
                continue;
            }

            int expectedMonths = calculateExpectedMonthlyLeaves(joinDate, baseDate);
            if (expectedMonths <= 0) {
                batchResult.skipCount++;
                continue;
            }

            MemberBalance existing = existingMap.get(member.getMemberId());

            if (existing != null) {
                // 기존 행 있으면 UPDATE
                double diff = expectedMonths - existing.getTotalGranted();
                if (diff > 0) {
                    existing.addGranted(diff);   // dirty checking → 커밋 시 UPDATE
                    batchResult.successCount++;
                } else {
                    batchResult.skipCount++;
                }
            } else {
                // 기존 행 없면 INSERT
                LocalDate expirationDate = joinDate.plusYears(1).minusDays(1);
                insertBuffer.add(buildMonthlyBalance(
                        companyId, member.getMemberId(),
                        (double) expectedMonths, expirationDate));
                batchResult.successCount++;
            }

            if (insertBuffer.size() >= BATCH_SIZE) {
                flushBuffer(insertBuffer);
            }
        }

        // 남은 INSERT 버퍼 처리
        if (!insertBuffer.isEmpty()) {
            flushBuffer(insertBuffer);
        }

        // UPDATE 대상은 dirty checking으로 트랜잭션 커밋 시 자동 flush
        return batchResult;
    }

    /**
     * 입사 후 baseDate 시점까지 경과한 월차 발생 개월 수(최대 11)를 계산
     */
    private int calculateExpectedMonthlyLeaves(LocalDate joinDate, LocalDate baseDate) {
        int monthsPassed = 0;
        for (int i = 1; i <= 11; i++) {
            if (!baseDate.isBefore(joinDate.plusMonths(i))) {
                monthsPassed = i;
            } else {
                break;
            }
        }
        return monthsPassed;
    }

    /** 버퍼를 일괄 저장한 뒤 영속성 컨텍스트를 비움 */
    private void flushBuffer(List<MemberBalance> buffer) {
        memberBalanceRepository.saveAll(buffer);
        memberBalanceRepository.flush();
        entityManager.clear();
        buffer.clear();
    }

    /** Feign으로 회사 소속 직원을 조회 */
    private List<MemberResDto> fetchMembers(UUID companyId) {
        ApiResponse<List<MemberResDto>> response = memberFeignClient.getMembersByCompany(companyId);
        if (response == null || response.getData() == null) {
            return Collections.emptyList();
        }
        return response.getData();
    }

    /** 월차 분은 MONTHLY에 저장 */
    private MemberBalance buildMonthlyBalance(UUID companyId, UUID memberId, Double days, LocalDate expirationDate) {
        return MemberBalance.builder()
                .memberId(memberId)
                .companyId(companyId)
                .balanceType(BalanceType.MONTHLY)
                .totalGranted(days)
                .totalUsed(0.0)
                .remaining(days)
                .expirationDate(expirationDate)
                .isUsableYn("Y")
                .isExpireYn("N")
                .delYn("N")
                .build();
    }
}
