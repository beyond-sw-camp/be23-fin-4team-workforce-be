package com._team._team.memberchat.repository;

import com._team._team.memberchat.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByClientMessageId(String clientMessageId);

    /** cursor(before) 기반 페이지네이션: messageId < cursor */
    @Query("""
            select m from ChatMessage m
            where m.chatRoom.id = :roomId and (:cursor is null or m.id < :cursor)
            order by m.id desc
            """)
    List<ChatMessage> pageBefore(@Param("roomId") Long roomId,
                                 @Param("cursor") Long cursor,
                                 Pageable pageable);

    /** 재연결 sync: lastSeenMessageId 초과 메시지 */
    @Query("""
            select m from ChatMessage m
            where m.chatRoom.id = :roomId and m.id > :lastSeen
            order by m.id asc
            """)
    List<ChatMessage> fetchAfter(@Param("roomId") Long roomId,
                                 @Param("lastSeen") Long lastSeenMessageId,
                                 Pageable pageable);

    /** Admin 검색: 회사/기간/키워드/방/멤버 조합 필터. */
    @Query("""
            select m from ChatMessage m
            where m.chatRoom.companyId = :companyId
              and (:from is null or m.createdAt >= :from)
              and (:to   is null or m.createdAt <= :to)
              and (:roomId is null or m.chatRoom.id = :roomId)
              and (:senderId is null or m.senderId = :senderId)
              and (:keyword is null or m.content like concat('%', :keyword, '%'))
            order by m.id desc
            """)
    List<ChatMessage> adminSearch(@Param("companyId") java.util.UUID companyId,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  @Param("roomId") Long roomId,
                                  @Param("senderId") java.util.UUID senderId,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    /** Retention batch: 보존 기간 경과 + legal hold 없는 방의 메시지 purge 대상. */
    @Modifying
    @Query("""
            delete from ChatMessage m
            where m.chatRoom.legalHold = false
              and m.chatRoom.id not in (
                  select h.roomId from ChatLegalHold h where h.released = false and h.roomId is not null
              )
              and m.createdAt < :threshold
            """)
    int purgeOlderThan(@Param("threshold") LocalDateTime threshold);
}
