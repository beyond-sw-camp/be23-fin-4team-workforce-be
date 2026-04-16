package com._team._team.memberchat.service;

import com._team._team.memberchat.domain.enums.AuditAction;
import com._team._team.memberchat.domain.ChatMessage;
import com._team._team.memberchat.error.ChatErrorCode;
import com._team._team.memberchat.error.ChatException;
import com._team._team.memberchat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * HR_ADMIN / AUDITOR 용. 모든 조회/내보내기는 감사로그를 남긴다.
 */
@Service
@RequiredArgsConstructor
public class AdminChatService {

    private final ChatMessageRepository messageRepo;
    private final AuditLogService audit;

    @Transactional(readOnly = true)
    public List<ChatMessage> search(UUID actor, boolean isSystemAdmin, UUID companyId,
                                    LocalDateTime from, LocalDateTime to,
                                    Long roomId, UUID senderId, String keyword, int size) {
        requireAdmin(isSystemAdmin);
        List<ChatMessage> result = messageRepo.adminSearch(companyId, from, to, roomId, senderId, keyword,
                PageRequest.of(0, Math.min(size, 1000)));
        result.forEach(m -> audit.record(actor, AuditAction.MESSAGE_VIEWED_BY_ADMIN, "MESSAGE", m.getId()));
        return result;
    }

    @Transactional
    public byte[] exportCsv(UUID actor, boolean isSystemAdmin, UUID companyId,
                            LocalDateTime from, LocalDateTime to, Long roomId) {
        requireAdmin(isSystemAdmin);
        List<ChatMessage> rows = messageRepo.adminSearch(companyId, from, to, roomId, null, null,
                PageRequest.of(0, 100_000));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            pw.println("messageId,roomId,senderId,type,content,deleted,edited,createdAt");
            for (ChatMessage m : rows) {
                pw.printf("%d,%d,%s,%s,\"%s\",%s,%s,%s%n",
                        m.getId(), m.getChatRoom().getId(), m.getSenderId(), m.getType(),
                        m.getContent().replace("\"", "\"\""),
                        m.isDeleted(), m.isEdited(), m.getCreatedAt());
            }
        }
        audit.record(actor, AuditAction.EXPORT_COMPLETED, "EXPORT", (long) rows.size());
        return baos.toByteArray();
    }

    /**
     * 현 JWT 에는 isSystemAdmin 이외의 role 클레임이 없으므로 System Admin 만 관리 기능을 사용할 수 있다.
     * 향후 MANAGER/AUDITOR 세분화가 추가되면 여기를 확장한다.
     */
    private void requireAdmin(boolean isSystemAdmin) {
        if (!isSystemAdmin) {
            throw new ChatException(ChatErrorCode.ROLE_FORBIDDEN);
        }
    }
}
