package com._team._team.salary.service;

import com._team._team.dto.BusinessException;
import com._team._team.salary.domain.MemberAllowance;
import com._team._team.salary.domain.enums.AllowanceApprovalStatus;
import com._team._team.salary.repository.MemberAllowanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MemberAllowanceService {

    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    private final MemberAllowanceRepository repository;

    @Autowired
    public MemberAllowanceService(MemberAllowanceRepository repository) {
        this.repository = repository;
    }

    // 직원 입사 시 수당 선택하면 기본 수당 AUTO 등록, 결재 생략
    public MemberAllowance autoGrant(UUID memberId, UUID companyId,
                                     UUID templateId, Long amount,
                                     LocalDate effectiveFrom) {
        // 기존 활성 수당 close
        repository.findCurrentByTemplate(memberId, companyId, templateId, effectiveFrom)
                .ifPresent(prev -> prev.closeEffectivePeriod(effectiveFrom.minusDays(1)));

        LocalDateTime now = LocalDateTime.now();
        MemberAllowance entity = MemberAllowance.builder()
                .memberId(memberId)
                .companyId(companyId)
                .salaryItemTemplateId(templateId)
                .amount(amount)
                .effectiveFrom(effectiveFrom)
                .approvalStatus(AllowanceApprovalStatus.AUTO)
                .requestedBy(SYSTEM_ACTOR)
                .requestedAt(now)
                .reason("입사시 수당 등록")
                .build();

        return repository.save(entity);
    }

    // 본인 신청, 결재 대기 PENDING 생성
    public MemberAllowance requestChange(UUID memberId, UUID companyId,
                                         UUID templateId, Long amount,
                                         LocalDate effectiveFrom, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "변경 사유를 입력해주세요.");
        }

        LocalDateTime now = LocalDateTime.now();
        MemberAllowance entity = MemberAllowance.builder()
                .memberId(memberId)
                .companyId(companyId)
                .salaryItemTemplateId(templateId)
                .amount(amount)
                .effectiveFrom(effectiveFrom)
                .approvalStatus(AllowanceApprovalStatus.PENDING)
                .requestedBy(memberId)
                .requestedAt(now)
                .reason(reason)
                .build();

        return repository.save(entity);
    }

    // 결재 ID 연결, 프론트에서 수당신청(전자결재)시 호출
    public MemberAllowance linkApprovalRequest(UUID memberAllowanceId,
                                               UUID memberId,
                                               UUID approvalRequestId) {
        MemberAllowance entity = findOwn(memberAllowanceId, memberId);
        entity.linkApprovalRequest(approvalRequestId);
        return entity;
    }

    // 결재 승인 반영, Consumer 가 호출
    public void applyApproval(UUID approvalRequestId, UUID approverId, LocalDateTime decidedAt) {
        MemberAllowance entity = repository.findByApprovalRequestId(approvalRequestId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "해당 결재에 연결된 수당 신청을 찾을 수 없습니다."));

        // 기존 유효 수당 종료, effectiveFrom 전날로 close
        repository.findCurrentByTemplate(
                        entity.getMemberId(),
                        entity.getCompanyId(),
                        entity.getSalaryItemTemplateId(),
                        entity.getEffectiveFrom())
                .filter(prev -> !prev.getMemberAllowanceId().equals(entity.getMemberAllowanceId()))
                .ifPresent(prev -> prev.closeEffectivePeriod(entity.getEffectiveFrom().minusDays(1)));

        entity.approve(approverId, decidedAt);
    }

    // 결재 반려 반영
    public void applyRejection(UUID approvalRequestId, UUID approverId,
                               LocalDateTime decidedAt, String note) {
        MemberAllowance entity = repository.findByApprovalRequestId(approvalRequestId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "해당 결재에 연결된 수당 신청을 찾을 수 없습니다."));
        entity.reject(approverId, decidedAt, note);
    }

    // 본인 수당 신청 철회
    public void cancel(UUID memberAllowanceId, UUID memberId) {
        MemberAllowance entity = findOwn(memberAllowanceId, memberId);
        entity.cancel();
    }

    // 급여 계산 시 호출, 활성 수당 전체 조회
    @Transactional(readOnly = true)
    public List<MemberAllowance> findActiveAt(UUID memberId, UUID companyId, LocalDate date) {
        return repository.findActiveByMemberAndDate(memberId, companyId, date);
    }

    private MemberAllowance findOwn(UUID memberAllowanceId, UUID memberId) {
        MemberAllowance entity = repository.findById(memberAllowanceId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "수당 신청 내역을 찾을 수 없습니다."));

        if (!entity.getMemberId().equals(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "본인 요청만 가능합니다.");
        }
        return entity;
    }

    // 본인 수당 이력 조회
    @Transactional(readOnly = true)
    public List<MemberAllowance> findMyHistory(UUID memberId, UUID companyId) {
        return repository.findByMemberIdAndCompanyIdAndDelYnOrderByEffectiveFromDesc(
                memberId, companyId, "N");
    }

    // 관리자, 상태별 회사 수당 조회
    @Transactional(readOnly = true)
    public List<MemberAllowance> findByCompanyAndStatus(
            UUID companyId, AllowanceApprovalStatus status) {
        return repository.findByCompanyIdAndApprovalStatusAndDelYnOrderByRequestedAtDesc(
                companyId, status, "N");
    }

    /**
     * 관리자 - 회사 전체 수당 조회
     */
    @Transactional(readOnly = true)
    public List<MemberAllowance> findCompanyActiveInMonth(
            UUID companyId, AllowanceApprovalStatus status, java.time.YearMonth ym) {
        java.time.LocalDate start = ym.atDay(1);
        java.time.LocalDate end = ym.atEndOfMonth();
        return status == null
                ? repository.findCompanyAllowancesActiveInMonth(companyId, start, end)
                : repository.findCompanyAllowancesActiveInMonthByStatus(companyId, status, start, end);
    }

    /**
     * 관리자 - 특정 직원 활성 수당 조회
     */
    @Transactional(readOnly = true)
    public List<MemberAllowance> findMemberActiveInMonth(
            UUID memberId, UUID companyId, java.time.YearMonth ym) {
        java.time.LocalDate start = ym.atDay(1);
        java.time.LocalDate end = ym.atEndOfMonth();
        return repository.findMemberAllowancesActiveInMonth(memberId, companyId, start, end);
    }
}