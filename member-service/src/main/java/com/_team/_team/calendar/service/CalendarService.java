package com._team._team.calendar.service;

import com._team._team.calendar.domain.CalendarEvent;
import com._team._team.calendar.domain.enums.EventType;
import com._team._team.calendar.dto.reqdto.CalendarEventCreateReqDto;
import com._team._team.calendar.dto.reqdto.CalendarEventUpdateReqDto;
import com._team._team.calendar.dto.resdto.CalendarEventResDto;
import com._team._team.calendar.repository.CalendarEventRepository;
import com._team._team.dto.BusinessException;
import com._team._team.member.domain.Member;
import com._team._team.member.domain.MemberPosition;
import com._team._team.member.repository.MemberPositionRepository;
import com._team._team.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class CalendarService {

    private final CalendarEventRepository calendarEventRepository;
    private final MemberRepository memberRepository;
    private final MemberPositionRepository memberPositionRepository;

    @Autowired
    public CalendarService(CalendarEventRepository calendarEventRepository, MemberRepository memberRepository, MemberPositionRepository memberPositionRepository) {
        this.calendarEventRepository = calendarEventRepository;
        this.memberRepository = memberRepository;
        this.memberPositionRepository = memberPositionRepository;
    }


    // 개인 일정 생성
    public UUID createPersonalEvent(UUID memberId, CalendarEventCreateReqDto reqDto) {
        Member member = getMember(memberId);

        validateDate(reqDto.getStartAt(), reqDto.getEndAt());

        CalendarEvent event = CalendarEvent.builder()
                .company(member.getCompany())
                .member(member)
                .title(reqDto.getTitle())
                .description(reqDto.getDescription())
                .startAt(reqDto.getStartAt())
                .endAt(reqDto.getEndAt())
                .eventType(EventType.PERSONAL)
                .isPublicYn(reqDto.getIsPublicYn())
                .build();

        return calendarEventRepository.save(event).getCalendarEventId();
    }

    // 개인 일정 수정 (본인만)
    public void updatePersonalEvent(UUID memberId, UUID eventId,
                                    CalendarEventUpdateReqDto reqDto) {
        CalendarEvent event = getEvent(eventId);

        validateOwner(event, memberId);
        validateEventType(event, EventType.PERSONAL);
        validateDate(reqDto.getStartAt(), reqDto.getEndAt());

        event.update(
                reqDto.getTitle(),
                reqDto.getDescription(),
                reqDto.getStartAt(),
                reqDto.getEndAt(),
                reqDto.getIsPublicYn()
        );
    }

    // 개인 일정 삭제 (본인만)
    public void deletePersonalEvent(UUID memberId, UUID eventId) {
        CalendarEvent event = getEvent(eventId);

        validateOwner(event, memberId);
        validateEventType(event, EventType.PERSONAL);

        event.delete();
    }

    // ═══════════════════════════════════════════════════════════
    // 팀 일정
    // ═══════════════════════════════════════════════════════════

    // 팀 일정 생성 (팀장, 인사 관리자)
    public UUID createTeamEvent(UUID memberId, CalendarEventCreateReqDto reqDto) {
        Member member = getMember(memberId);

        validateDate(reqDto.getStartAt(), reqDto.getEndAt());

        if (reqDto.getOrganizationId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "팀 일정은 조직 정보가 필요합니다.");
        }

        CalendarEvent event = CalendarEvent.builder()
                .company(member.getCompany())
                .member(member)
                .organizationId(reqDto.getOrganizationId())
                .title(reqDto.getTitle())
                .description(reqDto.getDescription())
                .startAt(reqDto.getStartAt())
                .endAt(reqDto.getEndAt())
                .eventType(EventType.TEAM)
                .isPublicYn("YES")  // 팀 일정은 항상 공개
                .build();

        return calendarEventRepository.save(event).getCalendarEventId();
    }

    // 팀 일정 수정 (생성자만)
    public void updateTeamEvent(UUID memberId, UUID eventId,
                                CalendarEventUpdateReqDto reqDto) {
        CalendarEvent event = getEvent(eventId);

        validateOwner(event, memberId);
        validateEventType(event, EventType.TEAM);
        validateDate(reqDto.getStartAt(), reqDto.getEndAt());

        event.update(
                reqDto.getTitle(),
                reqDto.getDescription(),
                reqDto.getStartAt(),
                reqDto.getEndAt(),
                "YES"  // 팀 일정은 항상 공개
        );
    }

    // 팀 일정 삭제 (생성자만)
    public void deleteTeamEvent(UUID memberId, UUID eventId) {
        CalendarEvent event = getEvent(eventId);

        validateOwner(event, memberId);
        validateEventType(event, EventType.TEAM);

        event.delete();
    }


    // 월별 일정 조회 (개인 + 팀 통합)
    @Transactional(readOnly = true)
    public List<CalendarEventResDto> getMonthlyEvents(UUID memberId, int year, int month) {
        Member member = getMember(memberId);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startAt = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endAt = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<CalendarEvent> events = new ArrayList<>();

        // 1. 개인 일정
        events.addAll(calendarEventRepository
                .findPersonalByMonth(member, startAt, endAt));

        // 2. 팀 일정 (본인 조직)
        MemberPosition position = memberPositionRepository
                .findById(member.getDefaultPositionId())
                .orElse(null);

        if (position != null) {
            events.addAll(calendarEventRepository.findTeamByMonth(
                    position.getOrganization().getOrganizationId(),
                    startAt, endAt));
        }

        // 시작일시 기준 정렬
        return events.stream()
                .sorted((a, b) -> a.getStartAt().compareTo(b.getStartAt()))
                .map(CalendarEventResDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 일정 상세 조회
    @Transactional(readOnly = true)
    public CalendarEventResDto getEventDetail(UUID memberId, UUID eventId) {
        Member member = getMember(memberId);
        CalendarEvent event = getEvent(eventId);

        // 다른 회사 일정 조회 불가
        if (!event.getCompany().getCompanyId()
                .equals(member.getCompany().getCompanyId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "접근할 수 없는 일정입니다.");
        }

        // 비공개 개인 일정은 본인만 조회 가능
        if ("NO".equals(event.getIsPublicYn())
                && !event.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "비공개 일정입니다.");
        }

        return CalendarEventResDto.fromEntity(event);
    }

    // Private 헬퍼

    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."));
    }

    private CalendarEvent getEvent(UUID eventId) {
        return calendarEventRepository
                .findByCalendarEventIdAndDelYn(eventId, "NO")
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 일정입니다."));
    }

    private void validateDate(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt.isBefore(startAt)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "종료일시는 시작일시보다 이후여야 합니다.");
        }
    }

    private void validateOwner(CalendarEvent event, UUID memberId) {
        if (!event.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "본인이 생성한 일정만 수정/삭제할 수 있습니다.");
        }
    }

    private void validateEventType(CalendarEvent event, EventType expectedType) {
        if (event.getEventType() != expectedType) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "일정 유형이 올바르지 않습니다.");
        }
    }
}