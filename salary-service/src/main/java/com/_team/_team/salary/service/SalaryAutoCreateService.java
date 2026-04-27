package com._team._team.salary.service;

import com._team._team.event.MemberHiredEvent;
import com._team._team.salary.domain.PayGradeTable;
import com._team._team.salary.repository.PayGradeTableRepository;
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
    private final PayGradeTableRepository payGradeTableRepository;

    @Autowired
    public SalaryAutoCreateService(SalaryRepository salaryRepository,
                                   SalaryPolicyRepository salaryPolicyRepository,
                                   SalaryAutoCreateProperties autoCreateProps, PayGradeTableRepository payGradeTableRepository) {
        this.salaryRepository = salaryRepository;
        this.salaryPolicyRepository = salaryPolicyRepository;
        this.autoCreateProps = autoCreateProps;
        this.payGradeTableRepository = payGradeTableRepository;
    }

    // 입사 이벤트 -> Salary INSERT
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

        // 3. 기본급 결정 (호봉제와 연봉협상제 분기)
        long baseSalary;
        Integer step = null;

        if ("Y".equals(policy.getUsePayGradeYn())) {
            // 호봉제, 이벤트에 step 지정되면 그 호봉으로, 없으면 1호봉 기본 배정
            step = e.getStep() != null ? e.getStep() : 1;
            PayGradeTable pg = payGradeTableRepository
                    .findActiveOn(e.getCompanyId(), step, e.getHireDate())
                    .orElse(null);
            if (pg == null) {
                log.warn("[SALARY-BOOTSTRAP] 호봉제지만 {}호봉 호봉표 없음 companyId={} → 스킵",
                        step, e.getCompanyId());
                return;
            }
            baseSalary = pg.getBaseSalary();
        } else {
            // 연봉제, 기존 로직 유지
            baseSalary = e.getBaseSalary() != null
                    ? e.getBaseSalary()
                    : autoCreateProps.getDefaultBaseSalary();
        }

        // 4. Salary 생성
        Salary salary = Salary.builder()
                .memberId(e.getMemberId())
                .companyId(e.getCompanyId())
                .salaryPolicyId(policy.getSalaryPolicyId())
                .baseSalary(baseSalary)
                .step(step)
                .jobGradeName(e.getJobGradeName())
                .jobTitleName(e.getJobTitleName())
                .effectiveFrom(e.getHireDate())
                .effectiveTo(null)
                .dependentCount(1)
                .build();
        Salary saved = salaryRepository.save(salary);


        log.info("[SALARY-BOOTSTRAP] 생성 완료 salaryId={} memberId={} step={} base={} policy={}",
                saved.getSalaryId(), e.getMemberId(), step, baseSalary, policy.getSalaryPolicyId());
    }
}
