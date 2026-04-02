package com._team._team.salary.Repository;

import com._team._team.salary.domain.Payroll;
import com._team._team.salary.domain.enums.PayrollStatus;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * 직원별 + 상태별 급여대장 조회 (회사 + 삭제되지 않은 것만)
     */
    List<Payroll> findByMemberIdAndPayrollStatusAndCompanyIdAndDelYn(UUID memberId, PayrollStatus payrollStatus, UUID companyId, String delYn);

    /**
     * 급여(Salary) ID로 급여대장 조회
     * - 특정 급여 정책에 연결된 급여대장들을 조회할 때 사용
     */
    List<Payroll> findBySalaryIdAndDelYn(UUID salaryId, String delYn);
}
