package com._team._team.memberchat.controller.admin;

import com._team._team.dto.ApiResponse;
import com._team._team.memberchat.service.AdminChatService;
import com._team._team.memberchat.service.LegalHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * HR 관리자 전용 관리 API.
 * Gateway 가 JWT 의 isSystemAdmin 클레임을 X-User-IsSystemAdmin 헤더로 주입한다.
 * (JWT 에 세부 role 클레임이 없으므로 isSystemAdmin=true 를 관리자로 간주한다.)
 */
@RestController
@RequestMapping("/member-chat/admin")
@RequiredArgsConstructor
public class ChatAdminController {

    private final AdminChatService adminService;
    private final LegalHoldService legalHoldService;

    private static boolean isAdmin(String header) {
        return "true".equalsIgnoreCase(header);
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestHeader("X-User-UUID") UUID actor,
                                    @RequestHeader(value = "X-User-IsSystemAdmin", required = false) String isSystemAdmin,
                                    @RequestHeader("X-User-CompanyId") UUID companyId,
                                    @RequestParam(required = false)
                                    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
                                    LocalDateTime from,
                                    @RequestParam(required = false)
                                    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
                                    LocalDateTime to,
                                    @RequestParam(required = false) Long roomId,
                                    @RequestParam(required = false) UUID senderId,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.search(actor, isAdmin(isSystemAdmin), companyId, from, to, roomId, senderId, q, size),
                "ok"));
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestHeader("X-User-UUID") UUID actor,
                                         @RequestHeader(value = "X-User-IsSystemAdmin", required = false) String isSystemAdmin,
                                         @RequestHeader("X-User-CompanyId") UUID companyId,
                                         @RequestBody Map<String, Object> body) {
        LocalDateTime from = body.get("from") == null ? null : LocalDateTime.parse(String.valueOf(body.get("from")));
        LocalDateTime to   = body.get("to")   == null ? null : LocalDateTime.parse(String.valueOf(body.get("to")));
        Long roomId = body.get("roomId") == null ? null : ((Number) body.get("roomId")).longValue();

        byte[] csv = adminService.exportCsv(actor, isAdmin(isSystemAdmin), companyId, from, to, roomId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mc-export.csv")
                .body(csv);
    }

    @PostMapping("/legal-hold")
    public ResponseEntity<?> applyHold(@RequestHeader("X-User-UUID") UUID actor,
                                       @RequestHeader(value = "X-User-IsSystemAdmin", required = false) String isSystemAdmin,
                                       @RequestBody Map<String, Object> body) {
        if (!isAdmin(isSystemAdmin))
            return ResponseEntity.status(403).body(ApiResponse.fail("ROLE_FORBIDDEN"));
        Long roomId = body.get("roomId") == null ? null : ((Number) body.get("roomId")).longValue();
        UUID memberId = body.get("memberId") == null ? null : UUID.fromString(String.valueOf(body.get("memberId")));
        String reason = String.valueOf(body.getOrDefault("reason", ""));
        String caseId = body.get("caseId") == null ? null : String.valueOf(body.get("caseId"));
        return ResponseEntity.ok(ApiResponse.success(
                legalHoldService.apply(actor, roomId, memberId, reason, caseId), "applied"));
    }

    @DeleteMapping("/legal-hold/{holdId}")
    public ResponseEntity<?> releaseHold(@RequestHeader("X-User-UUID") UUID actor,
                                         @RequestHeader(value = "X-User-IsSystemAdmin", required = false) String isSystemAdmin,
                                         @PathVariable Long holdId) {
        if (!isAdmin(isSystemAdmin))
            return ResponseEntity.status(403).body(ApiResponse.fail("ROLE_FORBIDDEN"));
        legalHoldService.release(actor, holdId);
        return ResponseEntity.ok(ApiResponse.success(null, "released"));
    }
}
