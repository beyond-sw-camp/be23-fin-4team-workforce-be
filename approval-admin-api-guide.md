# 결재 관리자 화면 - 프론트엔드 API 가이드

## 공통 헤더
모든 요청에 Gateway가 주입하는 헤더:
```
X-User-CompanyId: UUID          # 회사 ID
X-User-MemberPositionId: UUID   # 요청자 직위 ID
X-User-IsSystemAdmin: true/false
```

## 공통 응답 형식 (ApiResponse)
```json
{
  "status": "SUCCESS",
  "message": "메시지",
  "data": { ... }
}
```

---

## 1. 결재 양식 목록 화면

### 1-1. 전체 양식 목록 조회 (관리자용)
```
GET /approval/documents
Headers: X-User-CompanyId
권한: @CheckPermission(APPROVAL_AD, READ)
```

**응답 (data):**
```json
[
  {
    "documentId": "UUID",
    "companyId": "UUID",
    "documentName": "연차신청서",
    "formSchema": "{...JSON...}",
    "isActiveYn": "Y",
    "requestType": "VACATION",
    "autoApproveYn": "N",
    "isDeptVisibleYn": "Y",
    "createdAt": "2026-04-09T10:00:00",
    "updatedAt": "2026-04-09T10:00:00"
  }
]
```

### 1-2. 활성 양식 목록 조회 (일반 사용자용)
```
GET /approval/documents/active
Headers: X-User-CompanyId
권한: 없음 (누구나)
```

**응답:** 위와 동일한 구조, isActiveYn="Y"인 것만

---

## 2. 결재 양식 생성/수정 화면

### 2-1. 양식 생성
```
POST /approval/documents
Headers: X-User-CompanyId
Content-Type: application/json
권한: @CheckPermission(APPROVAL_AD, CREATE)
```

**요청 Body:**
```json
{
  "documentName": "연차신청서",
  "requestType": "VACATION",
  "formSchema": "{\"fields\":[{\"name\":\"vacationType\",\"label\":\"휴가 종류\",\"type\":\"select\",\"options\":[\"연차\",\"반차(오전)\",\"반차(오후)\"]},{\"name\":\"startDate\",\"label\":\"시작일\",\"type\":\"date\"},{\"name\":\"endDate\",\"label\":\"종료일\",\"type\":\"date\"},{\"name\":\"reason\",\"label\":\"사유\",\"type\":\"textarea\"}]}",
  "autoApproveYn": "N",
  "isDeptVisibleYn": "Y"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| documentName | String | O | 양식 이름 |
| requestType | String | O | VACATION, ATTENDANCE, HR_MOVEMENT, SALARY, GENERAL, CONTRACT, CERTIFICATE |
| formSchema | String(JSON) | O | 입력 필드 정의 |
| autoApproveYn | String | X | "Y" 또는 "N" (기본값 "N") |
| isDeptVisibleYn | String | X | "Y" 또는 "N" (기본값 "Y"). 부서 문서함 노출 여부. 인사/급여처럼 민감한 양식은 "N"으로 지정해 부서 문서함에서 제외 |

**응답 (data):** ApprovalDocumentResDto (1-1과 동일 구조, 단건)

### 2-2. 양식 단건 조회
```
GET /approval/documents/{documentId}
Headers: X-User-CompanyId
권한: @CheckPermission(APPROVAL_AD, READ)
```

**응답 (data):** ApprovalDocumentResDto 단건

### 2-3. 양식 활성화
```
PATCH /approval/documents/{documentId}/activate
Headers: X-User-CompanyId
권한: @CheckPermission(APPROVAL_AD, UPDATE)
```

**응답 (data):** ApprovalDocumentResDto (isActiveYn="Y"로 변경됨)

### 2-4. 양식 비활성화
```
PATCH /approval/documents/{documentId}/deactivate
Headers: X-User-CompanyId
권한: @CheckPermission(APPROVAL_AD, UPDATE)
```

**응답 (data):** ApprovalDocumentResDto (isActiveYn="N"로 변경됨)

### 2-5. 자동승인 활성화
```
PATCH /approval/documents/{documentId}/auto-approve/enable
Headers: X-User-CompanyId
권한: @CheckPermission(APPROVAL_AD, UPDATE)
```

**응답 (data):** ApprovalDocumentResDto (autoApproveYn="Y"로 변경됨)

### 2-6. 자동승인 비활성화
```
PATCH /approval/documents/{documentId}/auto-approve/disable
Headers: X-User-CompanyId
권한: @CheckPermission(APPROVAL_AD, UPDATE)
```

**응답 (data):** ApprovalDocumentResDto (autoApproveYn="N"로 변경됨)

### 2-7. 부서 문서함 노출 활성화
해당 양식으로 올린 결재를 부서 문서함에 노출시킴.

```
PATCH /approval/documents/{documentId}/dept-visible/enable
Headers: X-User-CompanyId
권한: @CheckPermission(APPROVAL_AD, UPDATE)
```

**응답 (data):** ApprovalDocumentResDto (isDeptVisibleYn="Y"로 변경됨)

### 2-8. 부서 문서함 노출 비활성화
민감 양식(인사평가, 급여 등)을 부서 문서함에서 숨김. 기존에 제출된 결재도 즉시 부서 문서함 목록에서 빠짐.

```
PATCH /approval/documents/{documentId}/dept-visible/disable
Headers: X-User-CompanyId
권한: @CheckPermission(APPROVAL_AD, UPDATE)
```

**응답 (data):** ApprovalDocumentResDto (isDeptVisibleYn="N"로 변경됨)

**프론트 UI 권장:**
- 양식 관리 화면에 `[부서 문서함 노출]` 토글 스위치 배치 (isActiveYn, autoApproveYn과 나란히)
- 인사/급여 카테고리 양식 생성 시 기본값을 "N"으로 자동 세팅
- 토글 변경 시 확인 다이얼로그: "해당 양식으로 올린 기존 결재도 부서 문서함에서 숨겨집니다. 진행하시겠습니까?"

---

## 3. 결재 정책라인 관리 화면

### 3-1. 정책라인 조회 (양식별)
```
GET /approval/policyLines/{documentId}
Headers: X-User-CompanyId
권한: 없음 (누구나)
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
    "createdAt": "2026-04-09T10:00:00",
    "updatedAt": "2026-04-09T10:00:00"
  },
  {
    "policyLineId": "UUID",
    "documentId": "UUID",
    "jobTitleId": "UUID",
    "stepOrder": 2,
    "organizationId": null,
    "createdAt": "2026-04-09T10:00:00",
    "updatedAt": "2026-04-09T10:00:00"
  }
]
```

### 3-2. 정책라인 일괄 저장 (전체 교체)
```
POST /approval/policyLines
Headers: X-User-CompanyId
Content-Type: application/json
권한: @CheckPermission(APPROVAL_AD, CREATE)
```

**요청 Body:**
```json
{
  "documentId": "UUID",
  "policyLines": [
    {
      "jobTitleId": "UUID (직책 ID - 예: 과장)",
      "stepOrder": 1,
      "organizationId": null
    },
    {
      "jobTitleId": "UUID (직책 ID - 예: 부장)",
      "stepOrder": 2,
      "organizationId": null
    },
    {
      "jobTitleId": "UUID (직책 ID - 예: 대표)",
      "stepOrder": 3,
      "organizationId": "UUID (특정 조직 지정시)"
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| documentId | UUID | O | 양식 ID |
| policyLines[].jobTitleId | UUID | O | 직책 ID |
| policyLines[].stepOrder | Integer | O | 결재 순서 (1부터 연속) |
| policyLines[].organizationId | UUID | X | 특정 조직 지정 (null이면 요청자 조직 계열에서 검색) |

**검증 규칙:**
- stepOrder는 1부터 시작, 연속이어야 함 (1, 2, 3...)
- stepOrder 중복 불가
- 기존 정책라인은 전체 삭제 후 새로 저장됨

**응답 (data):** ApprovalPolicyLineResDto 리스트 (3-1과 동일 구조)

### 3-3. 정책라인 전체 삭제
```
DELETE /approval/policyLines/{documentId}
Headers: X-User-CompanyId
권한: @CheckPermission(APPROVAL_AD, DELETE)
```

**응답 (data):** null

### 3-4. 후보 결재자 조회 (사용자가 결재 올릴 때 사용)
```
GET /approval/policyLines/{documentId}/candidates
Headers: X-User-CompanyId, X-User-MemberPositionId
권한: 없음 (누구나)
```

**응답 (data):**
```json
[
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
        "memberName": "김부장",
        "organizationId": "UUID",
        "organizationName": "개발팀",
        "jobTitleId": "UUID",
        "jobTitleName": "부장",
        "jobGradeId": "UUID",
        "jobGradeName": "3급"
      }
    ]
  }
]
```

---

## 프론트에서 필요한 member-service API (참고)

정책라인 관리 화면에서 직책 목록, 조직 목록이 필요:

### 직책 목록 조회
```
GET /organization/job-title/list
Headers: X-User-UUID
```

### 조직 목록 조회
```
GET /organization/list
Headers: X-User-UUID
```

---

## RequestType enum 값
| 값 | 설명 |
|---|---|
| VACATION | 휴가 |
| ATTENDANCE | 근태 (출퇴근/시간 관리) |
| HR_MOVEMENT | 부서이동 |
| SALARY | 급여 |
| GENERAL | 일반기안 |
| CONTRACT | 전자계약 |
| CERTIFICATE | 문서발급 |

---

## formSchema 예시
```json
{
  "fields": [
    {
      "name": "vacationType",
      "label": "휴가 종류",
      "type": "select",
      "options": ["연차", "반차(오전)", "반차(오후)"]
    },
    {
      "name": "startDate",
      "label": "시작일",
      "type": "date"
    },
    {
      "name": "endDate",
      "label": "종료일",
      "type": "date"
    },
    {
      "name": "reason",
      "label": "사유",
      "type": "textarea"
    }
  ]
}
```

지원 가능한 field type: text, textarea, number, date, select
