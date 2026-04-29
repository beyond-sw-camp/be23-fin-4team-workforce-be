package com._team._team.chat.service;

import com._team._team.chat.domain.ChatHistory;
import com._team._team.chat.dto.resdto.*;
import com._team._team.chat.dto.reqdto.*;
import com._team._team.chat.repository.ChatHistoryRepository;
import com._team._team.dto.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ChatService {

    private final RestTemplate restTemplate;
    private final ChatHistoryRepository chatHistoryRepository;
    private final SemanticMemoryService semanticMemoryService;
    private final ObjectMapper objectMapper;


    @Value("${n8n.webhook.url}")
    private String n8nWebhookUrl;

    @Autowired
    public ChatService(RestTemplate restTemplate, ChatHistoryRepository chatHistoryRepository, SemanticMemoryService semanticMemoryService, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.chatHistoryRepository = chatHistoryRepository;
        this.semanticMemoryService = semanticMemoryService;
        this.objectMapper = objectMapper;
    }

    // 챗봇 질문
    public ChatResDto chat(UUID memberId, UUID companyId,
                           UUID memberPositionId, String authorization,
                           ChatReqDto reqDto) {
        try {
//            // 1. SemanticMemory KNN 검색
//            String cachedAnswer = semanticMemoryService.findBotReplyWithKnn(
//                    companyId.toString(),
//                    memberId.toString(),
//                    reqDto.getQuestion());
//
//            if (cachedAnswer != null) {
//                log.info("SemanticMemory 캐시 히트");
//                chatHistoryRepository.save(ChatHistory.builder()
//                        .memberId(memberId)
//                        .companyId(companyId)
//                        .question(reqDto.getQuestion())
//                        .answer(cachedAnswer)
//                        .build());
//                return ChatResDto.builder().answer(cachedAnswer).build();
//            }

            // 1.5. 최근 대화 이력 조회 (최근 5턴, 30분 이내)
            String conversationHistory = buildConversationHistory(memberId, companyId);

            // 2. n8n 호출
            ChatN8nReqDto n8nReqDto = ChatN8nReqDto.builder()
                    .question(reqDto.getQuestion())
                    .memberId(memberId.toString())
                    .companyId(companyId.toString())
                    .memberPositionId(memberPositionId.toString())
                    .accessToken(authorization.replace("Bearer ", ""))
                    .conversationHistory(conversationHistory)
                    .build();


            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            n8nWebhookUrl, n8nReqDto, String.class);

            log.info("n8n raw 응답: {}", response.getBody());

            // 3. body null 체크
            if (response.getBody() == null || response.getBody().isBlank()) {
                log.error("n8n 응답 body null - status: {}", response.getStatusCode());
                throw new BusinessException(
                        HttpStatus.BAD_GATEWAY,
                        "챗봇 서비스 응답이 없습니다.");
            }

            // 4. String → Map 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> bodyMap = objectMapper.readValue(
                    response.getBody(), Map.class);

            String answer = (String) bodyMap.get("answer");

            // 5. answer null 체크
            if (answer == null) {
                log.error("n8n 응답 answer 필드 null - body: {}", response.getBody());
                throw new BusinessException(
                        HttpStatus.BAD_GATEWAY,
                        "챗봇 서비스 응답 형식이 올바르지 않습니다.");
            }

            // 6. Redis Stack 저장
            UUID uuidKey = UUID.randomUUID();
            semanticMemoryService.saveToRedis(
                    memberId.toString(), companyId.toString(),
                    "USER", reqDto.getQuestion(), uuidKey);
            semanticMemoryService.saveToRedis(
                    memberId.toString(), companyId.toString(),
                    "BOT", answer, uuidKey);

            // 7. DB 저장
            chatHistoryRepository.save(ChatHistory.builder()
                    .memberId(memberId)
                    .companyId(companyId)
                    .question(reqDto.getQuestion())
                    .answer(answer)
                    .build());

            return ChatResDto.builder().answer(answer).build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("n8n 챗봇 호출 실패: {}", e.getMessage());
            throw new BusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "챗봇 서비스 호출 실패");
        }
    }

    // 대화 이력 조회
    @Transactional(readOnly = true)
    public List<ChatHistoryResDto> getChatHistory(UUID memberId, UUID companyId) {
        return chatHistoryRepository
                .findByMemberIdAndCompanyIdOrderByCreatedAtAsc(memberId, companyId)
                .stream()
                .map(ChatHistoryResDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 대화 이력 삭제
    public void deleteChatHistory(UUID memberId, UUID companyId) {
        chatHistoryRepository.deleteByMemberIdAndCompanyId(memberId, companyId);
    }

    /**
     * 최근 5턴 + 30분 이내 대화 이력을 LLM 컨텍스트용 문자열로 변환
     */
    private String buildConversationHistory(UUID memberId, UUID companyId) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(30);
        Pageable pageable = PageRequest.of(0, 5);

        List<ChatHistory> recent = chatHistoryRepository.findRecentHistory(
                memberId,
                companyId,
                since,
                pageable
        );

        if (recent.isEmpty()) {
            return "";
        }

        Collections.reverse(recent);

        return recent.stream()
                .map(h -> String.format("Q: %s\nA: %s", h.getQuestion(), h.getAnswer()))
                .collect(Collectors.joining("\n---\n"));
    }
}