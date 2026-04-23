package com._team._team.salary.domain;

import com._team._team.domain.BaseTimeEntity;

import java.time.LocalDate;
import java.util.UUID;

import com._team._team.salary.dto.reqdto.SalaryUpdateReqDto;
import lombok.*;
import jakarta.persistence.*;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
public class Salary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID salaryId;

    /** 직원 ID (member-service에서 관리) */
    @Column(nullable = false)
    private UUID memberId;

    /** 소속 회사 ID (Gateway 헤더에서 전달받음) */
    @Column(nullable = false)
    private UUID companyId;

    /** 적용 급여 정책 ID (salary_policy) */
    @Column(nullable = false)
    private UUID salaryPolicyId;

    /**
     * 호봉, (호봉제인 회사용)
     */
    private Integer step;

    /** 기본급 */
    @Column(nullable = false)
    private Long baseSalary;

    /** 직급명 (생성 시점 스냅샷 - 이력 추적용) */
    private String jobGradeName;

    /** 직책명 (생성 시점 스냅샷 - 이력 추적용) */
    private String jobTitleName;

    /** 적용 시작일 */
    @Column(nullable = false)
    private LocalDate effectiveFrom;

    /** 적용 종료일 (null이면 현재 적용 중) */
    private LocalDate effectiveTo;

    /**
     * 급여 정보 수정
     */
    public void update(SalaryUpdateReqDto reqDto, long resolvedBaseSalary){
        this.salaryPolicyId = reqDto.getSalaryPolicyId();
        this.baseSalary = resolvedBaseSalary;
        this.step = reqDto.getStep();
        this.jobGradeName = reqDto.getJobGradeName();
        this.jobTitleName = reqDto.getJobTitleName();
        this.effectiveFrom = reqDto.getEffectiveFrom();
        this.effectiveTo = reqDto.getEffectiveTo();
    }

    /**
     * 현재 적용 중인 급여인지 확인
     * - 시작일이 오늘 이전이고, 종료일이 업거나 오늘 이후면 활성 상태
     */
    public boolean isActive(){
        LocalDate today = LocalDate.now();
        boolean started = !effectiveFrom.isAfter(today);
        boolean notEnded = effectiveTo == null || !effectiveTo.isBefore(today);
        return started && notEnded;
    }

    /**
     * 적용 기간 종료 - 새 급여 생성 시 이전 급여를 자동 마감
     * 예: 새 급여 effectiveFrom이 03-01이면, 이전 급여 effectiveTo를 02-28로 설정
     */
    public void closeEffectivePeriod(LocalDate endDate) {
        this.effectiveTo = endDate;
    }
}
