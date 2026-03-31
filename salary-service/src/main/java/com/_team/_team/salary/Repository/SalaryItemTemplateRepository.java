package com._team._team.salary.Repository;

import com._team._team.salary.domain.SalaryItemTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SalaryItemTemplateRepository extends JpaRepository<SalaryItemTemplate, UUID> {

    /** 회사별 활성 급여 항목 템플릿 목록 조회 (soft delete 제외) */
    List<SalaryItemTemplate> findByCompanyIdAndIsDeletedFalse(UUID companyId);

    /** 동일 회사 내 같은 이름의 활성 항목 존재 여부 확인 (중복 검증용) */
    boolean existsByCompanyIdAndItemNameAndIsDeletedFalse(UUID companyId, String itemName);
}
