package com._team._team.salary.Repository;

import com._team._team.salary.domain.PayrollItem;
import com._team._team.salary.domain.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PayrollItemRepository extends JpaRepository<PayrollItem, UUID> {

    /** 급여대장별 항목 목록 (삭제 안 된 것만, 표시 순서대로) */
    List<PayrollItem> findByPayroll_PayrollIdAndDelYnOrderByDisplayOrder(UUID payrollId, String delYn);

    /**
     * 급여대장의 항목 유형별 금액 합계 (DB 레벨 SUM)
     * - Java에서 전체 항목을 가져와서 stream 합산하는 것보다 효율적
     * - 항목이 없으면 COALESCE로 0 반환
     */
    /** 같은 급여대장 내 동일 항목명 존재 여부 확인 (중복 방지) */
    boolean existsByPayroll_PayrollIdAndItemNameAndDelYn(UUID payrollId, String itemName, String delYn);

    @Query("SELECT COALESCE(SUM(pi.amount), 0) FROM PayrollItem pi " +
           "WHERE pi.payroll.payrollId = :payrollId " +
           "AND pi.delYn = 'N' " +
           "AND pi.itemType = :itemType")
    Long sumAmountByPayrollIdAndItemType(@Param("payrollId") UUID payrollId,
                                         @Param("itemType") ItemType itemType);
}
