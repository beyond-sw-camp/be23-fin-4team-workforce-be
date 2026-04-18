package com._team._team.salary.service;

import com._team._team.event.MemberHiredEvent;
import com._team._team.salary.repository.SalaryPolicyRepository;
import com._team._team.salary.repository.SalaryRepository;
import com._team._team.salary.config.SalaryAutoCreateProperties;
import com._team._team.salary.domain.Salary;
import com._team._team.salary.domain.SalaryPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 신규 입사자 Salary 자동 생성
@Slf4j
@Service
public class SalaryAutoCreateService {

    private final SalaryRepository salaryRepository;
    private final SalaryPolicyRepository salaryPolicyRepository;
    private final SalaryAutoCreateProperties autoCreateProps;

    @Autowired
    public SalaryAutoCreateService(SalaryRepository salaryRepository,
                                   SalaryPolicyRepository salaryPolicyRepository,
                                   SalaryAutoCreateProperties autoCreateProps) {
        this.salaryRepository = salaryRepository;
        this.salaryPolicyRepository = salaryPolicyRepository;
        this.autoCreateProps = autoCreateProps;
    }

    // 입사 이벤트 -> Salary INSERT
    // 멱등성: 같은 날짜 기준 Active Salary 이미 있으면 스킵
    @Transactional
    public void bootstrapSalary(MemberHiredEvent e) {

        // 1. 중복 방지 재시도/재기동 대비
        if (salaryRepository.findActiveSalary(e.getMemberId(), e.getCompanyId(), e.getHireDate()).isPresent()) {
            log.warn("[SALARY-BOOTSTRAP] 이미 Active Salary 존재 memberId={} date={}",
                    e.getMemberId(), e.getHireDate());
            return;
        }

        // 2. 활성 정책 조회 가장 최신 1건
        SalaryPolicy policy = salaryPolicyRepository
                .findActivePolicies(e.getCompanyId(), e.getHireDate())
                .stream().findFirst().orElse(null);
        if (policy == null) {
            log.warn("[SALARY-BOOTSTRAP] 활성 급여 정책 없음 companyId={} memberId={} → 생성 스킵",
                    e.getCompanyId(), e.getMemberId());
            return;
        }

        // 3. 기본급 결정 HR이 입력한 값 우선 없으면 yml default로 폴백
        long baseSalary = e.getBaseSalary() != null
                ? e.getBaseSalary()
                : autoCreateProps.getDefaultBaseSalary();

        // 4. Salary 생성
        Salary salary = Salary.builder()
                .memberId(e.getMemberId())
                .companyId(e.getCompanyId())
                .salaryPolicyId(policy.getSalaryPolicyId())
                .baseSalary(baseSalary)
                .jobGradeName(e.getJobGradeName())
                .jobTitleName(e.getJobTitleName())
                .effectiveFrom(e.getHireDate())
                .effectiveTo(null)
                .build();
        Salary saved = salaryRepository.save(salary);

        log.info("[SALARY-BOOTSTRAP] 생성 완료 salaryId={} memberId={} base={} policy={}",
                saved.getSalaryId(), e.getMemberId(), baseSalary, policy.getSalaryPolicyId());
    }
}
