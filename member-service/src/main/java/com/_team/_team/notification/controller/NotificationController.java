package com._team._team.notification.controller;

import com._team._team.dto.ApiResponse;
import com._team._team.notification.service.NotificationService;
import com._team._team.notification.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final SseEmitterManager sseEmitterManager;
    private final NotificationService notificationService;

    // SSE 구독
    @GetMapping(value = "/subscribe",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestHeader("X-User-UUID") UUID memberId,
            @RequestHeader(value = "Last-Event-ID",
                    required = false, defaultValue = "") String lastEventId) {

        return sseEmitterManager.add(memberId, lastEventId);
    }

    // 알림 목록 조회
    @GetMapping("/list")
    public ResponseEntity<?> getNotificationList(
            @RequestHeader("X-User-UUID") UUID memberId) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        notificationService.getNotificationList(memberId),
                        "알림 목록 조회 성공"),
                HttpStatus.OK
        );
    }

    // 안읽은 알림 수 조회
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(
            @RequestHeader("X-User-UUID") UUID memberId) {
        return new ResponseEntity<>(
                ApiResponse.success(
                        notificationService.getUnreadCount(memberId),
                        "안읽은 알림 수 조회 성공"),
                HttpStatus.OK
        );
    }

    // 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<?> readNotification(
            @RequestHeader("X-User-UUID") UUID memberId,
            @PathVariable UUID notificationId) {
        notificationService.readNotification(memberId, notificationId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "알림 읽음 처리 성공"),
                HttpStatus.OK
        );
    }

    // 알림 전체 읽음 처리
    @PatchMapping("/read-all")
    public ResponseEntity<?> readAllNotification(
            @RequestHeader("X-User-UUID") UUID memberId) {
        notificationService.readAllNotification(memberId);
        return new ResponseEntity<>(
                ApiResponse.success(null, "알림 전체 읽음 처리 성공"),
                HttpStatus.OK
        );
    }
}