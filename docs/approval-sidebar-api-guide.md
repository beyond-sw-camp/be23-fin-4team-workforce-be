# 결재 사이드바 메뉴 API 가이드 (프론트 연동용)

사이드바 메뉴 항목별로 호출할 백엔드 API를 정리한 문서. 모든 엔드포인트는 approval-service 기준이며, Gateway를 통해 호출합니다.

## 공통 규약

### Base URL
```
/approval  (게이트웨이 기준 prefix)
```

### 필수 헤더
모든 요청에 아래 헤더가 필요합니다 (Gateway가 JWT 파싱 후 주입):

| Header | 설명 |
|---|---|
| `X-User-CompanyId` | 회사 UUID |
| `X-User-UUID` | 로그인한 사용자의 memberId |
| `X-User-MemberPositionId` | 로그인한 사용자의 직위 ID |

### 공통 응답 포맷
```json
{
  "success": true,
  "message": "...",
  "data": [ /* ApprovalRequestResDto[] 또는 단건 */ ]
}
```

### RequestStatus (결재 상태)
| 값 | 설명 |
|---|---|
| `DRAFT` | 임시저장 |
| `WAIT` | 결재 승인 시작 전 |
| `PENDING` | 결재 진행중 |
| `APPROVED` | 최종 승인 |
| `REJECTED` | 반려 |
| `CANCELED` | 회수/취소 |

---

## 1. 결재하기 (Inbox)

> 나에게 **액션이 필요한** 문서들을 모아보는 섹션.

### 1-1. 결재 대기 문서
지금 내 차례로 넘어온 결재 (대결 포함).

```http
GET /approval/approvals/pending
Headers: X-User-CompanyId, X-User-UUID, X-User-MemberPositionId
```

- **응답**: `ApprovalRequestResDto[]`
- **용도**: 사이드바 배지 카운트 / 결재 대기 리스트 화면

### 1-2. 결재 수신 문서
내가 결재라인에 포함된 **모든** 문서 (대기 + 진행중 + 완료/반려/회수). 현재 별도 엔드포인트가 없으므로 다음 두 API를 조합하거나 서버에 신설 요청 필요:

- 내가 이미 처리한 건: `GET /approval/approvals/acted`
- 내가 지금 처리할 건: `GET /approval/approvals/pending`

> **프론트 처리 권장**: 두 결과를 `requestId` 기준 de-dup 후 `createdAt DESC` 정렬.
> **장기적으로는** `/approval/approvals/received` 같은 통합 엔드포인트 신설이 바람직합니다.

### 1-3. 참조/열람 대기 문서
내가 참조자(CC) 또는 공람자(CIRCULATION)로 지정됐고 **아직 읽지 않은** 문서.

```http
GET /approval/viewers/cc
GET /approval/viewers/circulation
Headers: X-User-UUID
```

- **응답**: `ApprovalRequestResDto[]` (각각)
- **미읽음 필터**: 프론트에서 `ApprovalViewer.readAt == null` 기준으로 필터
- **읽음 처리**: `PATCH /approval/viewers/{viewerId}/read`

### 1-4. 결재 예정 문서
내가 결재라인에 있지만 **아직 내 차례가 아닌** 문서. 현재 별도 엔드포인트 없음.

- **임시 대응**: `GET /approval/approvals/pending` 응답에서 `Approval.approvalOrder > 현재 진행 단계` 인 항목을 프론트에서 필터 (백엔드에서 구조상 포함 안 됨 → **서버에 신설 요청 필요**: `GET /approval/approvals/upcoming`)

---

## 2. 개인 문서함

> **내가 주체**가 된 문서들을 유형별로 보관하는 섹션.

### 2-1. 기안 문서함
내가 기안한 **모든** 결재 요청.

```http
GET /approval/requests/my
GET /approval/requests/my?status={RequestStatus}
Headers: X-User-UUID
```

- **쿼리 파라미터**: `status` (optional) — 특정 상태만 필터

### 2-2. 임시 저장함
작성 중 저장만 해둔 문서.

```http
GET /approval/requests/my?status=DRAFT
Headers: X-User-UUID
```

- **수정**: `PATCH /approval/requests/{requestId}` (body: `ApprovalRequestCreateReqDto`)
- **제출**: 동일 PATCH로 `requestStatus: WAIT` 설정 (서비스 로직 참조)

### 2-3. 결재 문서함
내가 **결재자로서** 처리(승인/반려)한 문서 이력.

```http
GET /approval/approvals/acted
Headers: X-User-UUID, X-User-MemberPositionId
```

### 2-4. 참조/열람 문서함
내가 참조자(CC) 또는 공람자(CIRCULATION)로 지정된 **모든** 문서 (읽음 여부 무관).

```http
GET /approval/viewers/cc
GET /approval/viewers/circulation
Headers: X-User-UUID
```

> 2-3의 "대기" 화면과 동일 엔드포인트. 프론트에서 필터/탭으로 구분.

### 2-5. 수신 문서함
내가 **어떤 식으로든 관여된** 결재 문서 전체 (결재자/참조자/공람자).

- 현재 단일 엔드포인트 없음. 아래 조합으로 구성:
  - `GET /approval/approvals/pending` + `/acted`
  - `GET /approval/viewers/cc` + `/circulation`
- **프론트 처리**: `requestId` 기준 de-dup, 중복 시 우선순위는 결재자 > 참조자 > 공람자.
- **권장**: `GET /approval/requests/inbox` 신설 요청.

### 2-6. 발송 문서함
내가 기안해서 **실제로 발송된** 문서 (임시저장 제외).

```http
GET /approval/requests/my
Headers: X-User-UUID
```

- **프론트 필터**: `status != DRAFT` 로 필터
- 또는 상태별 반복 호출: `?status=WAIT`, `?status=PENDING`, `?status=APPROVED`, `?status=REJECTED`, `?status=CANCELED`

### 2-7. 부재 위임 (대결)
내 결재 권한을 다른 사람에게 위임 / 내가 위임받은 권한 관리.

| 기능 | Method | Path |
|---|---|---|
| 위임 등록 | `POST` | `/approval/absence-proxy` |
| 내가 등록한 위임 | `GET` | `/approval/absence-proxy/my` |
| 내가 위임받은 목록 | `GET` | `/approval/absence-proxy/delegated` |
| 위임 취소 | `PATCH` | `/approval/absence-proxy/{proxyId}/deactivate` |

**등록 요청 Body (`AbsenceProxyCreateReqDto`)**:
```json
{
  "delegateMemberId": "uuid",
  "startDate": "2026-04-20",
  "endDate": "2026-04-25",
  "reason": "연차"
}
```

### 2-8. 공문 문서함
공문 타입(`RequestType.OFFICIAL` 또는 유사)의 문서.

- 현재 `RequestType` 필터 쿼리 파라미터가 `/approval/requests/my` 에 없음.
- **프론트 임시 대응**: `/approval/requests/my` 응답을 받아 `documentType`/`requestType` 으로 필터.
- **권장**: `GET /approval/requests/my?requestType=OFFICIAL` 쿼리 지원 추가 요청.

---

## 3. 부서 문서함

> 본인이 속한 **부서 단위**로 모아보는 섹션.
> ⚠️ 최근 변경: 하위 조직 포함 X, **본인 소속 부서만** 조회 가능 (참고: `dept-document-scope-change-memo.md`)

### 3-1. 기안 완료함
부서원들이 기안해서 **완료/반려된** 문서.

```http
GET /approval/requests/department?organizationId={본인부서UUID}
Headers: X-User-CompanyId, X-User-MemberPositionId
```

- **서버 필터 조건** (자동 적용):
  - `isDeptVisibleYn = 'Y'` (민감 양식 제외)
  - `requestStatus ∈ {APPROVED, REJECTED}`
  - `requesterOrganizationId = 본인 부서`
- **403 FORBIDDEN**: `organizationId`가 본인 소속 부서와 불일치 시
- **응답**: 최신순(`createdAt DESC`) 정렬

### 3-2. 부서 참조함
부서가 참조처로 지정된 문서. 현재 별도 엔드포인트 없음.

- **현재 API로 구현 불가** — `ApprovalViewer`는 memberId 단위로만 저장되어 부서 단위 CC 조회 미지원.
- **권장**: 서버에 `GET /approval/viewers/department?organizationId=...` 신설 요청.

### 3-3. 공문 발송함
부서 단위로 발송한 공문.

- 현재 별도 엔드포인트 없음.
- **프론트 임시 대응**: `/approval/requests/department` 응답을 공문 타입으로 필터.
- **권장**: `GET /approval/requests/department?requestType=OFFICIAL` 쿼리 지원 추가 요청.

---

## 4. 공통 상세/액션 API

어느 리스트 화면에서 항목을 클릭했을 때 사용하는 API.

### 4-1. 결재 상세 조회
```http
GET /approval/requests/{requestId}
Headers: X-User-CompanyId
```

### 4-2. 결재 승인/반려 (결재자 본인)
```http
PATCH /approval/approvals/{approvalId}/approve
PATCH /approval/approvals/{approvalId}/reject
Headers: X-User-CompanyId, X-User-UUID, X-User-MemberPositionId
Body: { "comment": "..." }  // ApprovalActionReqDto
```

### 4-3. 결재 회수 (기안자 본인)
```http
PATCH /approval/requests/{requestId}/cancel
Headers: X-User-CompanyId, X-User-UUID
Body: { "cancelReason": "..." }
```

### 4-4. 첨부파일
| 기능 | Method | Path |
|---|---|---|
| 업로드 | `POST` | `/approval/attachments/{requestId}` (multipart) |
| 목록 | `GET` | `/approval/attachments/{requestId}` |
| 삭제 | `DELETE` | `/approval/attachments/{attachmentId}` |

### 4-5. 참조/공람 읽음 처리
```http
PATCH /approval/viewers/{viewerId}/read
Headers: X-User-UUID
```

---

## 5. 양식·결재라인 조회 (작성 화면용)

기안 작성 화면에서 필요한 메타 API.

| 기능 | Method | Path |
|---|---|---|
| 활성 양식 목록 | `GET` | `/approval/documents/active` |
| 양식 상세 | `GET` | `/approval/documents/{documentId}` |
| 양식별 결재라인 정책 | `GET` | `/approval/policyLines/{documentId}` |
| 양식별 후보 결재자 | `GET` | `/approval/policyLines/{documentId}/candidates` |

### 5-1. 결재 요청 생성
```http
POST /approval/requests
Headers: X-User-CompanyId, X-User-UUID, X-User-MemberPositionId
Body: ApprovalRequestCreateReqDto
```

---

## 6. 메뉴 → API 매핑 요약표

| 메뉴 | 주 API | 비고 |
|---|---|---|
| 결재하기 › 결재 대기 문서 | `GET /approval/approvals/pending` | |
| 결재하기 › 결재 수신 문서 | `pending` + `acted` 조합 | 통합 API 신설 권장 |
| 결재하기 › 참조/열람 대기 문서 | `GET /approval/viewers/cc` + `/circulation` | `readAt == null` 필터 |
| 결재하기 › 결재 예정 문서 | (없음) | 서버 신설 필요 |
| 개인 › 기안 문서함 | `GET /approval/requests/my` | |
| 개인 › 임시 저장함 | `GET /approval/requests/my?status=DRAFT` | |
| 개인 › 결재 문서함 | `GET /approval/approvals/acted` | |
| 개인 › 참조/열람 문서함 | `GET /approval/viewers/cc` + `/circulation` | |
| 개인 › 수신 문서함 | 4개 API 조합 | 통합 API 신설 권장 |
| 개인 › 발송 문서함 | `GET /approval/requests/my` (DRAFT 제외) | |
| 개인 › 부재 위임(대결) | `GET /approval/absence-proxy/my` + `/delegated` | |
| 개인 › 공문 문서함 | `GET /approval/requests/my` + 프론트 필터 | requestType 쿼리 추가 권장 |
| 부서 › 기안 완료함 | `GET /approval/requests/department` | 본인 부서만 |
| 부서 › 부서 참조함 | (없음) | 서버 신설 필요 |
| 부서 › 공문 발송함 | `GET /approval/requests/department` + 프론트 필터 | requestType 쿼리 추가 권장 |

---

## 7. 프론트 구현 참고 팁

1. **공통 Axios 인스턴스** 에서 3종 헤더를 자동 주입하세요 (`X-User-CompanyId`, `X-User-UUID`, `X-User-MemberPositionId`).
2. **사이드바 배지** 는 다음만으로 충분히 계산 가능:
   - 결재 대기: `pending.length`
   - 참조/열람 대기: `cc.filter(readAt==null).length + circulation.filter(readAt==null).length`
3. **중복 제거**는 항상 `requestId` 기준.
4. **정렬**은 대부분 서버가 `createdAt DESC` 로 반환하지만, 조합 API에선 프론트에서 재정렬 필요.
5. **실시간 갱신**은 기존 SSE 알림(`targetType === "APPROVAL"`) 수신 시 해당 리스트를 refetch.

---

## 8. 신설 요청 후보 (서버팀과 협의)

| 엔드포인트 제안 | 용도 |
|---|---|
| `GET /approval/approvals/upcoming` | 결재 예정 (내 차례 전) |
| `GET /approval/requests/inbox` | 수신 문서함 통합 |
| `GET /approval/viewers/department` | 부서 참조함 |
| `GET /approval/requests/my?requestType=` | 공문 문서함 필터 |
| `GET /approval/requests/department?requestType=` | 부서 공문 발송함 필터 |

---

_작성일: 2026-04-15_
