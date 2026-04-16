package com._team._team.memberchat.domain;

import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * 메시지 수정/삭제 이력. Append-only. 감사/컴플라이언스 용도.
 */
@Entity
@Table(name = "mc_chat_message_revision", indexes = {
        @Index(name = "idx_mc_rev_message", columnList = "message_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessageRevision extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RevisionAction action;

    @Column(length = 4000)
    private String previousContent;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    public enum RevisionAction { EDIT, DELETE }
}
