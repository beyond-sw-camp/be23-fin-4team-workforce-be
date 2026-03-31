package com._team._team.salary.Repository;

import com._team._team.salary.domain.SalaryPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SalaryPolicyRepository extends JpaRepository<SalaryPolicy, UUID> {
    List<SalaryPolicy> findByCompanyId(UUID companyId);
    List<SalaryPolicy> findByCompanyIdAndIsDeletedFalse(UUID companyId);
    // 동일 이름 급여정책 존재여부 확인
    boolean existsByCompanyIdAndPolicyNameAndEffectiveToIsNull(UUID companyId, String policyName);
}
