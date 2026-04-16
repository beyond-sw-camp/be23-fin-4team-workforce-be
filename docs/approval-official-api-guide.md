# 공문(OFFICIAL) 프론트 연동 API 가이드

미연결된 3개 화면용 API 정리. 게이트웨이가 토큰 → 아래 헤더로 주입해주므로 프론트에서는 헤더를 직접 세팅하지 않음 (gateway 통과 전 proxy/mock 환경이면 직접 주입 필요).

## 공통 헤더
| 헤더 | 설명 |
| --- | --- |
| `X-User-CompanyId` | 회사 UUID |
| `X-User-UUID` | 로그인 사용자(member) UUID |
| `X-User-MemberPositionId` | 현재 직위 UUID |

## 공통 응답 래퍼
```json
{
  "status": 200,
  "message": "...",
  "data": <실제 payload>
}
```

## 공통 ApprovalRequest 응답 스키마 (목록/상세 공통)
```ts
type ApprovalRequestResDto = {
  requestId: string;
  documentId: string;
  documentName: string;             // "공문"
  memberId: string;                 // 기안자
  requestType: "OFFICIAL" | ...;
  contentJson: string;              // 양식 렌더용
  requestStatus: "DRAFT" | "WAIT" | "PENDING" | "APPROVED" | "REJECTED" | "CANCELED";
  cancelReason: string | null;
  createdAt: string;                // ISO
  updatedAt: string;
  documentNumber: string | null;    // "인사-2026-0147" (최종승인 후 발번)
  recipients: {                     // 공문일 때만 채워짐
    recipientId: string;
    recipientOrganizationId: string;
    recipientOrganizationName: string;
  }[] | null;
  requesterName: string;
  requesterOrganizationId: string;
  requesterOrganizationName: string;
  approvalLines: ApprovalLineResDto[];
  viewers: ApprovalViewerResDto[];
};
```

---

## 1. 개인 문서함 > 공문 문서함

**의미**: 내가 기안한 공문 전체 (DRAFT~APPROVED~REJECTED~CANCELED 모두).

```
GET /approval/requests/my?requestType=OFFICIAL
```

### Query Params
| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `requestType` | enum | O | `OFFICIAL` 고정 |
| `status` | enum | X | `DRAFT` / `WAIT` / `PENDING` / `APPROVED` / `REJECTED` / `CANCELED` (탭 필터용) |

### Example
```
GET /approval/requests/my?requestType=OFFICIAL
GET /approval/requests/my?requestType=OFFICIAL&status=APPROVED
```

### Response
```json
{
  "status": 200,
  "message": "내 결재 목록 조회 성공",
  "data": [ ApprovalRequestResDto, ... ]
}
```

### 프론트 가이드
- 탭이 (전체 / 임시저장 / 진행중 / 완료) 구조라면 `status` 값만 바꿔서 재호출.
- `진행중` 탭 = `PENDING` / `완료` 탭 = `APPROVED` / `반려` 탭 = `REJECTED`.

---

## 2. 부서 문서함 > 부서 수신함 (공문 수신함)

**의미**: 내 소속 부서가 **수신처로 지정된** 공문 목록. 최종 승인(APPROVED)된 건만 노출됨.

```
GET /approval/requests/official/received
```

### Query Params
없음. 서버가 `X-User-MemberPositionId`로 내 부서를 조회해서 필터.

### Response
```json
{
  "status": 200,
  "message": "공문 수신함 조회 성공",
  "data": [ ApprovalRequestResDto, ... ]
}
```

### 프론트 가이드
- 리스트에서 보여줄 주요 컬럼 후보: `documentNumber`, `requesterOrganizationName`, `requesterName`, `createdAt`, `recipients[*].recipientOrganizationName`.
- 상세 진입은 공통 상세 API 사용 → `GET /approval/requests/{requestId}` (접근 권한 검증 포함).

---

## 3. 공문 발송함

**의미**: 내가 기안한 공문 중 **최종 승인된 것** (발송 완료된 문서).

> 별도 엔드포인트가 없고 `/my` 를 재활용합니다.

```
GET /approval/requests/my?requestType=OFFICIAL&status=APPROVED
```

### Response
공통 `ApprovalRequestResDto[]`. `documentNumber` 가 반드시 채워져 있음.

### 프론트 가이드
- "발송함" 이라는 UI 의미 = APPROVED + OFFICIAL. 필터만 이렇게 걸면 됨.
- 진행중인 공문(PENDING)까지 한 화면에서 보고 싶다면 `status` 를 빼고 `/my?requestType=OFFICIAL` 로 호출한 뒤 클라이언트에서 탭 나누기.

---

## 상세 조회 / 취소 (참고 — 기존 API 그대로)

```
GET    /approval/requests/{requestId}           # 상세 (권한 검증 포함)
PATCH  /approval/requests/{requestId}           # 임시저장 수정 (DRAFT만)
PATCH  /approval/requests/{requestId}/cancel    # 취소 (DRAFT/WAIT만)
```

상세 접근 가능 대상(공문):
1. 기안자
2. 결재라인에 포함된 결재자
3. 참조/공람자
4. 수신부서 소속원 (**APPROVED일 때만 2번 엔드포인트에 노출되지만, 상세 API도 같은 규칙 적용 필요**)

권한 없으면 `403 FORBIDDEN: 열람 권한이 없습니다.` 응답.

---

## 공문 작성 (참고 — 추가 필드)

```
POST /approval/requests
```

공문일 때 추가로 필요한 필드:

```json
{
  "documentId": "<공문 양식 documentId>",
  "contentJson": "{\"title\":\"...\",\"content\":\"...\",\"note\":\"...\"}",
  "requestStatus": "WAIT",
  "approvalLines": [ { "stepOrder":1, "approverMemberId":"...", "approverMemberPositionId":"..." } ],
  "viewers": [ ... ],
  "recipients": [
    {
      "recipientOrganizationId": "<부서 UUID>",
      "recipientOrganizationName": "인사팀"
    }
  ]
}
```

- `recipients` 최소 1개 필수 (없으면 400).
- 같은 부서 중복 지정 불가 (400).
- `documentNumber`는 **최종 승인 시점**에 서버가 자동 발번 (`{부서2자}-{YYYY}-{seq4}`, 예: `인사-2026-0147`).

---

## 에러 응답 공통 포맷
```json
{
  "status": 400,
  "message": "공문은 수신 부서가 최소 1개 필요합니다.",
  "data": null
}
```

| 상태 | 케이스 |
| --- | --- |
| 400 | 수신부서 누락/중복, 상태값 오류, stepOrder 불연속 |
| 403 | 회사 불일치, 타인 결재건 수정/취소, 공문 열람권한 없음 |
| 404 | requestId / documentId 없음, 요청자 정보 조회 실패 |

---

## 요약 매핑

| 사이드바 메뉴 | API |
| --- | --- |
| 개인 문서함 > 공문 문서함 | `GET /approval/requests/my?requestType=OFFICIAL` |
| 부서 문서함 > 부서 수신함 | `GET /approval/requests/official/received` |
| 공문 발송함 | `GET /approval/requests/my?requestType=OFFICIAL&status=APPROVED` |
