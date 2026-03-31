package com._team._team.salary.Repository;

import com._team._team.salary.domain.SalaryItemTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SalaryItemTemplateRepository extends JpaRepository<SalaryItemTemplate, UUID> {

    /** 회사별 사용 중인 급여 항목 템플릿 목록 조회 */
    List<SalaryItemTemplate> findByCompanyIdAndUseYn(UUID companyId, String useYn);

    /** 동일 회사 내 같은 이름의 사용 중 항목 존재 여부 확인 (중복 검증용) */
    boolean existsByCompanyIdAndItemNameAndUseYn(UUID companyId, String itemName, String useYn);
}
