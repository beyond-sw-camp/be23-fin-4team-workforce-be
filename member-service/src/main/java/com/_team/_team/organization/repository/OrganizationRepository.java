package com._team._team.organization.repository;

import com._team._team.organization.domain.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    List<Organization> findByCompany_CompanyIdAndDelYn(UUID companyId, String delYn);

}
