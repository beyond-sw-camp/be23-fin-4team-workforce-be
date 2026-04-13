# 결재 처리 응답 — 대결자 정보 가이드 (프론트용)

승인/반려 처리 후 응답, 결재 대기함/완료함/문서 상세 조회 시 **원래 결재자(부재자)** 와 **실제 결재자(대결자)** 를 구분해서 표시하기 위한 필드 설명서.

---

## 1. 응답이 내려오는 API

아래 API들 모두 동일한 `ApprovalRequestResDto` 구조를 반환하며, 그 안의 `approvalLines[]` 각 항목에 대결 정보가 담겨 있음.

| API | 메서드 | 경로 |
|-----|--------|------|
| 승인 처리 | PATCH | `/approval/approvals/{approvalId}/approve` |
| 반려 처리 | PATCH | `/approval/approvals/{approvalId}/reject` |
| 결재 대기함 | GET | `/approval/approvals/pending` |
| 결재 완료함 | GET | `/approval/approvals/acted` |
| 결재 요청 상세 | GET | `/approval/requests/{requestId}` |
| 내가 올린 결재 | GET | `/approval/requests/my` |

---

## 2. 핵심 필드 (ApprovalLineResDto)

`approvalLines[]` 배열의 각 결재라인에 아래 필드가 내려옴.

| 필드 | 타입 | 값 | 설명 |
|------|------|-----|------|
| `approverMemberId` | UUID | 항상 존재 | **원래 결재자**(부재자 포함)의 memberId. 결재라인 생성 시 고정되며 대결 처리돼도 바뀌지 않음 |
| `approverMemberPositionId` | UUID | 항상 존재 | **원래 결재자**의 직위 ID. 이름 조회용 |
| `actualApproverMemberId` | UUID | 처리 시에만 | **실제로 처리한 사람**의 memberId. 본인이 처리했으면 `approverMemberId`와 같음, 대결자가 처리했으면 다름 |
| `actualApproverMemberPositionId` | UUID | 처리 시에만 | **실제로 처리한 사람**의 직위 ID. 이름 조회용 |
| `isProxyYn` | String | `"Y"` / `"N"` | **대결 여부**. `"Y"` = 대결자가 처리, `"N"` = 본인이 처리 또는 미처리 |
| `approvalStatus` | enum | `WAITING` / `PENDING` / `APPROVED` / `REJECTED` / `CANCELED` | 결재 상태 |
| `actedAt` | datetime | 처리 시에만 | 실제 처리 일시 |
| `comment` | String | 처리 시에만 | 승인/반려 의견 |
| `stepOrder` | Integer | 항상 존재 | 결재 순서 (1부터 연속) |

### 상태별 필드 값 조합

| 상황 | `approvalStatus` | `actualApproverMemberId` | `isProxyYn` |
|------|------------------|--------------------------|-------------|
| 미처리 (대기/진행 중) | `WAITING` / `PENDING` | `null` | `"N"` |
| 본인이 처리 | `APPROVED` / `REJECTED` | = `approverMemberId` | `"N"` |
| 대결자가 처리 | `APPROVED` / `REJECTED` | ≠ `approverMemberId` | `"Y"` |
| 반려 전파로 취소 | `REJECTED` (다음 라인) | `null` | `"N"` |
| 요청 취소 | `CANCELED` | `null` | `"N"` |

---

## 3. 응답 예시

### 3-1. 본인이 직접 처리한 경우 (isProxyYn="N")
```json
{
  "approvalId": "a1...",
  "requestId": "r1...",
  "approverMemberId": "황주안2-UUID",
  "approverMemberPositionId": "황주안2-직위-UUID",
  "stepOrder": 1,
  "approvalStatus": "APPROVED",
  "actedAt": "2026-04-13T14:35:00",
  "comment": "승인합니다.",
  "actualApproverMemberId": "황주안2-UUID",
  "actualApproverMemberPositionId": "황주안2-직위-UUID",
  "isProxyYn": "N",
  "isSignedYn": "Y",
  "createdAt": "2026-04-13T10:00:00",
  "updatedAt": "2026-04-13T14:35:00"
}
```

### 3-2. 대결자가 처리한 경우 (isProxyYn="Y")
황주안2(부재자) 대신 이다은2(대결자)가 승인한 결재라인.

```json
{
  "approvalId": "a1...",
  "requestId": "r1...",
  "approverMemberId": "황주안2-UUID",
  "approverMemberPositionId": "황주안2-직위-UUID",
  "stepOrder": 1,
  "approvalStatus": "APPROVED",
  "actedAt": "2026-04-13T14:35:00",
  "comment": "대리 승인합니다.",
  "actualApproverMemberId": "이다은2-UUID",
  "actualApproverMemberPositionId": "이다은2-직위-UUID",
  "isProxyYn": "Y",
  "isSignedYn": "Y",
  "createdAt": "2026-04-13T10:00:00",
  "updatedAt": "2026-04-13T14:35:00"
}
```

### 3-3. 아직 처리 전 (PENDING/WAITING)
`actualApprover*` 필드는 `null`, `isProxyYn`은 `"N"`.

```json
{
  "approvalId": "a2...",
  "approverMemberId": "이병찬2-UUID",
  "approverMemberPositionId": "이병찬2-직위-UUID",
  "stepOrder": 2,
  "approvalStatus": "WAITING",
  "actedAt": null,
  "comment": null,
  "actualApproverMemberId": null,
  "actualApproverMemberPositionId": null,
  "isProxyYn": "N",
  "isSignedYn": null
}
```

---

## 4. 프론트 렌더링 규칙

### 4-1. 표시 규칙
| 조건 | 표시 형식 | 예시 |
|------|-----------|------|
| `isProxyYn == "N"` | `{원결재자 이름}` | `황주안2` |
| `isProxyYn == "Y"` | `{원결재자 이름} (대결: {실제결재자 이름})` | `황주안2 (대결: 이다은2)` |

### 4-2. 이름 조회
`approverMemberPositionId` / `actualApproverMemberPositionId`로 member-service 호출:

```
GET /member/position/internal/{memberPositionId}
```

**응답 (MemberPositionResDto):**
```json
{
  "memberPositionId": "UUID",
  "memberId": "UUID",
  "memberName": "황주안2",
  "organizationId": "UUID",
  "organizationName": "개발팀",
  "jobTitleName": "팀장",
  "jobGradeName": "부장"
}
```

### 4-3. 성능 팁
문서 상세 진입 시 `approvalLines[]`를 한 번 순회하여 아래 두 필드 전체를 수집 → 중복 제거 → 병렬 호출 → `Map<UUID, MemberPositionResDto>`로 캐싱.

```
수집 대상: approverMemberPositionId, actualApproverMemberPositionId (null 제외)
```

같은 문서 내에서는 한 번만 조회하도록 구현 권장.

---

## 5. UI 예시

### 5-1. 연차신청서 상단 승인 박스
```
┌────────────┬────────────┐
│   팀장       │   팀원       │
│ 황주안2      │ 이병찬2      │
│ (대결: 이다은2)│            │   ← isProxyYn="Y"인 라인에만 병기
│ 2026/04/13  │ 2026/04/13  │
└────────────┴────────────┘
```

### 5-2. 결재라인 테이블
| 순서 | 결재자 | 상태 | 의견 | 처리일 |
|------|--------|------|------|--------|
| 1 | 황주안2 (팀장) *(대결: 이다은2)* | APPROVED | 대리 승인 | 2026-04-13 14:35 |
| 2 | 이병찬2 (팀원) | APPROVED | — | 2026-04-13 14:48 |

- 대결 라인만 `"(대결: 실제결재자)"` 병기 또는 `"대결"` 배지 추가
- 배지 색상: 원결재 처리 = 회색, 대결 처리 = 주황/파랑 등 구분색 권장

---

## 6. 요약 체크리스트 (프론트 구현용)

- [ ] 응답에서 `approvalLines[]` 추출
- [ ] 각 라인마다 `isProxyYn` 확인 (`"Y"` / `"N"`)
- [ ] `approverMemberPositionId` → 원결재자 이름 조회
- [ ] `isProxyYn == "Y"`이면 `actualApproverMemberPositionId` → 실제결재자 이름 조회
- [ ] 상단 승인 박스와 하단 결재라인 테이블 양쪽 동일 규칙 적용
- [ ] 이름 조회는 Map 캐싱으로 API 호출 최소화
