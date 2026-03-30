package com._team._team.organization.repository;

import com._team._team.organization.domain.JobGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobGradeRepository extends JpaRepository<JobGrade, UUID> {
    List<JobGrade> findByCompany_CompanyIdAndDelYnOrderByDisplayOrder(
            UUID companyId, String delYn);
}
