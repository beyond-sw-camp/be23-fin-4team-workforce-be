package com._team._team.goal.repository;

import com._team._team.goal.domain.KpiTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KpiTemplateRepository extends JpaRepository<KpiTemplate, UUID> {

    List<KpiTemplate> findByCompanyIdIsNullOrCompanyId(UUID companyId);

    List<KpiTemplate> findByCompanyIdIsNullOrCompanyIdAndIsActiveTrue(UUID companyId);
}
