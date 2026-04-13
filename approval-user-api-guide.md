# 전자결재 사용자 화면 - 프론트엔드 API 가이드

## 공통 헤더
Gateway가 주입하는 헤더:
```
X-User-CompanyId: UUID
X-User-UUID: UUID                 # 사용자 memberId
X-User-MemberPositionId: UUID     # 사용자 직위 ID
```

## 공통 응답 형식
```json
{
  "status": "SUCCESS",
  "message": "메시지",
  "data": { ... }
}
```

---

## 사용자 흐름 요약

```
1. 결재 요청 작성
   양식 선택 → 내용 입력 → 후보 결재자 조회 → 결재자 선택
   → 참조자/공람자 검색 및 선택 → 임시저장 or 제출

2. 내 결재 목록 (요청자)
   내가 올린 결재 조회 → 상세 보기 → 취소 가능 (DRAFT, WAIT 상태만)

3. 결재 처리 (결재자)
   결재 대기함 → 상세 보기 → 승인 or 반려
   (대결자도 부재자의 결재건을 동일하게 처리 가능)

4. 결재 완료함 (결재자)
   내가 처리한 결재 이력 + 대결로 처리한 이력 조회

5. 부서 문서함
   본인 부서 + 하위 조직의 최종 처리된 결재 모아보기
   (승인 완료 / 반려된 결재만, 민감 양식 제외)

6. 부재 위임 (대결)
   부재 시 대리결재자 등록/조회/취소
```

## 참조자(CC) vs 공람자(CIRCULATION) 차이

| 구분 | 설명 | 열람 시점 |
|------|------|-----------|
| CC (참조) | 결재 진행 상태와 무관하게 언제든 열람 가능 | 결재 요청 시점부터 바로 |
| CIRCULATION (공람) | 결재가 최종 승인된 후 열람 | 결재 완료 후 |

참조자/공람자는 결재에 관여하지 않고 내용만 확인하는 역할이에요.

---

## 1. 결재 요청 작성

### 1-1. 활성 양식 목록 조회
결재 요청 시 사용할 수 있는 양식 목록.

```
GET /approval/documents/active
Headers: X-User-CompanyId
```

**응답 (data):**
```json
[
  {
    "documentId": "UUID",
    "companyId": "UUID",
    "documentName": "연차신청서",
    "formSchema": "{\"fields\":[...]}",
    "isActiveYn": "Y",
    "requestType": "VACATION",
    "autoApproveYn": "N",
    "createdAt": "2026-04-09T10:00:00",
    "updatedAt": "2026-04-09T10:00:00"
  }
]
```

프론트에서 `formSchema`를 파싱해서 동적으로 입력 폼을 렌더링해야 해요.

### 1-2. 후보 결재자 조회
양식을 선택한 후, 정책라인 기준으로 결재자 후보 목록 조회. autoApproveYn="Y"인 양식이면 이 API를 호출할 필요 없어요.

```
GET /approval/policyLines/{documentId}/candidates
Headers: X-User-CompanyId, X-User-MemberPositionId
```

**응답 (data):**
```json
[
  {
    "policyLineId": "UUID",
    "documentId": "UUID",
    "jobTitleId": "UUID",
    "stepOrder": 1,
    "organizationId": null,
    "candidates": [
      {
        "memberPositionId": "UUID",
        "memberId": "UUID",
        "memberName": "김과장",
        "organizationId": "UUID",
        "organizationName": "개발팀",
        "jobTitleId": "UUID",
        "jobTitleName": "과장",
        "jobGradeId": "UUID",
        "jobGradeName": "3급"
      },
      {
        "memberPositionId": "UUID",
        "memberId": "UUID",
        "memberName": "이과장",
        "organizationId": "UUID",
        "organizationName": "개발팀",
        "jobTitleId": "UUID",
        "jobTitleName": "과장",
        "jobGradeId": "UUID",
        "jobGradeName": "3급"
      }
    ]
  },
  {
    "policyLineId": "UUID",
    "documentId": "UUID",
    "jobTitleId": "UUID",
    "stepOrder": 2,
    "organizationId": null,
    "candidates": [
      {
        "memberPositionId": "UUID",
        "memberId": "UUID",
        "memberName": "최부장",
        "organizationId": "UUID",
        "organizationName": "개발팀",
        "jobTitleId": "UUID",
        "jobTitleName": "부장",
        "jobGradeId": "UUID",
        "jobGradeName": "2급"
      }
    ]
  }
]
```

프론트에서 각 stepOrder별로 candidates 중 1명을 선택하게 해야 해요. 요청자 본인의 직책이 정책라인에 포함되어 있으면 그 단계 이하는 자동으로 필터링되어 안 나와요.

### 1-3. 참조자/공람자 선택 - 직원 목록 조회
참조자(CC)나 공람자(CIRCULATION)를 선택하려면 직원 목록에서 검색/선택해야 해요. 정책라인 기반 후보가 아니라 회사 전체 직원 중에서 자유롭게 선택 가능.

```
GET /member/list
Headers: X-User-UUID, X-User-MemberPositionId
권한: @CheckPermission(MEMBER, READ)
```

**응답 (data):**
```json
[
  {
    "memberId": "UUID",
    "memberPositionId": "UUID",
    "name": "김사원",
    "email": "kim@example.com",
    "sabun": "E001",
    "organizationName": "개발팀",
    "jobTitleName": "사원",
    "jobGradeName": "5급",
    "profileUrl": "...",
    ...
  }
]
```

프론트에서 이 목록으로 직원을 검색/필터링한 후 참조자 또는 공람자로 선택하게 해요. 결재 요청 생성 시 사용할 필드: `memberId`, `memberPositionId`, `name`, `organizationName`, `jobTitleName`

### 1-4. 결재 요청 생성 (임시저장 or 제출)
```
POST /approval/requests
Headers: X-User-CompanyId, X-User-UUID
Content-Type: application/json
```

**요청 Body:**
```json
{
  "documentId": "UUID",
  "contentJson": "{\"vacationType\":\"연차\",\"startDate\":\"2026-04-10\",\"endDate\":\"2026-04-11\",\"reason\":\"개인 사유\"}",
  "requestStatus": "WAIT",
  "approvalLines": [
    {
      "stepOrder": 1,
      "approverMemberId": "UUID",
      "approverMemberPositionId": "UUID"
    },
    {
      "stepOrder": 2,
      "approverMemberId": "UUID",
      "approverMemberPositionId": "UUID"
    }
  ],
  "viewers": [
    {
      "viewerMemberId": "UUID",
      "viewerMemberPositionId": "UUID",
      "viewerType": "CC"
    },
    {
      "viewerMemberId": "UUID",
      "viewerMemberPositionId": "UUID",
      "viewerType": "CIRCULATION"
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| documentId | UUID | O | 양식 ID |
| contentJson | String(JSON) | O | formSchema에 맞춰 사용자가 입력한 데이터 |
| requestStatus | String | O | `DRAFT` (임시저장) 또는 `WAIT` (제출) |
| approvalLines | List | 조건부 | WAIT 제출 시 최소 1명 필수 (자동승인 양식 제외) |
| approvalLines[].stepOrder | Integer | O | 결재 순서 (1부터 연속) |
| approvalLines[].approverMemberId | UUID | O | 결재자 memberId |
| approvalLines[].approverMemberPositionId | UUID | O | 결재자 memberPositionId |
| viewers | List | X | 참조/공람자 (선택) |
| viewers[].viewerMemberId | UUID | O | 참조/공람자 memberId (직원 목록에서 선택) |
| viewers[].viewerMemberPositionId | UUID | O | 참조/공람자 memberPositionId (직원 목록에서 선택) |
| viewers[].viewerType | String | O | `CC` (참조) 또는 `CIRCULATION` (공람) |

**참조/공람자 선택 방법:**
- 1-3 직원 목록 API로 전체 직원 조회 → 검색/필터링
- 각 직원을 CC 또는 CIRCULATION으로 추가
- 동일 직원을 동일 타입으로 중복 지정 불가 (memberId + viewerType 조합 중복 불가)
- 동일 직원을 CC와 CIRCULATION 각각 하나씩은 가능
- 본인을 참조/공람자로 지정하는 것도 가능 (백엔드 제한 없음)

**프론트 UI 권장 사항:**
- 결재 요청 작성 화면에서 "참조자 추가" / "공람자 추가" 버튼 분리
- 각 버튼 클릭 시 직원 검색 모달 열기 (이름, 부서, 직급으로 검색)
- 선택된 참조자/공람자 태그 형태로 표시 (이름 + 부서/직책) + 삭제(x) 버튼
- 목록에서 제거 가능

**검증 규칙:**
- requestStatus는 DRAFT 또는 WAIT만 가능
- WAIT(제출) 시 approvalLines 최소 1명 필수 (autoApproveYn="Y" 양식 제외)
- 결재자 중복 불가
- 참조/공람자 중복 불가 (memberId + viewerType 조합)
- stepOrder 1부터 연속

**제출(WAIT) 시 동작:**
- 1번째 결재자: LineStatus = PENDING (결재 대기)
- 2번째 이후: LineStatus = WAITING (아직 차례 아님)
- autoApproveYn="Y"인 양식: 결재라인 없이 바로 APPROVED 처리

**응답 (data):** ApprovalRequestResDto (아래 응답 형식 참고)

### 1-5. 결재 문서 임시저장 (DRAFT)
작성 중 결재 문서를 저장만 하고 결재는 나중에 올리고 싶을 때 사용. 동일한 `POST /approval/requests`를 호출하되 `requestStatus`를 `DRAFT`로 지정해요.

```
POST /approval/requests
Headers: X-User-CompanyId, X-User-UUID
Content-Type: application/json
```

**요청 Body (최소):**
```json
{
  "documentId": "UUID",
  "contentJson": "{\"vacationType\":\"연차\",\"startDate\":\"2026-04-10\"}",
  "requestStatus": "DRAFT"
}
```

**요청 Body (결재라인/참조자 포함):**
```json
{
  "documentId": "UUID",
  "contentJson": "{\"vacationType\":\"연차\",\"startDate\":\"2026-04-10\",\"endDate\":\"2026-04-11\",\"reason\":\"개인 사유\"}",
  "requestStatus": "DRAFT",
  "approvalLines": [
    { "stepOrder": 1, "approverMemberId": "UUID", "approverMemberPositionId": "UUID" }
  ],
  "viewers": [
    { "viewerMemberId": "UUID", "viewerMemberPositionId": "UUID", "viewerType": "CC" }
  ]
}
```

**임시저장 vs 제출(WAIT) 차이:**

| 구분 | DRAFT (임시저장) | WAIT (제출) |
|------|------|------|
| requestStatus | `DRAFT` | `WAIT` |
| approvalLines 필수 | X (비어있어도 됨) | O (최소 1명, autoApprove 제외) |
| contentJson 검증 | 비어있거나 일부만 입력해도 허용 | formSchema 기반 필수값 검증 권장 |
| 결재자 알림 | 발송 안 함 | 1번째 결재자에게 알림 |
| 수정 가능 여부 | 가능 (반복 수정) | 불가 (취소 후 재작성만) |
| 본인만 조회 | O | 결재자/참조자도 조회 |

**동작:**
- 결재요청 저장 (requestStatus=DRAFT)
- approvalLines가 있으면 함께 저장되지만 LineStatus는 모두 `WAITING` (알림 발송 X)
- viewers가 있으면 함께 저장되지만 참조자도 아직 열람 대상 아님
- 임시저장 문서는 작성자 본인만 `GET /approval/requests/my?status=DRAFT`에서 조회 가능

**프론트 UI 권장:**
- 작성 화면 하단에 `[임시저장]` / `[결재 올리기]` 버튼 분리
- 임시저장 버튼 → `requestStatus=DRAFT`로 POST
- 결재 올리기 버튼 → `requestStatus=WAIT`로 POST (또는 임시저장 후 PATCH로 WAIT 전환)
- 자동 임시저장(auto-save): 일정 주기로 `PATCH /approval/requests/{requestId}` 호출 고려

---

## 2. 내 결재 목록 (요청자 입장)

### 2-1. 내가 올린 결재 전체 목록
```
GET /approval/requests/my
Headers: X-User-UUID
```

### 2-2. 내가 올린 결재 상태별 필터
```
GET /approval/requests/my?status=DRAFT
GET /approval/requests/my?status=WAIT
GET /approval/requests/my?status=PENDING
GET /approval/requests/my?status=APPROVED
GET /approval/requests/my?status=REJECTED
GET /approval/requests/my?status=CANCELED
Headers: X-User-UUID
```

**응답 (data):**
```json
[
  {
    "requestId": "UUID",
    "documentId": "UUID",
    "documentName": "연차신청서",
    "memberId": "UUID",
    "requestType": "VACATION",
    "contentJson": "{...}",
    "requestStatus": "WAIT",
    "cancelReason": null,
    "requesterName": "김사원",
    "requesterOrganizationId": "UUID",
    "requesterOrganizationName": "개발팀",
    "createdAt": "2026-04-09T10:00:00",
    "updatedAt": "2026-04-09T10:00:00",
    "approvalLines": [
      {
        "approvalId": "UUID",
        "requestId": "UUID",
        "approverMemberId": "UUID",
        "approverMemberPositionId": "UUID",
        "stepOrder": 1,
        "approvalStatus": "PENDING",
        "actedAt": null,
        "comment": null,
        "signatureImageUrl": null,
        "isSignedYn": null,
        "actualApproverMemberId": null,
        "actualApproverMemberPositionId": null,
        "isProxyYn": "N",
        "createdAt": "2026-04-09T10:00:00",
        "updatedAt": "2026-04-09T10:00:00"
      },
      {
        "approvalId": "UUID",
        "requestId": "UUID",
        "approverMemberId": "UUID",
        "approverMemberPositionId": "UUID",
        "stepOrder": 2,
        "approvalStatus": "WAITING",
        "actedAt": null,
        "comment": null,
        "signatureImageUrl": null,
        "isSignedYn": null,
        "actualApproverMemberId": null,
        "actualApproverMemberPositionId": null,
        "isProxyYn": "N",
        "createdAt": "2026-04-09T10:00:00",
        "updatedAt": "2026-04-09T10:00:00"
      }
    ],
    "viewers": [
      {
        "viewerId": "UUID",
        "requestId": "UUID",
        "viewerMemberId": "UUID",
        "viewerMemberPositionId": "UUID",
        "viewerType": "CC",
        "viewerReadStatus": "UNREAD",
        "viewedAt": null,
        "createdAt": "2026-04-09T10:00:00",
        "updatedAt": "2026-04-09T10:00:00"
      }
    ]
  }
]
```

### 2-3. 결재 요청 상세 조회
```
GET /approval/requests/{requestId}
Headers: X-User-CompanyId
```

**응답:** 위와 동일 구조 (단건)

### 2-4. 임시저장함 (DRAFT 전용 목록)
임시저장된 결재 문서만 따로 조회.

```
GET /approval/requests/my?status=DRAFT
Headers: X-User-UUID
```

**응답 (data):** ApprovalRequestResDto 리스트 (2-1과 동일)

**프론트 UI 권장:**
- 사이드바에 "임시저장함(N)" 메뉴로 카운트 배지 표시
- 클릭 시 임시저장 목록 노출 → 각 항목 클릭 → 상세 화면(1-4와 동일한 작성 폼)으로 이동하여 수정/제출

### 2-5. 임시저장 수정/제출
DRAFT 상태인 결재만 수정 가능. 수정 시 결재라인, 참조자, 첨부파일이 전부 삭제 후 새로 저장됨.

```
PATCH /approval/requests/{requestId}
Headers: X-User-CompanyId, X-User-UUID
Content-Type: application/json
```

**요청 Body:** 1-4 생성과 동일한 구조

**수정 시 동작:**
- DRAFT 상태에서만 가능 (WAIT 이후는 수정 불가)
- requestStatus를 DRAFT(재저장) 또는 WAIT(제출)로 변경 가능
- 기존 결재라인, 참조자, 첨부파일 전부 물리 삭제 후 새로 저장
- `requestStatus=WAIT`로 변경 시: 1번 결재자 LineStatus=PENDING으로 세팅, 알림 발송

**프론트 UX 흐름:**
1. 임시저장함에서 문서 클릭 → 상세 진입
2. 작성 폼에 기존 값 채워서 편집
3. `[임시저장]` 클릭 → `PATCH` with `requestStatus=DRAFT` (계속 임시저장 상태 유지)
4. `[결재 올리기]` 클릭 → `PATCH` with `requestStatus=WAIT` (정식 제출)
5. 더 이상 필요 없으면 `[삭제/취소]` → `PATCH /cancel` (CANCELED 처리)

### 2-6. 결재 취소
DRAFT 또는 WAIT 상태인 결재만 취소 가능.

```
PATCH /approval/requests/{requestId}/cancel
Headers: X-User-CompanyId, X-User-UUID
Content-Type: application/json
```

**요청 Body:**
```json
{
  "cancelReason": "일정 변경으로 취소합니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| cancelReason | String | O | 취소 사유 |

**취소 시 동작:**
- 결재요청 상태 → CANCELED
- 모든 결재라인 → CANCELED

---

## 3. 결재 처리 (결재자 입장)

### 3-1. 결재 대기함 (내가 결재할 문서)
현재 내 차례인 결재 목록 (LineStatus = PENDING인 것만).
부재 위임이 설정되어 있으면 부재자의 결재 대기건도 함께 조회됨.

```
GET /approval/approvals/pending
Headers: X-User-CompanyId, X-User-UUID, X-User-MemberPositionId
```

**응답 (data):** ApprovalRequestResDto 리스트 (2-1과 동일 구조)

**동작:**
- 내 직위(memberPositionId)에 할당된 PENDING 건 조회
- 내가 대결자로 지정된 부재자의 PENDING 건도 자동 합산
- 중복 제거 (동일 requestId)

### 3-2. 결재 완료함 (내가 처리한 결재 이력)
내가 승인 또는 반려한 결재 목록. 대결로 처리한 건도 포함.

```
GET /approval/approvals/acted
Headers: X-User-UUID, X-User-MemberPositionId
```

**응답 (data):** ApprovalRequestResDto 리스트

**동작:**
- 내 직위(memberPositionId)로 직접 처리한 건 조회
- 내가 대결자(actualApproverMemberId)로서 처리한 건도 자동 합산
- 위임 기간이 끝나도 대결 처리 이력은 계속 보임

### 3-3. 승인 처리
본인 결재건 또는 대결 권한이 있는 건만 처리 가능.

```
PATCH /approval/approvals/{approvalId}/approve
Headers: X-User-CompanyId, X-User-UUID, X-User-MemberPositionId
Content-Type: application/json
```

**요청 Body:**
```json
{
  "comment": "승인합니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| comment | String | X | 승인 의견 (선택) |

**승인 시 동작:**
- 권한 확인: 본인 결재건(memberPositionId 일치) 또는 활성 부재위임의 대결자인지 검증
- 현재 결재라인: PENDING → APPROVED
- 다음 결재라인이 있으면: 다음 라인 WAITING → PENDING, 요청 상태 WAIT → PENDING
- 마지막 결재자면: 요청 상태 → APPROVED
- `actualApproverMemberId`, `actualApproverMemberPositionId`에 실제 처리자 기록

### 3-4. 반려 처리
본인 결재건 또는 대결 권한이 있는 건만 처리 가능.

```
PATCH /approval/approvals/{approvalId}/reject
Headers: X-User-CompanyId, X-User-UUID, X-User-MemberPositionId
Content-Type: application/json
```

**요청 Body:**
```json
{
  "comment": "사유가 불충분합니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| comment | String | O | 반려 사유 (필수) |

**반려 시 동작:**
- 권한 확인: 본인 결재건 또는 활성 부재위임의 대결자인지 검증
- 현재 결재라인: PENDING → REJECTED
- 나머지 WAITING 라인: 전부 REJECTED
- 요청 상태: → REJECTED
- `actualApproverMemberId`, `actualApproverMemberPositionId`에 실제 처리자 기록

### 대결(대리결재) 응답 예시
대결자가 처리한 결재라인은 `isProxyYn="Y"`로 표시됨:

```json
{
  "approvalId": "UUID",
  "requestId": "UUID",
  "approverMemberId": "김부장-UUID (원래 결재자)",
  "approverMemberPositionId": "김부장-직위-UUID",
  "stepOrder": 2,
  "approvalStatus": "APPROVED",
  "actedAt": "2026-04-10T14:30:00",
  "comment": "대리 승인합니다.",
  "actualApproverMemberId": "이과장-UUID (대결자)",
  "actualApproverMemberPositionId": "이과장-직위-UUID",
  "isProxyYn": "Y",
  "createdAt": "2026-04-09T10:00:00",
  "updatedAt": "2026-04-10T14:30:00"
}
```

**프론트 UI 권장 — 결재자 이름 표시 가이드**

상단 결재 박스(신청/승인 칸)와 하단 결재라인 테이블 둘 다 아래 규칙으로 렌더링.

#### (1) 표시 규칙
| 조건 | 표시 형식 | 예시 |
|------|-----------|------|
| `isProxyYn == "N"` (또는 null, 미처리) | `{원결재자 이름}` | `황주안2` |
| `isProxyYn == "Y"` (대결 처리됨) | `{원결재자 이름} (대결: {실제결재자 이름})` | `황주안2 (대결: 이다은2)` |

#### (2) 필드 매핑
ApprovalLineResDto 응답에서 다음 필드를 사용:

| 용도 | 필드 | 비고 |
|------|------|------|
| 원결재자 이름 조회용 ID | `approverMemberPositionId` | 항상 존재 (결재라인 생성 시 고정) |
| 실제결재자 이름 조회용 ID | `actualApproverMemberPositionId` | 대결 처리 시에만 값 존재 (아니면 null) |
| 대결 여부 판단 | `isProxyYn` | `"Y"` / `"N"` String |
| 상태 | `approvalStatus` | `WAITING` / `PENDING` / `APPROVED` / `REJECTED` / `CANCELED` |
| 처리 일시 | `actedAt` | 승인/반려 시간 |
| 의견 | `comment` | |

#### (3) 이름 조회 방법
memberPositionId → 이름 매핑은 member-service 내부 API로 조회:

```
GET /member/position/internal/{memberPositionId}
```
**응답 (MemberPositionResDto):** `memberName`, `organizationName`, `jobTitleName` 등 포함

**프론트 구현 팁:**
- 문서 상세 진입 시 `approvalLines[]`를 한 번 순회하며 `approverMemberPositionId` + `actualApproverMemberPositionId` 전체를 모아 한 번에 병렬 조회 → Map<UUID, name> 캐싱
- 같은 직위가 여러 번 등장해도 API 호출은 1회로 제한

#### (4) UI 예시 (연차신청서 상단 승인 박스)
```
┌──────────────┬──────────────┐
│   팀장        │   팀원        │
│ 황주안2       │ 이병찬2       │
│ (대결: 이다은2)│              │  ← isProxyYn="Y"인 라인에만 (대결: ...) 덧붙임
│ 2026/04/13   │ 2026/04/13   │
└──────────────┴──────────────┘
```

#### (5) UI 예시 (하단 결재라인 테이블)
| 순서 | 결재자 | 상태 | 의견 | 처리일 |
|------|--------|------|------|--------|
| 1 | 황주안2 (팀장) *(대결: 이다은2)* | APPROVED | — | 2026-04-13 14:35 |
| 2 | 이병찬2 (팀원) | APPROVED | — | 2026-04-13 14:48 |

- 대결 라인만 "(대결: 실제결재자)" 병기 또는 "대결" 배지 추가
- 직책/부서도 필요하면 MemberPositionResDto의 `jobTitleName` / `organizationName` 활용

---

## 4. 부서 문서함

본인 부서 및 하위 조직의 구성원이 올린 **최종 처리된 결재**(승인 완료 / 반려)를 모아보는 화면.

**주요 규칙:**
- 작성 **시점 부서** 기준 (작성자가 이후 부서이동해도 원래 부서 문서함에 남음)
- 본인 부서 + 하위 조직 전체 (재귀) 포함
- **승인 완료(APPROVED) / 반려(REJECTED)** 상태만 노출
- DRAFT, WAIT, PENDING, CANCELED는 제외
- 민감 양식(`isDeptVisibleYn = "N"`인 양식)은 목록에서 제외
- 본인이 올린 결재도 포함해서 보임
- 권한: 본인 부서 또는 그 하위 조직만 조회 가능 (범위 밖 `organizationId` 요청 시 403)

### 4-1. 부서 문서함 목록 조회
```
GET /approval/requests/department?organizationId={조회대상부서UUID}
Headers: X-User-CompanyId, X-User-MemberPositionId
```

| 쿼리 파라미터 | 타입 | 필수 | 설명 |
|------|------|------|------|
| organizationId | UUID | O | 조회할 부서 ID (본인 부서 또는 그 하위 조직) |

**권한 검증:**
- 요청자의 현재 부서를 기준으로 하위 조직 범위를 계산
- `organizationId`가 이 범위 안에 없으면 `403 FORBIDDEN`

**응답 (data):** ApprovalRequestResDto 리스트 (2-1과 동일 구조)
- `requesterName`, `requesterOrganizationId`, `requesterOrganizationName` 필드로 작성자 정보 확인 가능
- `createdAt` 내림차순 정렬 (최신이 위로)

**프론트 UI 권장:**
- 사이드바/상단 탭에 "부서 문서함" 메뉴 제공
- 상단에 부서 선택 드롭다운 (본인 부서 기본값, 하위 조직으로 전환 가능)
  - 본인 부서의 `GET /organization/list` 결과를 기반으로 드롭다운 구성하거나,
  - 본인 부서만 고정해놓고 보여줘도 됨 (서버에서 하위 조직까지 자동 포함)
- 목록 컬럼 예시: 작성일 / 작성자 / 작성자 소속 / 양식명 / 상태 / 제목
- 행 클릭 → `GET /approval/requests/{requestId}` 호출해 상세 진입

**예시:**
- 사용자 A가 "개발팀" 소속일 때 `organizationId=개발팀UUID` 호출 → 개발팀 + 하위 조직 전원의 승인/반려 결재 노출
- 같은 사용자가 `organizationId=마케팅팀UUID` 호출 → 403 FORBIDDEN

---

## 5. 부재 위임 (대결)

결재자가 휴가, 출장 등으로 부재 시 대리결재자를 지정하는 기능.

**주요 규칙:**
- 위임 기간 중 부재자의 PENDING 결재건이 대결자의 결재 대기함에도 노출
- 대결자는 부재자의 결재건을 본인 결재건처럼 승인/반려 가능
- 대결 처리 시 `isProxyYn="Y"`, `actualApproverMemberId`에 대결자 정보 기록
- 부재자 복귀 후에도 대결자가 처리했던 건은 대결자의 결재 완료함에 유지
- 자기 자신에게 위임 불가, 체인 위임(A→B→C) 불가
- 동일 기간 중복 위임 불가
- 시작일은 당일 이후만 가능

### 5-1. 부재 위임 등록
```
POST /approval/absence-proxy
Headers: X-User-CompanyId, X-User-UUID
Content-Type: application/json
```

**요청 Body:**
```json
{
  "substituteId": "UUID (대결자 memberId)",
  "startDate": "2026-04-15T00:00:00",
  "endDate": "2026-04-20T23:59:59"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| substituteId | UUID | O | 대결자 memberId |
| startDate | LocalDateTime | O | 위임 시작일시 (당일 이후) |
| endDate | LocalDateTime | O | 위임 종료일시 (startDate 이후) |

**검증 규칙:**
- 자기 자신에게 위임 불가 (memberId == substituteId → 400)
- startDate는 당일(오늘 00:00) 이후여야 함 (과거 불가 → 400)
- endDate > startDate여야 함 (→ 400)
- 기존 활성 위임과 기간 겹침 불가 (→ 409)
- 대결자가 같은 기간에 본인도 부재자(위임 등록자)이면 불가 — 체인 위임 방지 (→ 409)

**응답 (data):**
```json
{
  "proxyId": "UUID",
  "companyId": "UUID",
  "memberId": "UUID (부재자)",
  "substituteId": "UUID (대결자)",
  "startDate": "2026-04-15T00:00:00",
  "endDate": "2026-04-20T23:59:59",
  "isActiveYn": "Y",
  "createdAt": "2026-04-13T10:00:00",
  "updatedAt": "2026-04-13T10:00:00"
}
```

### 5-2. 내가 등록한 위임 목록
현재 유효 + 미래 예약된 위임만 조회 (기간 만료 건 제외).

```
GET /approval/absence-proxy/my
Headers: X-User-CompanyId, X-User-UUID
```

**응답 (data):** AbsenceProxyResDto 리스트 (5-1과 동일 구조, startDate ASC 정렬)

### 5-3. 나에게 위임된 목록
다른 사람이 나를 대결자로 지정한 현재 + 미래 위임 목록.

```
GET /approval/absence-proxy/delegated
Headers: X-User-CompanyId, X-User-UUID
```

**응답 (data):** AbsenceProxyResDto 리스트 (5-1과 동일 구조, startDate ASC 정렬)

### 5-4. 위임 비활성화 (취소)
본인이 등록한 위임만 취소 가능.

```
PATCH /approval/absence-proxy/{proxyId}/deactivate
Headers: X-User-CompanyId, X-User-UUID
```

**응답 (data):** AbsenceProxyResDto (isActiveYn="N"으로 변경됨)

**비활성화 후 동작:**
- 즉시 대결 효력 상실
- 대결자의 결재 대기함에서 부재자 건이 더 이상 안 보임
- 이미 대결 처리 완료된 건은 영향 없음 (이력 유지)

**프론트 UI 권장:**
- "부재 위임 관리" 화면 (마이페이지 또는 결재 설정 내)
- 상단: "위임 등록" 버튼 → 대결자 선택 모달 + 기간 입력
- 탭 1: "내가 등록한 위임" (5-2) — 각 항목에 [취소] 버튼
- 탭 2: "나에게 위임된 목록" (5-3) — 조회 전용
- 위임 상태 표시: 현재 진행 중(초록), 미래 예약(파랑), 시작일/종료일 표시
- 결재 대기함에서 대결 건은 "(대결)" 라벨로 구분

---

## enum 정리

### RequestStatus (결재 요청 상태 - 요청자 입장)
| 값 | 설명 |
|---|---|
| DRAFT | 임시저장 |
| WAIT | 제출됨 (첫 번째 결재자 차례) |
| PENDING | 결재 진행 중 (2번째 이후 결재자 차례) |
| APPROVED | 최종 승인 |
| REJECTED | 반려 |
| CANCELED | 취소 |

### LineStatus (결재라인 상태 - 결재자 입장)
| 값 | 설명 |
|---|---|
| WAITING | 아직 내 차례 아님 (이전 결재자가 미처리) |
| PENDING | 내 차례 (결재 대기 중) |
| APPROVED | 승인함 |
| REJECTED | 반려됨 |
| CANCELED | 취소된 문서 |

### ViewerType (참조/공람 구분)
| 값 | 설명 |
|---|---|
| CC | 참조 (결재 진행과 무관하게 열람) |
| CIRCULATION | 공람 (결재 완료 후 열람) |

### ViewerReadStatus
| 값 | 설명 |
|---|---|
| UNREAD | 미확인 |
| READ | 확인함 |

### RequestType (양식 유형)
| 값 | 설명 |
|---|---|
| VACATION | 휴가 |
| ATTENDANCE | 근태 |
| HR_MOVEMENT | 부서이동 |
| SALARY | 급여 |
| GENERAL | 일반기안 |
| CONTRACT | 전자계약 |
| CERTIFICATE | 문서발급 |

---

## 프론트 화면별 API 매핑

### 결재 요청 작성 화면
1. `GET /approval/documents/active` → 양식 선택 드롭다운
2. `GET /approval/policyLines/{documentId}/candidates` → 결재자 후보 목록
3. `GET /member/list` → 참조자/공람자 선택 모달 (직원 검색)
4. `POST /approval/requests` → 임시저장(DRAFT) 또는 제출(WAIT)
   - `viewers` 배열에 `{ memberPositionId, viewerType: "CC" | "CIRCULATION" }` 포함

### 내 결재함 (요청자)
1. `GET /approval/requests/my` → 전체 목록
2. `GET /approval/requests/my?status=DRAFT` → 임시저장함
3. `GET /approval/requests/{requestId}` → 상세 조회 (참조자/공람자 목록 포함)
4. `GET /member/list` → 참조자/공람자 수정 모달 (직원 검색)
5. `PATCH /approval/requests/{requestId}` → 임시저장 수정/제출 (viewers 배열로 전체 교체)
6. `PATCH /approval/requests/{requestId}/cancel` → 취소

### 임시저장 플로우
1. 작성 화면에서 `[임시저장]` → `POST /approval/requests` body `requestStatus=DRAFT`
2. 임시저장함 진입 → `GET /approval/requests/my?status=DRAFT`
3. 항목 클릭 → `GET /approval/requests/{requestId}` (기존 값 로드)
4. 수정 후 재저장 → `PATCH /approval/requests/{requestId}` body `requestStatus=DRAFT`
5. 최종 제출 → `PATCH /approval/requests/{requestId}` body `requestStatus=WAIT`
6. 폐기 → `PATCH /approval/requests/{requestId}/cancel`

### 결재함 (결재자)
1. `GET /approval/approvals/pending` → 결재 대기함 (대결 건 포함)
2. `GET /approval/approvals/acted` → 결재 완료함 (대결 처리 건 포함)
3. `GET /approval/requests/{requestId}` → 상세 조회
4. `PATCH /approval/approvals/{approvalId}/approve` → 승인 (본인 건 + 대결 건)
5. `PATCH /approval/approvals/{approvalId}/reject` → 반려 (본인 건 + 대결 건)

### 부서 문서함
1. `GET /approval/requests/department?organizationId={본인부서UUID}` → 부서 문서함 목록
   - 본인 부서 + 하위 조직의 승인 완료/반려 결재만 반환
   - 민감 양식(`isDeptVisibleYn="N"`)은 자동 제외
2. `GET /approval/requests/{requestId}` → 상세 조회

### 부재 위임 관리
1. `GET /member/list` → 대결자 선택 모달 (직원 검색)
2. `POST /approval/absence-proxy` → 위임 등록
3. `GET /approval/absence-proxy/my` → 내가 등록한 위임 목록
4. `GET /approval/absence-proxy/delegated` → 나에게 위임된 목록
5. `PATCH /approval/absence-proxy/{proxyId}/deactivate` → 위임 취소
