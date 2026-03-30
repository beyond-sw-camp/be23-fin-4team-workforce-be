package com._team._team.organization.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com._team._team.company.domain.Company;
import com._team._team.organization.domain.JobGrade;
import com._team._team.organization.domain.JobTitle;
import com._team._team.organization.domain.Organization;
import com._team._team.organization.repository.JobGradeRepository;
import com._team._team.organization.repository.JobTitleRepository;
import com._team._team.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final JobGradeRepository jobGradeRepository;
    private final JobTitleRepository jobTitleRepository;

    @Autowired
    public OrganizationService(OrganizationRepository organizationRepository, JobGradeRepository jobGradeRepository, JobTitleRepository jobTitleRepository) {
        this.organizationRepository = organizationRepository;
        this.jobGradeRepository = jobGradeRepository;
        this.jobTitleRepository = jobTitleRepository;
    }

    // 온보딩 시 기본 조직 생성
    public Organization createDefaultOrganization(Company company) {
        Organization organization = Organization.builder()
                .company(company)
                .name(company.getCompanyName())
                .displayOrder(0)
                .build();
        return organizationRepository.save(organization);
    }

    // 온보딩 시 기본 직급 생성
    public JobGrade createDefaultJobGrade(Company company) {
        JobGrade jobGrade = JobGrade.builder()
                .company(company)
                .name("미지정")
                .displayOrder(0)
                .build();
        return jobGradeRepository.save(jobGrade);
    }

    // 온보딩 시 기본 직책 생성
    public JobTitle createDefaultJobTitle(Company company) {
        JobTitle jobTitle = JobTitle.builder()
                .company(company)
                .name("미지정")
                .displayOrder(0)
                .build();
        return jobTitleRepository.save(jobTitle);
    }
}