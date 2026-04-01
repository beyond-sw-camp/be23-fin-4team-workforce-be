package com._team._team.salary.Service;

import com._team._team.dto.BusinessException;
import com._team._team.salary.Repository.SalaryPolicyRepository;
import com._team._team.salary.domain.SalaryPolicy;
import com._team._team.salary.dto.reqdto.SalaryPolicyCreateReqDto;
import com._team._team.salary.dto.reqdto.SalaryPolicyUpdateReqDto;
import com._team._team.salary.dto.resdto.SalaryPolicyResDto;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Service
@Transactional
public class SalaryService {

    private final SalaryPolicyRepository salaryPolicyRepository;

    public SalaryService(SalaryPolicyRepository salaryPolicyRepository) {
        this.salaryPolicyRepository = salaryPolicyRepository;
    }

    // 급여 정책 생성
    public SalaryPolicyResDto save(UUID companyId, SalaryPolicyCreateReqDto reqDto) {
        validateDateRange(reqDto.getEffectiveFrom(), reqDto.getEffectiveTo());
        validateDuplicateCurrentPolicy(companyId, reqDto.getPolicyName(), reqDto.getEffectiveTo());

        SalaryPolicy salaryPolicy = reqDto.toEntity(companyId);
        SalaryPolicy saved = salaryPolicyRepository.save(salaryPolicy);
        return SalaryPolicyResDto.fromEntity(saved);
    }

    // 급여 정책 단건 조회
    @Transactional(readOnly = true)
    public SalaryPolicyResDto findById(UUID salaryPolicyId) {
        SalaryPolicy salaryPolicy = salaryPolicyRepository.findById(salaryPolicyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 정책을 찾을 수 없습니다."));
        return SalaryPolicyResDto.fromEntity(salaryPolicy);
    }

    // 회사별 급여 정책 목록 조회
    @Transactional(readOnly = true)
    public List<SalaryPolicyResDto> findByCompanyId(UUID companyId) {
        return salaryPolicyRepository.findByCompanyIdAndIsDeletedFalse(companyId).stream()
                .map(SalaryPolicyResDto::fromEntity)
                .toList();
    }

    // 급여 정책 수정
    public SalaryPolicyResDto update(UUID salaryPolicyId, SalaryPolicyUpdateReqDto reqDto) {
        SalaryPolicy salaryPolicy = salaryPolicyRepository.findById(salaryPolicyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 정책을 찾을 수 없습니다."));

        // 정산 완료된 과거 정책은 수정 불가
        if (salaryPolicy.isPast()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "정산이 완료된 과거 정책은 수정할 수 없습니다.");
        }

        // 현재 적용 중인 정책은 수정 불가
        if (salaryPolicy.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "현재 급여 정산이 진행 중인 정책은 수정할 수 없습니다.");
        }

        validateDateRange(reqDto.getEffectiveFrom(), reqDto.getEffectiveTo());

        salaryPolicy.update(reqDto);
        return SalaryPolicyResDto.fromEntity(salaryPolicy);
    }

    // 급여 정책 삭제 (soft delete)
    public void delete(UUID salaryPolicyId) {
        SalaryPolicy salaryPolicy = salaryPolicyRepository.findById(salaryPolicyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 정책을 찾을 수 없습니다."));

        // 현재 적용 중인 정책은 삭제 불가
        if (salaryPolicy.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "현재 적용 중인 정책은 삭제할 수 없습니다.");
        }

        // 정산 완료된 과거 정책은 삭제 불가
        if (salaryPolicy.isPast()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "정산이 완료된 과거 정책은 삭제할 수 없습니다.");
        }

        // 미래 정책만 soft delete 처리
        salaryPolicy.softDelete();
    }

    // 적용 종료일이 시작일보다 빠른지 검증
    private void validateDateRange(LocalDate effectiveFrom, LocalDate effectiveTo) {
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "적용 종료일은 적용 시작일보다 빠를 수 없습니다.");
        }
    }

    // 동일 이름의 현재 급여 정책 중복 검증
    private void validateDuplicateCurrentPolicy(UUID companyId, String policyName, LocalDate effectiveTo) {
        if (effectiveTo == null &&
                salaryPolicyRepository.existsByCompanyIdAndPolicyNameAndEffectiveToIsNull(companyId, policyName)) {
            throw new BusinessException(HttpStatus.CONFLICT, "동일한 이름의 현재 급여 정책이 이미 존재합니다.");
        }
    }
}
