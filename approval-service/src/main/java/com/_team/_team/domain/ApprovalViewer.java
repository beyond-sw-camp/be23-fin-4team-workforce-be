package com._team._team.domain;

import com._team._team.domain.enums.ViewerReadStatus;
import com._team._team.domain.enums.ViewerType;
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
public class ApprovalViewer extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID viewerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_id", nullable = false)
    private Approval approval;

    @Column(nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViewerType viewerType; // CC(참조), CIRCULATION(공람)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ViewerReadStatus status; // UNREAD(미확인), READ(확인 완료)

    private LocalDateTime viewedAt; // 사용자가 실제 문서 확인 시각

    @Column(nullable = false)
    private LocalDateTime createdAt; // 참조/공람 대상자로 지정된 시각
}
