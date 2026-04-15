# 부서 문서함 조회 범위 변경 API 가이드 메모

## 변경 개요

부서 문서함 조회 시 **본인 부서 + 하위 조직 전체** → **본인이 속한 부서 단독**으로 범위를 축소합니다. 하위 조직(팀/셀 등) 문서는 더 이상 상위 부서에서 열람할 수 없습니다.

---

## 엔드포인트 (변경 없음)

```
GET /approval/requests/department?organizationId={uuid}
```

| 항목 | 값 |
|---|---|
| Method | `GET` |
| Path | `/approval/requests/department` |
| Query | `organizationId` (UUID, 필수) |
| Headers | `X-User-CompanyId`, `X-User-MemberPositionId` |

> 엔드포인트·요청 파라미터·응답 포맷은 이전과 동일합니다. 내부 동작(권한 검증 + 조회 범위)만 변경되었습니다.

---

## 동작 변경 상세

### Before
1. 요청자 본인의 소속 부서 + 그 하위 조직 전체 ID 목록 조회
2. 요청한 `organizationId`가 그 범위 안에 포함되는지 검증
3. 대상 부서의 하위 조직 ID 목록을 다시 조회
4. 해당 ID 목록 전체에 걸쳐 문서 조회

### After
1. 요청자 본인의 소속 부서 ID 조회 (`memberServiceClient.getMemberPosition`)
2. 요청한 `organizationId`가 본인 소속 부서 ID와 **정확히 일치**하는지 검증
3. 본인 부서 단일 ID로만 문서 조회

→ `memberServiceClient.getOrganizationDescendantIds(...)` 호출 2회 제거.

---

## 권한 검증 규칙

- 요청한 `organizationId` ≠ 요청자 본인 `organizationId` → `403 FORBIDDEN`
- 메시지: `"본인이 속한 부서의 문서함만 조회할 수 있습니다."`
- 기존의 `"해당 부서의 문서함에 접근할 권한이 없습니다."` 문구는 사용하지 않습니다.

---

## 조회 필터 조건 (변경 없음)

- `companyId` 일치
- `ApprovalDocument.isDeptVisibleYn = 'Y'` (민감 양식 제외)
- `requesterOrganizationId` = 본인 부서 (작성 시점 부서 기준)
- 상태: `APPROVED` 또는 `REJECTED` 만
- `delYn = 'N'`
- 정렬: `createdAt DESC`

---

## 프론트엔드 영향도

| 구분 | 내용 |
|---|---|
| API 호출 방식 | 변경 없음 |
| 응답 스키마 | 변경 없음 |
| UX 변경 | 부서 선택 UI에서 **하위 조직 선택 옵션 제거** 필요 |
| 에러 처리 | 403 응답 메시지 문구 업데이트 필요 (`"본인이 속한 부서의 문서함만 조회할 수 있습니다."`) |
| 노출 데이터 | 하위 조직 문서가 더 이상 포함되지 않음 — 관리자/팀장의 문서 수 감소 예상 |

---

## 요청/응답 예시

### 요청
```http
GET /approval/requests/department?organizationId=7a2f...c3 HTTP/1.1
X-User-CompanyId: 11111111-...-...
X-User-MemberPositionId: 22222222-...-...
```

### 성공 (200)
```json
{
  "success": true,
  "message": "부서 문서함 조회 성공",
  "data": [
    {
      "requestId": "...",
      "requesterOrganizationId": "7a2f...c3",
      "requestStatus": "APPROVED",
      "createdAt": "2026-04-10T09:12:33",
      "...": "..."
    }
  ]
}
```

### 권한 오류 (403)
```json
{
  "success": false,
  "message": "본인이 속한 부서의 문서함만 조회할 수 있습니다."
}
```

---

## 변경 파일 체크리스트

- [x] `approval-service/.../service/ApprovalRequestService.java` — `findDepartmentRequests` 로직 교체
- [ ] `approval-service/.../repository/ApprovalRequestRepository.java` — (선택) `IN` 절을 `=` 로 단순화
- [ ] `docs/approval-notification-frontend-guide.md` — "본인 부서 + 하위 조직" 설명 문구 수정
- [ ] 프론트엔드 — 부서 선택 UI / 403 에러 메시지 반영

---

_작성일: 2026-04-15_
