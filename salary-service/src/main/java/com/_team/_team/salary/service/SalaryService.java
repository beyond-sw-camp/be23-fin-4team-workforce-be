package com._team._team.salary.service;

import com._team._team.dto.BusinessException;
import com._team._team.salary.domain.PayGradeTable;
import com._team._team.salary.domain.SalaryPolicy;
import com._team._team.salary.repository.PayGradeTableRepository;
import com._team._team.salary.repository.SalaryPolicyRepository;
import com._team._team.salary.repository.SalaryRepository;
import com._team._team.salary.domain.Salary;
import com._team._team.salary.dto.reqdto.SalaryCreateReqDto;
import com._team._team.salary.dto.reqdto.SalaryUpdateReqDto;
import com._team._team.salary.dto.resdto.SalaryResDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SalaryService {

    private final SalaryRepository salaryRepository;
    private final SalaryPolicyRepository salaryPolicyRepository;
    private final PayGradeTableRepository payGradeTableRepository;

    @Autowired
    public SalaryService(SalaryRepository salaryRepository,
                         SalaryPolicyRepository salaryPolicyRepository, PayGradeTableRepository payGradeTableRepository){
        this.salaryRepository = salaryRepository;
        this.salaryPolicyRepository = salaryPolicyRepository;
        this.payGradeTableRepository = payGradeTableRepository;
    }

    // 급여 생성
    public SalaryResDto createSalary(UUID companyId, SalaryCreateReqDto reqDto) {
        // 1. 적용일 기준 회사 활성 정책 조회
        SalaryPolicy policy = salaryPolicyRepository
                .findActivePolicies(companyId, reqDto.getEffectiveFrom())
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "활성 급여 정책이 없습니다."));

        // 적용 기간 검증
        validateDateRange(reqDto.getEffectiveFrom(), reqDto.getEffectiveTo());

        // 이전 급여가 있었을 경우, 이전 급여의 effectiveTo를 자동으로 닫아줌
        // 새 급여 시작일 전날로 이전 급여 종료
        salaryRepository.findActiveSalary(reqDto.getMemberId(), companyId, reqDto.getEffectiveFrom())
                .ifPresent(prevSalary -> {
                    prevSalary.closeEffectivePeriod(reqDto.getEffectiveFrom().minusDays(1));
                });

        // 호봉제 여부에 따라 step 필수 검증
        if ("Y".equals(policy.getUsePayGradeYn()) && reqDto.getStep() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "호봉제 정책입니다. 호봉을 지정하세요.");
        }

        long resolvedBaseSalary = resolveBaseSalary(
                companyId, reqDto.getStep(), reqDto.getBaseSalary(), reqDto.getEffectiveFrom());

        Salary salary = Salary.builder()
                .memberId(reqDto.getMemberId())
                .companyId(companyId)
                .salaryPolicyId(policy.getSalaryPolicyId())
                .baseSalary(resolvedBaseSalary)
                .step(reqDto.getStep())
                .jobGradeName(reqDto.getJobGradeName())
                .jobTitleName(reqDto.getJobTitleName())
                .effectiveFrom(reqDto.getEffectiveFrom())
                .effectiveTo(reqDto.getEffectiveTo())
                .build();

        Salary saved = salaryRepository.save(salary);
        return SalaryResDto.fromEntity(saved);
    }

    // 급여 단건 조회
    @Transactional(readOnly = true)
    public SalaryResDto findById(UUID companyId, UUID salaryId){
        Salary salary = salaryRepository.findById(salaryId)
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND,"급여 정보를 찾을 수 없습니다."));

        if (!salary.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        return SalaryResDto.fromEntity(salary);
    }

    // 회사별 급여 목록 조회
    @Transactional(readOnly = true)
    public List<SalaryResDto> findByCompanyId(UUID companyId){
        return salaryRepository.findByCompanyId(companyId).stream()
                .map(SalaryResDto::fromEntity).toList();
    }

    // 직원별 급여 이력 조회
    @Transactional(readOnly = true)
    public List<SalaryResDto> findByMemberId(UUID companyId, UUID memberId){
        return salaryRepository.findByMemberIdAndCompanyId(memberId, companyId).stream()
                .map(SalaryResDto::fromEntity).toList();
    }

    // 급여 수정
    public SalaryResDto update(UUID companyId, UUID salaryId, SalaryUpdateReqDto reqDto){
        Salary salary = salaryRepository.findById(salaryId)
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "급여 정보를 찾을수 없습니다."));

        if (!salary.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        // 급여 정책 존재 여부 확인
        if(!salaryPolicyRepository.existsById(reqDto.getSalaryPolicyId())){
            throw new BusinessException(HttpStatus.NOT_FOUND, "급여 정책을 찾을 수 없습니다.");
        }

        // 적용 기간 검증
        validateDateRange(reqDto.getEffectiveFrom(), reqDto.getEffectiveTo());

        SalaryPolicy policy = salaryPolicyRepository.findById(reqDto.getSalaryPolicyId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 정책을 찾을 수 없습니다."));

        if ("Y".equals(policy.getUsePayGradeYn()) && reqDto.getStep() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "호봉제 정책입니다. 호봉을 지정하세요.");
        }

        long resolvedBaseSalary = resolveBaseSalary(
                companyId, reqDto.getStep(), reqDto.getBaseSalary(), reqDto.getEffectiveFrom());

        salary.update(reqDto, resolvedBaseSalary);
        return SalaryResDto.fromEntity(salary);
    }

    // 급여 삭제 (현재 적용 중인 급여는 삭제 불가)
    public void delete(UUID companyId, UUID salaryId){
        Salary salary = salaryRepository.findById(salaryId)
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "급여 정보를 찾을 수 없습니다."));

        if (!salary.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        if(salary.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,"현재 적용 중인 급여는 삭제 불가합니다.");
        }

        salaryRepository.delete(salary);
    }

    // 적용 종료일이 시작일보다 빠른지 검증
    private void validateDateRange(LocalDate effectiveFrom, LocalDate effectiveTo){
        if(effectiveTo != null && effectiveTo.isBefore(effectiveFrom)){
            throw new BusinessException(HttpStatus.BAD_REQUEST, "적용 종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    /**
     * 호봉이 지정되면 호봉표에서 baseSalary 조회, 없으면 reqDto 값 사용
     */
    private long resolveBaseSalary(UUID companyId, Integer step, Long fallbackBase,
                                   LocalDate effectiveFrom) {
        if (step != null) {
            PayGradeTable pg = payGradeTableRepository
                    .findActiveOn(companyId, step, effectiveFrom)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                            step + "호봉이 " + effectiveFrom + " 기준 호봉표에 없습니다. "
                                    + "호봉표 먼저 등록하세요."));
            return pg.getBaseSalary();
        }
        if (fallbackBase == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "호봉을 지정하지 않으면 기본급을 직접 입력해야 합니다.");
        }
        return fallbackBase;
    }
}
