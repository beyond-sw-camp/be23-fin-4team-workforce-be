package com._team._team.organization.repository;

import com._team._team.organization.domain.JobTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobTitleRepository extends JpaRepository<JobTitle, UUID> {
    List<JobTitle> findByCompany_CompanyIdAndDelYnOrderByDisplayOrder(
            UUID companyId, String delYn);
}
