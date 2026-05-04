package com._team._team.personnel.service;

import com._team._team.event.PersonnelOrderApprovedEvent;
import com._team._team.member.domain.Member;
import com._team._team.member.domain.MemberPosition;
import com._team._team.member.repository.MemberPositionRepository;
import com._team._team.member.repository.MemberRepository;
import com._team._team.organization.domain.JobGrade;
import com._team._team.organization.domain.JobTitle;
import com._team._team.organization.domain.Organization;
import com._team._team.organization.repository.JobGradeRepository;
import com._team._team.organization.repository.JobTitleRepository;
import com._team._team.organization.repository.OrganizationRepository;
import com._team._team.personnel.domain.PersonnelOrder;
import com._team._team.personnel.domain.enums.PersonnelOrderType;
import com._team._team.personnel.repository.PersonnelOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 인사발령 적용 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonnelOrderApplyService {

    private final PersonnelOrderRepository personnelOrderRepository;
    private final MemberRepository memberRepository;
    private final MemberPositionRepository memberPositionRepository;
    private final OrganizationRepository organizationRepository;
    private final JobGradeRepository jobGradeRepository;
    private final JobTitleRepository jobTitleRepository;

    @Transactional
    public void apply(PersonnelOrderApprovedEvent event) {
        if (event == null || event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("[PersonnelOrder] empty event - skipped");
            return;
        }
        LocalDate effectiveDate = event.getEffectiveDate() != null
                ? event.getEffectiveDate() : LocalDate.now();
        for (PersonnelOrderApprovedEvent.Item item : event.getItems()) {
            try {
                applyOne(event, item, effectiveDate);
            } catch (Exception e) {
                log.error("[PersonnelOrder] item apply failed - memberId={} - {}",
                        item.getMemberId(), e.getMessage(), e);
            }
        }
    }

    private void applyOne(PersonnelOrderApprovedEvent event,
                          PersonnelOrderApprovedEvent.Item item,
                          LocalDate effectiveDate) {
        UUID memberId = item.getMemberId();
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            log.warn("[PersonnelOrder] member not found - skipped. memberId={}", memberId);
            return;
        }
        UUID companyId = event.getCompanyId();

        // 1) 이력 INSERT - 이벤트의 before/after 값을 그대로 저장 (이벤트 발행자가 시뮬 결과 packing)
        PersonnelOrderType orderType = parseOrderType(item.getOrderType());
        PersonnelOrder order = PersonnelOrder.builder()
                .memberId(memberId)
                .companyId(companyId)
                .orderType(orderType)
                .effectiveDate(effectiveDate)
                .approvalDocumentId(event.getApprovalDocumentId())
                .beforeOrganizationId(item.getBeforeOrganizationId())
                .afterOrganizationId(item.getAfterOrganizationId())
                .beforeOrganizationName(item.getBeforeOrganizationName())
                .afterOrganizationName(item.getAfterOrganizationName())
                .beforeJobGradeName(item.getBeforeJobGradeName())
                .afterJobGradeName(item.getAfterJobGradeName())
                .beforeJobTitleName(item.getBeforeJobTitleName())
                .afterJobTitleName(item.getAfterJobTitleName())
                .reason(item.getReason())
                .approverId(event.getApproverId())
                .build();
        personnelOrderRepository.save(order);

        // 2) MemberPosition UPDATE - 현재 활성 포지션의 organization / jobGrade / jobTitle 변경
        MemberPosition mp = memberPositionRepository
                .findActivePositionsByMemberIds(java.util.Set.of(memberId))
                .stream().findFirst().orElse(null);
        if (mp == null) {
            log.warn("[PersonnelOrder] active member position not found - skipped. memberId={}", memberId);
            return;
        }

        Organization newOrg = item.getAfterOrganizationId() != null
                ? organizationRepository.findById(item.getAfterOrganizationId()).orElse(mp.getOrganization())
                : mp.getOrganization();
        JobGrade newGrade = item.getAfterJobGradeName() != null
                ? findJobGradeByName(companyId, item.getAfterJobGradeName()).orElse(mp.getJobGrade())
                : mp.getJobGrade();
        JobTitle newTitle = item.getAfterJobTitleName() != null
                ? findJobTitleByName(companyId, item.getAfterJobTitleName()).orElse(mp.getJobTitle())
                : mp.getJobTitle();

        mp.update(newOrg, newGrade, newTitle, mp.getRole());
        log.info("[PersonnelOrder] applied - memberId={} type={} orgAfter={} gradeAfter={} titleAfter={}",
                memberId, orderType,
                item.getAfterOrganizationName(),
                item.getAfterJobGradeName(),
                item.getAfterJobTitleName());
    }

    private PersonnelOrderType parseOrderType(String s) {
        if (s == null) return PersonnelOrderType.ROLE_CHANGE;
        try {
            return PersonnelOrderType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return PersonnelOrderType.ROLE_CHANGE;
        }
    }

    private Optional<JobGrade> findJobGradeByName(UUID companyId, String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        List<JobGrade> list = jobGradeRepository
                .findByCompany_CompanyIdAndDelYnOrderByDisplayOrder(companyId, "NO");
        return list.stream().filter(g -> name.equals(g.getName())).findFirst();
    }

    private Optional<JobTitle> findJobTitleByName(UUID companyId, String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        return jobTitleRepository
                .findByCompany_CompanyIdAndDelYnOrderByDisplayOrder(companyId, "NO")
                .stream()
                .filter(t -> name.equals(t.getName()))
                .findFirst();
    }
}
