package com._team._team.salary.repository;

import com._team._team.salary.domain.MemberAllowance;
import com._team._team.salary.domain.enums.AllowanceApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberAllowanceRepository extends JpaRepository<MemberAllowance, UUID> {


    // 급여 계산 시 호출, 특정 일자에 활성 수당 전체
    @Query("""
           SELECT a FROM MemberAllowance a
           WHERE a.memberId = :memberId
             AND a.companyId = :companyId
             AND a.effectiveFrom <= :date
             AND (a.effectiveTo IS NULL OR a.effectiveTo >= :date)
             AND a.approvalStatus IN ('APPROVED', 'AUTO')
             AND a.delYn = 'N'
           """)
    List<MemberAllowance> findActiveByMemberAndDate(@Param("memberId") UUID memberId,
                                                    @Param("companyId") UUID companyId,
                                                    @Param("date") LocalDate date);

    // 특정 수당 항목의 최신 활성 건 조회, 변경 시 close 대상 탐색용
    @Query("""
           SELECT a FROM MemberAllowance a
           WHERE a.memberId = :memberId
             AND a.companyId = :companyId
             AND a.salaryItemTemplateId = :templateId
             AND (a.effectiveTo IS NULL OR a.effectiveTo >= :date)
             AND a.approvalStatus IN ('APPROVED', 'AUTO')
             AND a.delYn = 'N'
           ORDER BY a.effectiveFrom DESC
           """)
    Optional<MemberAllowance> findCurrentByTemplate(@Param("memberId") UUID memberId,
                                                    @Param("companyId") UUID companyId,
                                                    @Param("templateId") UUID templateId,
                                                    @Param("date") LocalDate date);

    // 결재 이벤트 수신 시 연결된 레코드 조회
    Optional<MemberAllowance> findByApprovalRequestId(UUID approvalRequestId);

    // 개인 수당 이력 전체
    List<MemberAllowance> findAllByMemberIdAndCompanyIdOrderByEffectiveFromDesc(
            UUID memberId, UUID companyId);

    // 직원 본인 현재 수당 목록
    List<MemberAllowance> findByMemberIdAndCompanyIdAndDelYnOrderByEffectiveFromDesc(
            UUID memberId, UUID companyId, String delYn);

    // 관리자 결재 대기 목록
    List<MemberAllowance> findAllByCompanyIdAndApprovalStatusOrderByRequestedAtAsc(
            UUID companyId, AllowanceApprovalStatus status);

    // 회사 전체 수당 상태별 조회 (관리자)
    List<MemberAllowance> findByCompanyIdAndApprovalStatusAndDelYnOrderByRequestedAtDesc(
            UUID companyId, AllowanceApprovalStatus status, String delYn);
}
