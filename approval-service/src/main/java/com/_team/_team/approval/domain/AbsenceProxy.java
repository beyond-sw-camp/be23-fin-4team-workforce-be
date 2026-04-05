package com._team._team.approval.domain;

import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
public class AbsenceProxy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID proxyId;

    @Column(nullable = false)
    private UUID memberId; // 부재자(원래 결재자)

    @Column(nullable = false)
    private UUID substituteId; // 대결자

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Builder.Default
    @Column(nullable = false)
    private String isActiveYn = "Y";

    public void activate() {
        this.isActiveYn = "Y";
    }

    public void deactivate() {
        this.isActiveYn = "N";
    }
}
