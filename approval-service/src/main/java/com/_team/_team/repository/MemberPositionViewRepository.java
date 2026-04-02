package com._team._team.repository;

import com._team._team.domain.read.MemberPositionView;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberPositionViewRepository extends JpaRepository<MemberPositionView, UUID> {
    @Query("SELECT mp FROM MemberPositionView mp " +
            "JOIN FETCH mp.member " +
            "JOIN FETCH mp.organization " +
            "JOIN FETCH mp.jobTitle " +
            "JOIN FETCH mp.jobGrade " +
            "WHERE mp.memberPositionId = :id " +
            "AND mp.isActiveYn = 'YES' AND mp.delYn = 'NO'")
    Optional<MemberPositionView> findByIdWithDetails(@Param("id") UUID memberPositionId);

    @Query("SELECT mp FROM MemberPositionView mp " +
            "JOIN FETCH mp.member " +
            "JOIN FETCH mp.organization " +
            "JOIN FETCH mp.jobTitle " +
            "JOIN FETCH mp.jobGrade " +
            "WHERE mp.organization.organizationId = :orgId " +
            "AND mp.jobTitle.jobTitleId = :jobTitleId " +
            "AND mp.isActiveYn = 'YES' AND mp.delYn = 'NO'")
    List<MemberPositionView> findByOrganizationAndJobTitle(
            @Param("orgId") UUID organizationId,
            @Param("jobTitleId") UUID jobTitleId);

    @Query("SELECT mp FROM MemberPositionView mp " +
            "JOIN FETCH mp.member " +
            "JOIN FETCH mp.organization " +
            "JOIN FETCH mp.jobTitle " +
            "JOIN FETCH mp.jobGrade " +
            "WHERE mp.organization.organizationId = :orgId " +
            "AND mp.jobGrade.jobGradeId = :jobGradeId " +
            "AND mp.isActiveYn = 'YES' AND mp.delYn = 'NO'")
    List<MemberPositionView> findByOrganizationAndJobGrade(
            @Param("orgId") UUID organizationId,
            @Param("jobGradeId") UUID jobGradeId);
}
