package com._team._team.calendar.repository;


import com._team._team.calendar.domain.CalendarEvent;
import com._team._team.calendar.domain.enums.EventType;
import com._team._team.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, UUID> {

    // 개인 일정 조회
    List<CalendarEvent> findByMemberAndEventTypeAndDelYnOrderByStartAtAsc(
            Member member, EventType eventType, String delYn);

    // 월별 개인 일정 조회
    @Query("SELECT c FROM CalendarEvent c " +
            "WHERE c.member = :member " +
            "AND c.delYn = 'NO' " +
            "AND c.startAt >= :startAt " +
            "AND c.startAt < :endAt " +
            "ORDER BY c.startAt ASC")
    List<CalendarEvent> findPersonalByMonth(
            @Param("member") Member member,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    // 월별 팀 일정 조회 (같은 조직 + 공개 설정)
    @Query("SELECT c FROM CalendarEvent c " +
            "WHERE c.organizationId = :organizationId " +
            "AND c.eventType = 'TEAM' " +
            "AND c.isPublicYn = 'YES' " +
            "AND c.delYn = 'NO' " +
            "AND c.startAt >= :startAt " +
            "AND c.startAt < :endAt " +
            "ORDER BY c.startAt ASC")
    List<CalendarEvent> findTeamByMonth(
            @Param("organizationId") UUID organizationId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    // 단건 조회
    Optional<CalendarEvent> findByCalendarEventIdAndDelYn(
            UUID calendarEventId, String delYn);
}