# Member-Chat 서비스 종합 정리

엔터프라이즈 HR 채팅 서비스(`member-chat`) 의 개발 결과와 실행 방법을 한 번에 볼 수 있도록 정리한 문서. 상세 설계는 `docs/member-chat-design.md` 참고.

---

## 1. 개요

### 1.1 왜 새로 만들었나
기존 `member-service/chat` 은 AI 챗봇용이고, `member-service/chat-ref` 는 레거시 1:1 채팅 레퍼런스였다. HR 엔터프라이즈 요구사항(그룹/공지/읽음/파일/법적 보존/감사)을 충족하려면 다음이 필요했다.

- 서버 측에서 강제되는 JWT 인증 (payload 의 sender 신뢰 금지)
- 권한 분리 (EMPLOYEE vs SystemAdmin)
- 메시지 편집/삭제 이력, 감사 로그, 리텐션, Legal Hold
- 파일 업로드 Presigned URL + MIME/크기 이중 검증 + AV 스캔 훅
- 다중 인스턴스 간 Redis Pub/Sub fan-out

### 1.2 완전 분리 원칙
기존 코드와 충돌 없이 공존하도록 모든 네임스페이스를 분리했다.

| 축 | 기존 chat-ref | member-chat |
|---|---|---|
| Java package | `member.chat` | `memberchat` |
| REST prefix | `/chat` | `/member-chat` |
| STOMP endpoint | `/connect` | `/mc/connect` |
| STOMP prefix | `/app`, `/topic` | `/mc/app`, `/mc/topic` |
| Redis channel | `chat` (단일) | `member-chat:room:{roomId}` (방 단위) |
| DB 테이블 | `chat_*` | `mc_chat_*` |
| Config prefix | `chat.*` | `member-chat.*` |

---

## 2. 아키텍처

```
Client (SockJS+STOMP / REST)
   │
   ▼
Gateway (JwtAuthFilter)
   │   X-User-UUID, X-User-CompanyId, X-User-IsSystemAdmin, X-User-Name 주입
   ▼
member-service (Spring Boot)
   ├─ REST  /member-chat/**         ← Room/Message/File/Admin 컨트롤러
   ├─ STOMP /mc/connect (SockJS)    ← ChatStompHandler 에서 JWT 재검증
   │                                    → AuthPrincipal 을 세션에 고정
   ├─ Service
   │   ├─ ChatMessageService / ChatRoomService / ReadReceiptService
   │   ├─ FileService (S3 Presigned)
   │   ├─ LegalHoldService / AdminChatService / RetentionService
   │   ├─ AuditLogService (@Async)
   │   └─ ChatAuthPolicy (중앙 인가 로직)
   ├─ Infra
   │   ├─ MariaDB (mc_ prefix 테이블)
   │   ├─ Redis    (Pub/Sub + RateLimiter + ShedLock)
   │   └─ S3        (파일)
   └─ Redis Subscriber → SimpMessagingTemplate
        member-chat:room:{roomId} → /mc/topic/room/{roomId}
```

### 2.1 보안 경계
1. **Gateway** — HTTP JWT 파싱 후 헤더로 downstream 에 전달.
2. **ChatStompHandler** — WebSocket CONNECT 단계에서 `jwt.secretKeyAt` 키로 JWT 를 서버에서 다시 검증, `AuthPrincipal` 을 세션에 박아둠. SUBSCRIBE/SEND/UNSUBSCRIBE 는 Principal 없으면 거부.
3. **ChatChannelInterceptor** — AntPathMatcher 로 destination 파싱 후 rate limit(60/min per 유저×방) + `ChatAuthPolicy.canSubscribe()` 적용, MDC 삽입.
4. **ChatAuthPolicy** — STOMP/REST 공용 인가 로직. SystemAdmin 은 감사 목적 열람 허용, 일반 사용자는 `JOINED` 참여자만 허용.

---

## 3. 실행 방법

### 3.1 선행 조건
- JDK 17+, Gradle Wrapper
- MariaDB (기존 member-service 와 동일 인스턴스) — `mc_` prefix 테이블 마이그레이션 필요
- Redis — 기존 member-service 와 동일 인스턴스
- AWS S3 버킷 (파일 기능 사용 시) + `S3Client`/`S3Presigner` Bean 정의
- Eureka / Gateway 는 기존 설정 그대로

### 3.2 설정 흐름
`application.yml` 에 profile group 을 걸어두었으므로, `prod` 또는 `local` profile 이 활성화되면 `application-member-chat.yml` overlay 가 자동 로드된다.

```yaml
spring:
  profiles:
    active: prod
    group:
      prod: "prod,member-chat"
      local: "local,member-chat"
```

`application-member-chat.yml` 의 주요 키 (`member-chat.*`):

```yaml
member-chat:
  enabled: true
  websocket:
    endpoint: /mc/connect
    allowed-origins: [http://localhost:3000]
  stomp:
    app-prefix: /mc/app
    topic-prefix: /mc/topic
    user-prefix: /user
  redis:
    channel-prefix: "member-chat:room:"
  files:
    bucket: member-chat-files
    max-size-mb: 20
    allowed-mimes: [image/png, image/jpeg, image/webp, image/gif, application/pdf]
    presigned-ttl-seconds: 300
  retention:
    default-days: 1095          # 3년
    batch-cron: "0 0 3 * * *"   # 매일 03:00
  rate-limit:
    messages-per-minute: 60
    burst: 20
```

JWT 서명키는 기존 `jwt.secretKeyAt` (AccessToken 키) 를 재사용한다. 별도 키 분리 원할 시 `ChatStompHandler` 의 `@Value` 만 교체.

### 3.3 DB 마이그레이션
`member-service/src/main/resources/db/migration/V1__mc_init.sql` 를 기존 마이그레이션 도구(Flyway 등) 혹은 수동 실행으로 적용. 생성 테이블:

```
mc_chat_room, mc_chat_participant, mc_chat_message,
mc_chat_message_image, mc_chat_message_revision, mc_chat_read_receipt,
mc_chat_audit_log, mc_chat_legal_hold
```

### 3.4 SecurityConfig
`/mc/connect/**` 와 `/member-chat/**` 는 명시적으로 `permitAll` 이다. 실제 인증은 Gateway(HTTP) 와 ChatStompHandler(WebSocket) 가 담당한다.

### 3.5 로컬 실행

```bash
# 1. DB/Redis 기동 (docker-compose 사용 중이면 그대로)
# 2. member-service 실행 (local profile)
./gradlew :member-service:bootRun -Dspring.profiles.active=local
# 3. Gateway/Eureka 도 함께 기동
```

### 3.6 클라이언트 연결 예시 (JS)

```js
const sock = new SockJS("/mc/connect");
const stomp = Stomp.over(sock);
stomp.connect({ Authorization: `Bearer ${accessToken}` }, () => {
  // 구독
  stomp.subscribe(`/mc/topic/room/${roomId}`, (msg) => { /* 메시지 이벤트 */ });
  stomp.subscribe(`/mc/topic/read/${roomId}`, (msg) => { /* 읽음 이벤트 */ });
  stomp.subscribe(`/user/queue/errors`,       (msg) => { /* 에러 이벤트 */ });

  // 전송 (sender 는 넣지 않는다 — 서버 Principal 에서 결정)
  stomp.send(`/mc/app/room/${roomId}/send`, {}, JSON.stringify({
    type: "TEXT",
    content: "hello",
    clientMessageId: crypto.randomUUID(),  // 중복 방지
  }));
});
```

---

## 4. 기능별 개발 현황

### P0. 스캐폴딩 & 설계
- `docs/member-chat-design.md` — 요구사항/ERD/API 스펙/보안 매트릭스/P0~P9 로드맵 전 범위 설계서.
- `V1__mc_init.sql` — `mc_` prefix 8개 테이블 DDL.
- `MemberChatProperties` / `application-member-chat.yml` — 독립 설정 네임스페이스.

### P1. 도메인 + WebSocket + Principal 강제
- `ChatRoom`, `ChatParticipant`, `ChatMessage`, `ChatMessageImage`, `ChatMessageRevision`, `ChatReadReceipt`, `ChatAuditLog`, `ChatLegalHold` + enum 전 세트.
- `StompChatConfig` — endpoint/prefix 분리, interceptor 2단(JWT → Rate/MDC) 배선.
- `ChatStompHandler` — CONNECT 시 JWT 검증 + `AuthPrincipal(userId, companyId, role)` 고정, 이후 커맨드는 Principal 없으면 거부.
- `ChatChannelInterceptor` — AntPathMatcher 기반 destination 파싱 + 방 단위 레이트리밋 + MDC.

### P2. 메시지 송/편집/삭제 + 감사
- `ChatMessageService`
  - `saveAndPublish()` — `clientMessageId` 중복 검사(멱등성) → 인가 → 저장 → 감사 → Redis publish.
  - `editMessage()` — 본인 + 24시간 이내 + revision append(EDIT).
  - `deleteMessage()` — legal hold 방에서 차단, revision append(DELETE), soft delete.
  - `history()` / `sync()` — cursor / `lastSeenMessageId` 기반 페이지네이션.
- `AuditLogService` — `@Async + REQUIRES_NEW` 로 본 트랜잭션 실패와 무관하게 기록.

### P3. 방 관리
- `ChatRoomService`
  - `getOrCreateDirectRoom()` — 기존 HIDDEN 참여자 `restoreFromHidden()` 로 부활.
  - `createGroup()`, `addMember()`, `leave()` (DIRECT→hide / GROUP→leave), `deleteRoom()` — legalHold 가드.

### P4. 읽음 영수증 (멀티 디바이스)
- `ReadReceiptService.ack(memberId, roomId, messageId, deviceId)`
  - `ChatParticipant.updateLastRead()` 단조 증가(과거 ack 무시).
  - `(messageId, memberId)` UNIQUE 로 멱등.
  - READ 이벤트를 Redis 로 publish → 구독자들 `/mc/topic/read/{roomId}` 로 브로드캐스트.

### P5. 파일 업로드/다운로드
- `FileService`
  - `presignUpload()` — 확장자/MIME/Content-Length 를 **서명에 박아서** 업로드 조건 강제.
  - `confirmUpload()` — S3 HEAD 로 실제 저장된 MIME/크기 재검증, `FileUploadedEvent` 발행.
  - `presignDownload()` — `scanStatus=CLEAN` 이어야만 1h presigned GET URL 반환.
- AV 스캔 실제 연동은 추후(`FileUploadedEvent` 소비 리스너 미구현). 현재는 이벤트 발행까지만 완료.

### P6. 관리자 기능
- `AdminChatService`
  - `search()` — `adminSearch` 쿼리 실행, 조회되는 모든 메시지를 `MESSAGE_VIEWED_BY_ADMIN` 감사.
  - `exportCsv()` — CSV 바이트 생성, `EXPORT_COMPLETED` 감사.
  - 권한: `X-User-IsSystemAdmin=true` 인 경우에만 허용.
- `LegalHoldService` — 방/개인 대상 hold 적용·해제, 활성 hold 있는 동안 방 삭제/메시지 삭제 차단.
- `ChatAdminController` — `/member-chat/admin/search`, `/export`, `/legal-hold`, `/legal-hold/{id}`.

### P7. 리텐션 배치
- `RetentionService.@Scheduled(cron = "${member-chat.retention.batch-cron}")` + `@SchedulerLock` 으로 다중 인스턴스 단일 실행 보장.
- `messageRepo.purgeOlderThan(threshold)` — legal hold 걸린 방은 JPQL subquery 로 제외.

### P8. Redis Pub/Sub → STOMP Fan-out
- `RedisChatPubSubService.publish(roomId, event)` → `member-chat:room:{roomId}` 채널.
- `RedisChatSubscriberConfig` 가 `member-chat:room:*` 패턴 구독 → `SimpMessagingTemplate` 로 로컬 인스턴스의 STOMP 구독자에게 전달.
  - 기본: `/mc/topic/room/{roomId}`
  - `type=READ` 이벤트: `/mc/topic/read/{roomId}` 로 분리 송출.

### P9. 에러 & 인가 공통화
- `ChatErrorCode` — `CHAT_4001/4011/4031~4038/4041~4043/4091~4092/4291/5001` 표준 코드.
- `ChatExceptionHandler` — REST: `@RestControllerAdvice`, STOMP: `@MessageExceptionHandler` → `/user/queue/errors`.
- `ChatAuthPolicy` — `canSubscribe()`, `requireActiveParticipant()`, `canSendMessage()` 중앙화 (REST/STOMP 공용).

### 보조
- `RateLimiter` — Redis `INCR + EXPIRE` 고정 윈도우.
- 단위 테스트 3종
  - `ChatParticipantTest` — `updateLastRead` 단조성, `canSendNotice` 권한.
  - `AuthPrincipalTest` — SystemAdmin/Auditor bypass 판정.
  - `ChatAuthPolicyTest` — Mockito 로 4가지 구독 시나리오.

---

## 5. API 요약

### 5.1 REST (`/member-chat`)

| Method | Path | 설명 |
|---|---|---|
| POST | `/rooms/direct` | 1:1 방 생성/복원 |
| POST | `/rooms/group` | 그룹 방 생성 |
| GET  | `/rooms/my` | 내가 참여 중인 방 목록 |
| POST | `/rooms/{roomId}/members` | 참여자 추가 |
| POST | `/rooms/{roomId}/leave` | 나가기 (DIRECT→hide) |
| DELETE | `/rooms/{roomId}` | 방 삭제 (legalHold 차단) |
| GET  | `/messages/{roomId}` | 히스토리 (cursor) |
| GET  | `/messages/{roomId}/sync` | 재접속 동기화 (lastSeenMessageId) |
| PATCH | `/messages/{messageId}` | 편집 |
| DELETE | `/messages/{messageId}` | 삭제 |
| POST | `/messages/{messageId}/read` | 읽음 ack |
| POST | `/files/presigned` | 업로드 URL 발급 |
| POST | `/files/confirm` | 업로드 확정 + AV 이벤트 |
| GET  | `/files/{key}` | 다운로드 프록시 (CLEAN 만) |
| GET  | `/admin/search` | 관리자 검색 (SystemAdmin) |
| POST | `/admin/export` | CSV 내보내기 |
| POST | `/admin/legal-hold` | hold 적용 |
| DELETE | `/admin/legal-hold/{id}` | hold 해제 |

### 5.2 STOMP

| Direction | Destination | 설명 |
|---|---|---|
| CONNECT | `/mc/connect` | SockJS handshake + JWT 검증 |
| SUBSCRIBE | `/mc/topic/room/{roomId}` | 방 메시지 이벤트 |
| SUBSCRIBE | `/mc/topic/read/{roomId}` | 읽음 이벤트 |
| SUBSCRIBE | `/user/queue/errors` | 개인 에러 채널 |
| SEND | `/mc/app/room/{roomId}/send` | 메시지 전송 |
| SEND | `/mc/app/room/{roomId}/read` | 읽음 ack |

SEND 바디에 `senderId` 를 넣어도 서버는 무시하고 Principal 의 userId 만 사용한다.

---

## 6. 운영 체크리스트 / 남은 일

**꼭 체크해야 하는 것**
- `S3Client` / `S3Presigner` Bean 등록 여부 — 파일 기능을 켤 때 반드시 필요.
- Flyway/Liquibase 등 마이그레이션 러너에 `V1__mc_init.sql` 반영.
- Redis, MariaDB 접속 정보 (기존 member-service 설정 재사용).
- 프런트 CORS 허용 오리진을 `member-chat.websocket.allowed-origins` 에 추가.

**여유 있을 때**
- AV 스캔 워커 listener 구현 (`FileUploadedEvent` 소비 → scanStatus=CLEAN/INFECTED 갱신).
- `@SpringBootTest` + Testcontainers(MariaDB, Redis) 로 CONNECT → SUBSCRIBE → SEND → READ → FAN-OUT E2E 테스트.
- JWT 에 세부 role(MANAGER/AUDITOR) 클레임 도입 시 `AdminChatService.requireAdmin()` 확장.
- 프런트용 STOMP 이벤트 스키마 JSON 예시 문서 추가.

---

## 7. 파일 맵 (핵심만)

```
docs/
  ├─ member-chat-design.md          전체 설계서
  └─ member-chat-overview.md        (본 문서)

member-service/src/main/
  ├─ resources/
  │   ├─ application.yml            (profiles.group overlay)
  │   ├─ application-member-chat.yml
  │   └─ db/migration/V1__mc_init.sql
  └─ java/com/_team/_team/
      ├─ member/config/SecurityConfig.java       (/mc/connect, /member-chat 허용)
      └─ memberchat/
          ├─ MemberChatProperties.java
          ├─ config/
          │   ├─ StompChatConfig.java
          │   ├─ ChatStompHandler.java           (JWT → AuthPrincipal)
          │   ├─ ChatChannelInterceptor.java    (rate-limit + MDC)
          │   ├─ ChatExceptionHandler.java
          │   └─ RedisChatSubscriberConfig.java (Redis → STOMP fan-out)
          ├─ domain/ ...                         (엔티티 13개)
          ├─ repository/ ...                     (7개)
          ├─ service/
          │   ├─ ChatAuthPolicy.java
          │   ├─ ChatMessageService.java
          │   ├─ ChatRoomService.java
          │   ├─ ReadReceiptService.java
          │   ├─ FileService.java
          │   ├─ AdminChatService.java
          │   ├─ LegalHoldService.java
          │   ├─ RetentionService.java
          │   ├─ AuditLogService.java
          │   ├─ RateLimiter.java
          │   └─ RedisChatPubSubService.java
          ├─ controller/
          │   ├─ ChatRoomController.java
          │   ├─ ChatMessageController.java
          │   ├─ ChatStompController.java
          │   ├─ ChatFileController.java
          │   └─ admin/ChatAdminController.java
          └─ error/
              ├─ ChatErrorCode.java
              └─ ChatException.java

member-service/src/test/java/com/_team/_team/memberchat/
  ├─ ChatParticipantTest.java
  ├─ AuthPrincipalTest.java
  └─ ChatAuthPolicyTest.java

gateway/src/main/java/com/_team/_team/
  └─ JwtAuthFilter.java             (X-User-IsSystemAdmin 등 헤더 주입)
```
