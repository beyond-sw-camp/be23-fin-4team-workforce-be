package com._team._team.organization.service;

import com._team._team.dto.BusinessException;
import com._team._team.member.domain.Member;
import com._team._team.member.repository.MemberRepository;
import com._team._team.member.service.MemberService;
import com._team._team.organization.dto.reqdto.OrganizationReqDto;
import com._team._team.organization.dto.resdto.OrganizationResDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final JobGradeRepository jobGradeRepository;
    private final JobTitleRepository jobTitleRepository;
    private final MemberRepository memberRepository;

    @Autowired
    public OrganizationService(OrganizationRepository organizationRepository, JobGradeRepository jobGradeRepository, JobTitleRepository jobTitleRepository, MemberRepository memberRepository) {
        this.organizationRepository = organizationRepository;
        this.jobGradeRepository = jobGradeRepository;
        this.jobTitleRepository = jobTitleRepository;
        this.memberRepository = memberRepository;
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

    // 조직 생성
    public UUID createOrganization(UUID memberId, OrganizationReqDto reqDto) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."
                ));
        Company company = member.getCompany();

        Organization parent = null;
        if (reqDto.getParentId() != null) {
            parent = organizationRepository.findById(reqDto.getParentId())
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND, "존재하지 않는 상위 조직입니다."
                    ));
            if (!parent.getCompany().getCompanyId().equals(company.getCompanyId())) {
                throw new BusinessException(
                        HttpStatus.FORBIDDEN, "다른 회사의 조직입니다."
                );
            }
        }

        int displayOrder = organizationRepository
                .findByCompanyAndDelYnOrderByDisplayOrder(company, "NO").size();

        Organization organization = Organization.builder()
                .company(company)
                .name(reqDto.getName())
                .parent(parent)
                .displayOrder(displayOrder)
                .build();

        return organizationRepository.save(organization).getOrganizationId();
    }

    // 조직 목록 조회 (트리 구조)
    @Transactional(readOnly = true)
    public List<OrganizationResDto> getOrganizationList(UUID memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."
                ));

        return organizationRepository
                .findByCompanyAndDelYnOrderByDisplayOrder(member.getCompany(), "NO")
                .stream()
                .filter(o -> o.getParent() == null)
                .map(OrganizationResDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 조직 수정
    public void updateOrganization(UUID memberId, UUID organizationId,
                                   OrganizationReqDto reqDto) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."
                ));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 조직입니다."
                ));

        if (!organization.getCompany().getCompanyId()
                .equals(member.getCompany().getCompanyId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 조직입니다.");
        }

        organization.updateName(reqDto.getName());
    }

    // 조직 삭제
    public void deleteOrganization(UUID memberId, UUID organizationId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."
                ));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 조직입니다."
                ));

        if (!organization.getCompany().getCompanyId()
                .equals(member.getCompany().getCompanyId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "다른 회사의 조직입니다.");
        }

        if (!organization.getChildren().isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "하위 조직이 있어 삭제할 수 없습니다."
            );
        }

        organization.delete();
    }

}