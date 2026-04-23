package com._team._team.attendance.repository;

import com._team._team.attendance.domain.CompanyIpWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyIpWhitelistRepository extends JpaRepository<CompanyIpWhitelist, UUID> {

    List<CompanyIpWhitelist> findByCompanyIdAndDelYnOrderByCreatedAtAsc(UUID companyId, String delYn);

    Optional<CompanyIpWhitelist> findByCompanyIpWhitelistIdAndCompanyIdAndDelYn(
            UUID companyIpWhitelistId, UUID companyId, String delYn);
}