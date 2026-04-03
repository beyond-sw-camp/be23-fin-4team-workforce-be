package com._team._team.salary.Service;

import com._team._team.dto.BusinessException;
import com._team._team.salary.Repository.SalaryPolicyRepository;
import com._team._team.salary.Repository.SalaryRepository;
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

    @Autowired
    public SalaryService(SalaryRepository salaryRepository,
                         SalaryPolicyRepository salaryPolicyRepository){
        this.salaryRepository = salaryRepository;
        this.salaryPolicyRepository = salaryPolicyRepository;
    }

    // 급여 생성
    public SalaryResDto save(UUID companyId, SalaryCreateReqDto reqDto){
        // 급여 정책 존재 여부 확인
        if(!salaryPolicyRepository.existsById(reqDto.getSalaryPolicyId())){
            throw new BusinessException(HttpStatus.NOT_FOUND,"급여 정책을 찾을 수 없습니다.");
        }

        // 적용 기간 검증
        validateDateRange(reqDto.getEffectiveFrom(), reqDto.getEffectiveTo());

        Salary salary = reqDto.toEntity(companyId);
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

        salary.update(reqDto);
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
}
