package com._team._team.attendance.dto.resDto;

import com._team._team.attendance.domain.LeavePromotionLog;
import com._team._team.attendance.domain.enums.PromotionLogStatus;
import com._team._team.attendance.domain.enums.PromotionStage;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
// 직원 본인이 받은 촉진 통보 단건
public class LeavePromotionMyResDto {
    private UUID promotionLogId;
    private PromotionStage stage;
    private PromotionLogStatus status;
    private LocalDate sentOn;
    private LocalDateTime acknowledgedAt;
    private LocalDate balanceExpirationDate;
    private Double remainingDays;

    public static LeavePromotionMyResDto from(LeavePromotionLog log,
                                              LocalDate balanceExpirationDate,
                                              Double remainingDays) {
        return LeavePromotionMyResDto.builder()
                .promotionLogId(log.getPromotionLogId())
                .stage(log.getStage())
                .status(log.getStatus())
                .sentOn(log.getSentOn())
                .acknowledgedAt(log.getAcknowledgedAt())
                .balanceExpirationDate(balanceExpirationDate)
                .remainingDays(remainingDays)
                .build();
    }
}