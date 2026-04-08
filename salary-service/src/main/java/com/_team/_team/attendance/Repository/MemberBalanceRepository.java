package com._team._team.attendance.Repository;

import com._team._team.attendance.domain.MemberBalance;
import com._team._team.attendance.domain.enums.BalanceType;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 휴가 잔여 Repository
 * - 동시 차감 방지를 위한 비관적 락 메서드 포함
 * [동시성 시나리오]
 * 1. 직원 A가 연차 1일 신청 → 승인 처리 중 (remaining: 5.0)
 * 2. 같은 순간 직원 A의 다른 연차 1일도 승인 처리
 * 3. 락 없으면: 둘 다 remaining=5.0 읽음 → 둘 다 4.0으로 갱신 → 1일 증발
 * 4. 락 있으면: 첫 번째가 5.0→4.0, 두 번째가 4.0→3.0 순차 처리
 */

@Repository
public interface MemberBalanceRepository extends JpaRepository<MemberBalance, UUID> {

    /** 직원의 전체 휴가 잔여 조회 (마이페이지용) */
    List<MemberBalance> findByMemberIdAndCompanyIdAndDelYn(
            UUID memberId, UUID companyId, String delYn);

    /**
     * 차감용 조회 (비관적 락)
     * - 트랜잭션 끝날 때까지 다른 트랜잭션 대기
     * (조회하는 순간 행을 잠금. 다른 트랜잭션은 읽지 못하고 대기.)
     * 연차는 숫자 차감, 재시도로 실패할 수 있는 낙관적 락보다, 잠깐 대기하더라도 반드시 순차 처리되는 비관적 락이 맞음.
     * 실제로 연차 차감 동시 요청이 빈번하진 않으니 대기 시간도 거의 없는 것으로 예상
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT mb FROM MemberBalance mb " +
            "WHERE mb.companyId = :companyId " +
            "AND mb.memberId = :memberId " +
            "AND mb.balanceType = :type " +
            "AND mb.isUsableYn = 'Y' " +
            "AND mb.isExpireYn = 'N' " +
            "AND mb.delYn = 'N'")
    Optional<MemberBalance> findWithLock(
            @Param("companyId") UUID companyId,
            @Param("memberId") UUID memberId,
            @Param("type") BalanceType type);
}

