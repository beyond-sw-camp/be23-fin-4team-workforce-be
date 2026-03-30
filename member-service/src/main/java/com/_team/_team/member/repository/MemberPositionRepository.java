package com._team._team.member.repository;

import com._team._team.member.domain.MemberPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberPositionRepository extends JpaRepository<MemberPosition, UUID> {
    // 특정 멤버의 활성 포지션 조회
    Optional<MemberPosition> findByMember_MemberIdAndIsActiveYnAndDelYn(
            UUID memberId, String isActiveYn, String delYn);

    // 특정 멤버의 전체 포지션 조회
    List<MemberPosition> findByMember_MemberIdAndDelYn(UUID memberId, String delYn);

    // 시스템 관리자 조회 (회사별)
    List<MemberPosition> findByMember_Company_CompanyIdAndIsSystemAdminYn(
            UUID companyId, String isSystemAdminYn);
}
