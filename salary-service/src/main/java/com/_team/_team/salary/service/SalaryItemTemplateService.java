package com._team._team.salary.service;

import com._team._team.dto.BusinessException;
import com._team._team.salary.repository.SalaryItemTemplateRepository;
import com._team._team.salary.domain.SalaryItemTemplate;
import com._team._team.salary.dto.reqdto.SalaryItemTemplateCreateReqDto;
import com._team._team.salary.dto.reqdto.SalaryItemTemplateUpdateReqDto;
import com._team._team.salary.dto.resdto.SalaryItemTemplateResDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 급여 항목 템플릿 서비스
 * - 급여 항목 CRUD 비즈니스 로직 처리
 * - 생성자 주입 방식 사용
 * - 읽기 전용 트랜잭션으로 더티체킹 스킵하여 성능 최적화
 */
@Service
@Transactional
public class SalaryItemTemplateService {

    private final SalaryItemTemplateRepository salaryItemTemplateRepository;

    /** 생성자 주입 */
    @Autowired
    public SalaryItemTemplateService(SalaryItemTemplateRepository salaryItemTemplateRepository) {
        this.salaryItemTemplateRepository = salaryItemTemplateRepository;
    }

    /** 급여 항목 템플릿 생성 */
    public SalaryItemTemplateResDto save(UUID companyId, SalaryItemTemplateCreateReqDto reqDto) {
        validateDuplicateItemName(companyId, reqDto.getItemName());

        SalaryItemTemplate template = reqDto.toEntity(companyId);
        SalaryItemTemplate saved = salaryItemTemplateRepository.save(template);
        return SalaryItemTemplateResDto.fromEntity(saved);
    }

    /** 급여 항목 템플릿 단건 조회 */
    @Transactional(readOnly = true)
    public SalaryItemTemplateResDto findById(UUID companyId, UUID salaryItemTemplateId) {
        SalaryItemTemplate template = salaryItemTemplateRepository.findById(salaryItemTemplateId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 항목 템플릿을 찾을 수 없습니다."));

        if (!template.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        return SalaryItemTemplateResDto.fromEntity(template);
    }

    /** 회사별 급여 항목 템플릿 목록 조회 */
    @Transactional(readOnly = true)
    public List<SalaryItemTemplateResDto> findByCompanyId(UUID companyId) {
        return salaryItemTemplateRepository.findByCompanyIdAndDelYn(companyId, "N").stream()
                .map(SalaryItemTemplateResDto::fromEntity)
                .toList();
    }

    /** 급여 항목 템플릿 수정 (더티체킹) */
    public SalaryItemTemplateResDto update(UUID companyId, UUID salaryItemTemplateId, SalaryItemTemplateUpdateReqDto reqDto) {
        SalaryItemTemplate template = salaryItemTemplateRepository.findById(salaryItemTemplateId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 항목 템플릿을 찾을 수 없습니다."));

        if (!template.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        template.update(reqDto);
        return SalaryItemTemplateResDto.fromEntity(template);
    }

    /** 급여 항목 템플릿 삭제 (soft delete, 더티체킹) */
    public void delete(UUID companyId, UUID salaryItemTemplateId) {
        SalaryItemTemplate template = salaryItemTemplateRepository.findById(salaryItemTemplateId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "급여 항목 템플릿을 찾을 수 없습니다."));

        if (!template.getCompanyId().equals(companyId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        template.softDelete();
    }

    /** 동일 이름 항목 중복 검증 */
    private void validateDuplicateItemName(UUID companyId, String itemName) {
        if (salaryItemTemplateRepository.existsByCompanyIdAndItemNameAndDelYn(companyId, itemName, "N")) {
            throw new BusinessException(HttpStatus.CONFLICT, "동일한 이름의 급여 항목 템플릿이 이미 존재합니다.");
        }
    }
}
