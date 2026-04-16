package com._team._team.memberchat.domain;

import com._team._team.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 특정 방/사용자에 대해 삭제/퍼지를 중단시키는 Legal Hold.
 */
@Entity
@Table(name = "mc_chat_legal_hold", indexes = {
        @Index(name = "idx_mc_hold_room", columnList = "room_id,released"),
        @Index(name = "idx_mc_hold_member", columnList = "member_id,released")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatLegalHold extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "member_id")
    private UUID memberId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(length = 100)
    private String caseId;

    @Column(nullable = false)
    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean released = false;

    public void release() { this.released = true; this.endAt = LocalDateTime.now(); }
}
