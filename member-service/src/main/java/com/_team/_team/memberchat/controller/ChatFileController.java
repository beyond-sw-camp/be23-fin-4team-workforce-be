package com._team._team.memberchat.controller;

import com._team._team.dto.ApiResponse;
import com._team._team.memberchat.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

// 채팅 첨부 업로드·다운로드
@RestController
@RequestMapping("/member-chat/files")
@RequiredArgsConstructor
public class ChatFileController {

    private final FileService fileService;

    // 업로드 주소 발급
    @PostMapping("/presigned")
    public ResponseEntity<?> presigned(@RequestHeader("X-User-UUID") UUID uploader,
                                       @RequestBody Map<String, Object> body) {
        String name = String.valueOf(body.get("filename"));
        String mime = String.valueOf(body.get("mime"));
        long size = ((Number) body.getOrDefault("size", 0)).longValue();
        return ResponseEntity.ok(ApiResponse.success(
                fileService.presignUpload(uploader, name, mime, size), "ok"));
    }

    // 업로드 완료 확인
    @PostMapping("/confirm")
    public ResponseEntity<?> confirm(@RequestHeader("X-User-UUID") UUID uploader,
                                     @RequestBody Map<String, Object> body) {
        String key = String.valueOf(body.get("key"));
        Long messageId = body.get("messageId") == null ? null : ((Number) body.get("messageId")).longValue();
        return ResponseEntity.ok(ApiResponse.success(
                fileService.confirmUpload(uploader, key, messageId), "confirmed"));
    }

    // 다운로드로 이동
    @GetMapping("/{key}")
    public ResponseEntity<?> download(@PathVariable String key,
                                      @RequestParam(defaultValue = "CLEAN") String scanStatus) {
        String url = fileService.presignDownload(key, scanStatus);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }
}
