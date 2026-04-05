package com._team._team.approval.repository;

import com._team._team.approval.domain.AbsenceProxy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AbsenceProxyRepository extends JpaRepository<AbsenceProxy, UUID> {

    // 부재자의 활성 대결 설정 조회
    List<AbsenceProxy> findByMemberIdAndIsActiveYn(UUID memberId, String isActiveYn);

    // 현재 시점 유효한 대결자 조회
    @Query("SELECT ap FROM AbsenceProxy ap " +
            "WHERE ap.memberId = :memberId " +
            "AND ap.isActiveYn = 'Y' " +
            "AND ap.startDate <= :now " +
            "AND ap.endDate >= :now")
    Optional<AbsenceProxy> findActiveProxy(
            @Param("memberId") UUID memberId,
            @Param("now") LocalDateTime now);

    // 대결자로 지정된 목록 조회
    List<AbsenceProxy> findBySubstituteIdAndIsActiveYn(UUID substituteId, String isActiveYn);
}
