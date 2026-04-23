package com._team._team.attendance.repository;

import com._team._team.attendance.domain.LeavePromotionLog;
import com._team._team.attendance.domain.enums.PromotionStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeavePromotionLogRepository extends JpaRepository<LeavePromotionLog, UUID> {
    boolean existsByMemberBalanceIdAndStage(UUID memberBalanceId, PromotionStage stage);
}