# 결재 알림 프론트엔드 연동 가이드

결재(Approval) 도메인에서 발행되는 알림을 프론트에서 어떻게 받고 표시할지 정리한 문서입니다.
알림 자체는 **member-service** 가 책임지고, 결재 액션은 **approval-service** 가 책임집니다.

---

## 1. 공통 사항

### 1-1. 인증 헤더
모든 호출에는 게이트웨이를 거치면서 아래 헤더가 주입됩니다. 프론트가 직접 넣지 않아도 됩니다.

| Header | 설명 |
|---|---|
| `X-User-UUID` | 로그인 사용자 memberId (UUID) |
| `X-User-CompanyId` | 회사 ID |
| `X-User-MemberPositionId` | 직위 ID |

### 1-2. 응답 포맷
모든 응답은 `ApiResponse` 래퍼로 내려갑니다.

```json
{
  "status": "success",
  "message": "결재 요청이 생성되었습니다.",
  "data": { ... }
}
```

### 1-3. NotificationType (결재 관련만)
| 값 | 발생 시점 | 수신자 |
|---|---|---|
| `APPROVAL_REQUESTED` | 새 결재 요청 / 다음 결재자 차례 도래 | 현재 차례 결재자 1명 |
| `APPROVAL_APPROVED` | 최종 승인 완료 / 자동승인 즉시 | 요청자 + 참조자 |
| `APPROVAL_REJECTED` | 어떤 단계에서든 반려 | 요청자 + 참조자 |
| `APPROVAL_CANCELED` | 요청자가 회수(cancel) | 알림 받았던 결재자 + 참조자 |
| `APPROVAL_REFERENCED` | 결재 요청 시점 | 참조자(CC) |
| `APPROVAL_CIRCULATED` | 최종 승인/자동승인 시점 | 공람자(CIRCULATION) |

> 중복 수신자는 백엔드에서 자동 dedup (요청자 > 참조자 > 공람자 우선순위).
> 즉, 같은 사람이 여러 자격을 가져도 알림은 1건만 갑니다.

---

## 2. 알림 수신 (member-service)

Base path: `/notification`

### 2-1. SSE 구독 — 실시간 알림 수신
```
GET /notification/subscribe
Accept: text/event-stream
```

**프론트 예시 (EventSource)**
```js
const es = new EventSource('/notification/subscribe', { withCredentials: true });

es.addEventListener('notification', (e) => {
  const noti = JSON.parse(e.data);
  // noti = NotificationResDto
  showToast(noti);
  refreshUnreadCount();
});

es.onerror = () => {
  // 자동 재연결됨. Last-Event-ID 헤더로 누락 알림 복구
};
```

> EventSource는 헤더 커스터마이징이 안 되므로, 게이트웨이/프록시에서 쿠키→헤더 변환이 동작해야 합니다.
> 또는 `fetch` + ReadableStream 기반 polyfill 사용.

### 2-2. 알림 목록 조회
```
GET /notification/list
```

**Response.data** (배열)
```json
[
  {
    "notificationId": "uuid",
    "notificationType": "APPROVAL_REQUESTED",
    "content": "[휴가신청서] 결재 요청이 도착했습니다.",
    "targetId": "uuid (= requestId)",
    "targetType": "APPROVAL_REQUEST",
    "isRead": "N",
    "createdAt": "2026-04-14T10:23:11"
  }
]
```

### 2-3. 안 읽은 알림 수
```
GET /notification/unread-count
```
**Response.data**: `number`

### 2-4. 단건 읽음 처리
```
PATCH /notification/{notificationId}/read
```

### 2-5. 전체 읽음 처리
```
PATCH /notification/read-all
```

### 2-6. 알림 클릭 → 결재 상세 라우팅
`targetType === "APPROVAL_REQUEST"` 인 경우:

```js
function onClickNotification(noti) {
  markAsRead(noti.notificationId);

  if (noti.targetType === 'APPROVAL_REQUEST') {
    router.push(`/approval/requests/${noti.targetId}`);
  }
}
```

타입별 추천 진입 화면:

| Type | 권장 라우팅 |
|---|---|
| `APPROVAL_REQUESTED` | 결재 대기함 상세 (결재 액션 가능) |
| `APPROVAL_APPROVED` / `APPROVAL_REJECTED` / `APPROVAL_CANCELED` | 내가 올린 결재 상세 (read-only) |
| `APPROVAL_REFERENCED` / `APPROVAL_CIRCULATED` | 결재 상세 (read-only) |

---

## 3. 결재 액션 (approval-service) — 알림 트리거 API

Base path: `/approval`

### 3-1. 결재 요청 생성 → `APPROVAL_REQUESTED` + `APPROVAL_REFERENCED` 발행
```
POST /approval/requests
Content-Type: application/json
```

**Request Body**
```json
{
  "documentId": "uuid",
  "contentJson": "{...폼데이터 JSON 문자열...}",
  "requestStatus": "WAIT",          // DRAFT 면 알림 미발행
  "approvalLines": [
    { "stepOrder": 1, "approverMemberId": "uuid", "approverMemberPositionId": "uuid" },
    { "stepOrder": 2, "approverMemberId": "uuid", "approverMemberPositionId": "uuid" }
  ],
  "viewers": [
    { "viewerMemberId": "uuid", "viewerMemberPositionId": "uuid", "viewerType": "CC" },
    { "viewerMemberId": "uuid", "viewerMemberPositionId": "uuid", "viewerType": "CIRCULATION" }
  ]
}
```

**알림 동작**
- `requestStatus = WAIT`: 첫 결재자에게 `APPROVAL_REQUESTED`, 참조자에게 `APPROVAL_REFERENCED`
- `requestStatus = WAIT` + 자동승인 문서: 요청자/참조자에게 `APPROVAL_APPROVED`, 공람자에게 `APPROVAL_CIRCULATED` 즉시
- `requestStatus = DRAFT`: 알림 없음

### 3-2. 임시저장 수정
```
PATCH /approval/requests/{requestId}
```
> DRAFT → WAIT 전환은 알림 발행 안 함 (현재 정책). WAIT 진입은 신규 생성으로만 가능.

### 3-3. 결재 회수 → `APPROVAL_CANCELED` 발행
```
PATCH /approval/requests/{requestId}/cancel
```
**Request Body**
```json
{ "cancelReason": "오타가 있어 다시 작성합니다." }
```

**알림 동작**
- WAIT 상태에서만 회수 가능 → 첫 결재자 + 참조자에게 `APPROVAL_CANCELED`
- PENDING 이후는 400/409 에러 (회수 불가)

### 3-4. 결재 승인 → `APPROVAL_REQUESTED` (다음 차례) 또는 `APPROVAL_APPROVED` (최종)
```
PATCH /approval/approvals/{approvalId}/approve
```
**Request Body**
```json
{ "comment": "확인했습니다." }   // 선택
```

**알림 동작**
- 다음 결재자가 있으면 → 다음 결재자에게 `APPROVAL_REQUESTED`
- 최종 승인이면 → 요청자/참조자에게 `APPROVAL_APPROVED`, 공람자에게 `APPROVAL_CIRCULATED`

### 3-5. 결재 반려 → `APPROVAL_REJECTED` 발행
```
PATCH /approval/approvals/{approvalId}/reject
```
**Request Body**
```json
{ "comment": "예산 초과로 반려합니다." }   // 필수
```

**알림 동작**
- 요청자 + 참조자에게 `APPROVAL_REJECTED` (반려 사유는 `content` 안에 포함)
- 공람자, 다른 결재자에게는 알림 없음

---

## 4. 결재 조회 API (참고)

| Method | URL | 설명 |
|---|---|---|
| GET | `/approval/requests/{requestId}` | 결재 상세 |
| GET | `/approval/requests/my?status=WAIT\|PENDING\|APPROVED\|REJECTED\|CANCELED` | 내가 올린 결재 목록 |
| GET | `/approval/requests/department?organizationId=uuid` | 부서 문서함 |
| GET | `/approval/approvals/pending` | 결재 대기함 (내 차례) |
| GET | `/approval/approvals/acted` | 결재 완료함 (내가 처리한 것) |

상세 응답 (`ApprovalRequestResDto`) 주요 필드:

| 필드 | 설명 |
|---|---|
| `requestId` | 결재 요청 ID (= 알림 `targetId`) |
| `documentName` | 양식명 (e.g. "휴가신청서") |
| `requesterName`, `requesterOrganizationName` | 요청자 스냅샷 |
| `requestStatus` | DRAFT / WAIT / PENDING / APPROVED / REJECTED / CANCELED |
| `cancelReason` | 회수 사유 |
| `approvalLines[]` | 결재선 (stepOrder, 결재자, lineStatus, comment, actedAt) |
| `viewers[]` | 참조/공람자 (viewerType, viewerReadStatus) |

---

## 5. 프론트 구현 체크리스트

- [ ] 로그인 직후 `/notification/subscribe` SSE 연결 + `/notification/unread-count` 호출
- [ ] 헤더 종 아이콘에 unreadCount 배지 표시
- [ ] SSE 이벤트 수신 시 토스트 + unreadCount + 1
- [ ] 알림 드롭다운: `/notification/list` 호출
- [ ] 알림 클릭: 읽음 처리 + `targetId` 로 결재 상세 라우팅
- [ ] 로그아웃 시 SSE 연결 close
- [ ] SSE 끊김 대비: 재연결 + `Last-Event-ID` 헤더로 누락 보충

---

## 6. 에러 응답 예시

```json
{
  "status": "error",
  "message": "이미 진행 중인 결재라 회수할 수 없습니다.",
  "data": null
}
```

주요 결재 에러:
- `400` 회수 불가 상태 (PENDING 이후)
- `400` 반려 시 comment 누락
- `403` 권한 없음 (남의 결재 회수, 본인 차례 아닌 결재 처리)
- `404` requestId/approvalId 없음
- `409` 동시성 충돌 (추후 @Version 적용 시)
