package com._team._team.memberchat.domain;

import com._team._team.domain.BaseTimeEntity;
import com._team._team.memberchat.domain.enums.ChatRoomType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mc_chat_room", indexes = {
        @Index(name = "idx_mc_room_company", columnList = "company_id"),
        @Index(name = "idx_mc_room_deleted", columnList = "deleted")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRoomType type;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(length = 500)
    private String imageUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean deleted = false;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatParticipant> participants = new ArrayList<>();

    public void softDelete() { this.deleted = true; }
    public void rename(String newName) { this.name = newName; }
    public void updateImageUrl(String url) { this.imageUrl = url; }
}
