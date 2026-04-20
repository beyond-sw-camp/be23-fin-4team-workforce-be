package com._team._team.salary.repository;

import com._team._team.salary.domain.Payroll;
import com._team._team.salary.domain.enums.PayrollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, UUID> {

    /**
     * 급여대장 단건 조회 (회사 + 삭제되지 않은 것만)
     * - companyId로 타사 접근 차단
     */
    Optional<Payroll> findByPayrollIdAndCompanyIdAndDelYn(UUID payrollId, UUID companyId, String delYn);

    /**
     * 직원별 급여대장 목록 조회 (회사 + 삭제되지 않은 것만, 최신순)
     */
    List<Payroll> findByMemberIdAndCompanyIdAndDelYnOrderByPayrollYearMonthDayDesc(UUID memberId, UUID companyId, String delYn);

    /**
     * 직원 + 정산일 중복 체크 (삭제되지 않은 것만)
     * - 같은 직원이 같은 정산일에 급여대장을 중복 생성하는 것을 방지
     */
    boolean existsByMemberIdAndPayrollYearMonthDayAndDelYn(UUID memberId, LocalDate payrollYearMonthDay, String delYn);


    /**
     * 직원 + 정산일 기존 대장 조회 (delYn 무관)
     * - Unique Constraint 검사용
     * - 존재하면 재생성 불가 (회계 원칙: 삭제 후 재생성 금지)
     */
    Optional<Payroll> findByCompanyIdAndMemberIdAndPayrollYearMonthDay(
            UUID companyId, UUID memberId, LocalDate payrollYearMonthDay);

    /**
     * 직원별 + 상태별 급여대장 조회 (회사 + 삭제되지 않은 것만)
     */
    List<Payroll> findByMemberIdAndPayrollStatusAndCompanyIdAndDelYn(UUID memberId, PayrollStatus payrollStatus, UUID companyId, String delYn);

    /**
     * 급여(Salary) ID로 급여대장 조회
     * - 특정 급여 정책에 연결된 급여대장들을 조회
     */
    List<Payroll> findBySalaryIdAndDelYn(UUID salaryId, String delYn);

    /**
     * 급여대장을 상태·삭제여부로 전부 조회
     */
    List<Payroll> findByPayrollStatusAndDelYn(PayrollStatus payrollStatus, String delYn);

    /**
     * [미사용수당 API 용]
     * 특정 연/월에 해당하는 DRAFT 급여대장 조회
     */
    @Query("SELECT p FROM Payroll p " +
            "WHERE p.companyId = :companyId " +
            "AND p.memberId = :memberId " +
            "AND p.payrollYearMonthDay BETWEEN :from AND :to " +
            "AND p.payrollStatus = :status " +
            "AND p.delYn = 'N'")
    Optional<Payroll> findDraftByCompanyMemberAndMonth(
            @Param("companyId") UUID companyId,
            @Param("memberId") UUID memberId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("status") PayrollStatus status);
}
