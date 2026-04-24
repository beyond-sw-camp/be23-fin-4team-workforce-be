package com._team._team.notification.service;

import com._team._team.dto.BusinessException;
import com._team._team.member.domain.Member;
import com._team._team.member.repository.MemberRepository;
import com._team._team.notification.domain.Notification;
import com._team._team.notification.dto.NotificationResDto;
import com._team._team.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;

    // 알림 목록 조회
    @Transactional(readOnly = true)
    public List<NotificationResDto> getNotificationList(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."));

        return notificationRepository
                .findByMemberAndDelYnOrderByCreatedAtDesc(member, "N")
                .stream()
                .map(NotificationResDto::fromEntity)
                .collect(Collectors.toList());
    }
//    @Transactional(readOnly = true)
//    public List<NotificationResDto> getNotificationList(UUID memberId) {
//
//        Member member = memberRepository.findById(memberId)
//                .orElseThrow(() -> new BusinessException(
//                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."
//                ));
//
//        return notificationRepository
//                .findByMemberOrderByCreatedAtDesc(member)
//                .stream()
//                .map(NotificationResDto::fromEntity)
//                .collect(Collectors.toList());
//    }

//    // 안읽은 알림 수 조회
//    @Transactional(readOnly = true)
//    public int getUnreadCount(UUID memberId) {
//
//        Member member = memberRepository.findById(memberId)
//                .orElseThrow(() -> new BusinessException(
//                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."
//                ));
//
//        return notificationRepository.countByMemberAndIsRead(member, "NO", "N");
//    }

    // 안읽은 알림 수
    @Transactional(readOnly = true)
    public int getUnreadCount(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."));

        return notificationRepository.countByMemberAndIsReadAndDelYn(member, "NO", "N");
    }


    // 알림 읽음 처리
    public void readNotification(UUID memberId, UUID notificationId) {

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."
                ));

        if (!notification.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "본인의 알림만 읽음 처리할 수 있습니다."
            );
        }

        notification.read();
    }

    // 알림 전체 읽음 처리
    public void readAllNotification(UUID memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."
                ));

        notificationRepository
                .findByMemberAndIsRead(member, "NO")
                .forEach(Notification::read);
    }


    public void sendMissedNotifications(UUID memberId,
                                        String lastEventId,
                                        SseEmitter emitter) {
        // lastEventId 없으면 스킵
        if (lastEventId == null || lastEventId.isEmpty()) return;

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 계정입니다."
                ));

        // lastEventId 이후 못받은 알림 조회
        LocalDateTime lastEventTime = LocalDateTime.parse(lastEventId);

        notificationRepository
                .findByMemberAndCreatedAtAfterOrderByCreatedAtAsc(
                        member, lastEventTime)
                .forEach(notification -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .id(notification.getCreatedAt().toString())
                                .name("notification")
                                .data(NotificationResDto.fromEntity(notification)));
                    } catch (Exception e) {
                        log.error("못받은 알림 전송 실패: {}", e.getMessage());
                    }
                });
    }

    // 알림 삭제
    public void deleteNotification(UUID memberId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."));

        if (!notification.getMember().getMemberId().equals(memberId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN, "본인의 알림만 삭제할 수 있습니다.");
        }

        notification.delete();
    }
}