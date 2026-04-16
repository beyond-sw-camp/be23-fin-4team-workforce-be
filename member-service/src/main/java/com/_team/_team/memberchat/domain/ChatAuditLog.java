package com._team._team.memberchat.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.memberchat.domain.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 누가 언제 어떤 방/메시지를 열람/수정/삭제/내보냈는지 기록.
 * append-only, retention 정책의 영향을 받지 않음.
 */
@Entity
@Table(name = "mc_chat_audit_log", indexes = {
        @Index(name = "idx_mc_audit_actor", columnList = "actor_id,createdAt"),
        @Index(name = "idx_mc_audit_target", columnList = "target_type,target_id,createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatAuditLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;    // ROOM | MESSAGE | FILE | EXPORT

    @Column(name = "target_id")
    private Long targetId;

    @Column(length = 64)
    private String ip;

    @Column(length = 512)
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String metadata;
}
