package com._team._team.chat.service;

import com._team._team.chat.domain.ChatHistory;
import com._team._team.chat.dto.resdto.*;
import com._team._team.chat.dto.reqdto.*;
import com._team._team.chat.repository.ChatHistoryRepository;
import com._team._team.dto.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ChatService {

    private final RestTemplate restTemplate;
    private final ChatHistoryRepository chatHistoryRepository;
    private final SemanticMemoryService semanticMemoryService;


    @Value("${n8n.webhook.url}")
    private String n8nWebhookUrl;

    @Autowired
    public ChatService(RestTemplate restTemplate, ChatHistoryRepository chatHistoryRepository, SemanticMemoryService semanticMemoryService) {
        this.restTemplate = restTemplate;
        this.chatHistoryRepository = chatHistoryRepository;
        this.semanticMemoryService = semanticMemoryService;
    }

    // 챗봇 질문
    public ChatResDto chat(UUID memberId, UUID companyId,
                           UUID memberPositionId, String authorization,
                           ChatReqDto reqDto) {
        try {
            // 1. SemanticMemory KNN 검색
            String cachedAnswer = semanticMemoryService.findBotReplyWithKnn(
                    companyId.toString(),
                    memberId.toString(),
                    reqDto.getQuestion());

            if (cachedAnswer != null) {
                log.info("SemanticMemory 캐시 히트");
                chatHistoryRepository.save(ChatHistory.builder()
                        .memberId(memberId)
                        .companyId(companyId)
                        .question(reqDto.getQuestion())
                        .answer(cachedAnswer)
                        .build());
                return ChatResDto.builder().answer(cachedAnswer).build();
            }

            // 2. n8n 호출
            ChatN8nReqDto n8nReqDto = ChatN8nReqDto.builder()
                    .question(reqDto.getQuestion())
                    .memberId(memberId.toString())
                    .companyId(companyId.toString())
                    .memberPositionId(memberPositionId.toString())
                    .accessToken(authorization.replace("Bearer ", ""))
                    .build();

            ResponseEntity<ChatResDto> response =
                    restTemplate.postForEntity(
                            n8nWebhookUrl, n8nReqDto, ChatResDto.class);

            ChatResDto result = response.getBody();

            // 3. Redis Stack 에 저장
            UUID uuidKey = UUID.randomUUID();
            semanticMemoryService.saveToRedis(
                    memberId.toString(), companyId.toString(),
                    "USER", reqDto.getQuestion(), uuidKey);
            semanticMemoryService.saveToRedis(
                    memberId.toString(), companyId.toString(),
                    "BOT", result.getAnswer(), uuidKey);

            // 4. DB 저장
            chatHistoryRepository.save(ChatHistory.builder()
                    .memberId(memberId)
                    .companyId(companyId)
                    .question(reqDto.getQuestion())
                    .answer(result.getAnswer())
                    .build());

            return result;

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
                .findByMemberIdAndCompanyIdOrderByCreatedAtDesc(memberId, companyId)
                .stream()
                .map(ChatHistoryResDto::fromEntity)
                .collect(Collectors.toList());
    }

    // 대화 이력 삭제
    public void deleteChatHistory(UUID memberId, UUID companyId) {
        chatHistoryRepository.deleteByMemberIdAndCompanyId(memberId, companyId);
    }
}