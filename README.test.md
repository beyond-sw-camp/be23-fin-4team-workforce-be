<div align="center">
<img width="1920" height="800" alt="workforce 쇼릴" src="https://github.com/user-attachments/assets/5027fc7c-45b0-427e-bca8-fb1eac7bc5eb" />
</div>

> **WORKFORCE**는 회사마다 다른 인사 정책과 낮은 디지털 성숙도로 인해 발생하는 업무 공백을 줄이기 위한 AI 기반 HRMS입니다.  
> 결재, 근태, 급여, 목표/평가, ESG, 전자계약, 채팅을 하나의 흐름으로 연결하고, 회사별 정책을 AI 챗봇과 자동화 모듈에 동기화하여 누구나 쉽게 사용할 수 있는 인사 운영 환경을 제공합니다.

---

## 팀원 소개

<div align="center">

| ![김정훈](url1) | ![박세민](url2) | ![이다은](url3) | ![이지연](url4) |
|----------------|-----------------|-----------------|-----------------|
| [김정훈](https://github.com/jeonghuny) | [박세민](https://github.com/semin980520) | [이다은](https://github.com/leeda973) | [이지연](https://github.com/jiyean99) |

</div>

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 목차

1. [서비스 개요](#서비스-개요)
2. [기획 배경](#기획-배경)
3. [주요 기능](#주요-기능)
4. [기술 스택](#기술-스택)
5. [기술 문서](#기술-문서)
6. [시연 영상](#시연-영상)
7. [부록](#부록)

---

## 서비스 개요

WORKFORCE는 **업무 인수인계가 부족해도 누구나 바로 사용할 수 있는 HRMS**를 지향합니다.  
단순히 인사 기능을 모아둔 시스템이 아니라, 회사 정책 변경이 결재 양식, 급여/근태 자동화, AI 챗봇 답변까지 이어지도록 설계했습니다.

### 우리가 해결하려는 문제

| 문제 | 현장에서 발생하는 영향 | WORKFORCE의 접근 |
|------|----------------------|------------------|
| 낮은 디지털 성숙도 | 시스템을 도입해도 사용법 학습과 운영 인수인계에 시간이 소요됨 | AI 챗봇이 시스템 사용법과 회사 정책을 자연어로 안내 |
| 회사별 정책 다양성 | 휴가, 급여, 결재, 조직 구조가 회사마다 달라 표준 솔루션이 맞지 않음 | 회사별 정책, 양식, 조직, 권한을 독립적으로 커스텀 |
| 법령 준수 부담 | 연차 촉진, 주 52시간, 급여/퇴직금 산정 등을 담당자가 수동 검증 | 배치/이벤트 기반 자동 산정과 알림으로 누락 방지 |
| 업무 공백 | 인사 담당자 부재 또는 겸직 상황에서 신규 입사자·직원의 문의가 집중됨 | 온보딩, 조회, 결재 초안, 반복 업무를 AI와 자동화로 보조 |
| 시스템 단절 | 결재 승인 후 근태, 급여, 캘린더 반영을 사람이 다시 처리 | Kafka 이벤트 기반 후속 처리로 업무 흐름 연결 |

### 서비스 한 줄 정의

> **회사별 HR 정책을 AI와 자동화 흐름에 동기화하여, 인사 담당자의 반복 업무와 직원의 시스템 적응 시간을 줄이는 맞춤형 HR 관리 서비스**

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 기획 배경

국내 HR SaaS 시장은 성장하고 있지만, 실제 HR 운영 현장에서는 여전히 인사 담당자가 여러 업무를 겸직하고, 회사마다 다른 정책을 시스템에 맞추기 어려운 문제가 남아 있습니다.  
WORKFORCE는 이 한계를 **디지털 성숙도 격차**와 **정책 커스터마이징의 어려움**으로 정의했습니다.

### 시장 분석에서 도출한 방향

- HR SaaS와 AI 기반 업무 자동화 수요는 증가하고 있습니다.
- 기존 시장 분석에서는 국내 HR 시장 성장, 조직별 디지털 성숙도 격차, 인사 담당자의 겸직 부담이 함께 확인되었습니다.
- 그러나 표준화된 HR 솔루션은 회사별 휴가, 급여, 결재, 조직 정책을 충분히 반영하기 어렵습니다.
- 인사 담당자 부재, 겸직, 인수인계 공백은 시스템 도입 이후에도 업무 병목으로 남습니다.
- 따라서 핵심은 “기능이 많은 HRMS”가 아니라 **가이드 없이도 쉽게 접근하고, 회사 정책에 맞게 자동으로 움직이는 HRMS**입니다.

관련 근거는 기존 README의 시장 분석 출처를 기준으로 정리했습니다.  
[MTN HR 시장 기사](https://news.mtn.co.kr/news-detail/2023022310563781713) · [KDI 디지털 성숙도 조사](https://eiec.kdi.re.kr/policy/domesticView.do?ac=0000156198) · [다우오피스HR 인사담당자 현황](https://hr.daouoffice.com/blog/sme-hr-manager-recruitment-burnout) · [디지털데일리 HR테크 기사](https://www.ddaily.co.kr/page/view/2024050511345817576)

### 핵심 차별점

| 구분 | 기존 HRMS | WORKFORCE |
|------|-----------|-----------|
| 온보딩 | 메뉴얼과 담당자 설명에 의존 | AI 챗봇이 시스템 사용법과 회사 정책을 즉시 안내 |
| 정책 적용 | 고정된 기능에 회사가 맞춤 | 회사별 정책을 설정하고 자동화/챗봇에 동기화 |
| 반복 업무 | 담당자가 조건을 확인하고 수동 처리 | 배치, 이벤트, 알림으로 정기 업무 자동 처리 |
| 결재 이후 처리 | 승인 후 별도 시스템에 재입력 | 승인 이벤트로 근태, 급여, 캘린더 자동 반영 |
| ESG | 별도 캠페인 또는 수기 관리 | HR 데이터와 연결된 ESG 활동, 포인트, 캠페인 관리 |

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 주요 기능

### 1. AI 챗봇 기반 온보딩

- 신입사원과 일반 사용자가 인사 시스템 사용법을 자연어로 질문할 수 있습니다.
- 회사 정책, 결재 양식, 휴가/급여/근태 규정을 RAG 기반으로 조회합니다.
- “내 잔여 연차 얼마야?”처럼 실시간 데이터가 필요한 질문은 백엔드 API를 호출해 답변합니다.
- 전자결재 양식 등록/수정 시 벡터 DB가 갱신되어 챗봇이 최신 양식을 안내할 수 있습니다.

### 2. 회사별 정책 커스터마이징

- 조직, 직급, 직책, 역할/권한, 휴가 종류, 공휴일, 근무 정책, 급여 정책을 회사 단위로 관리합니다.
- JSON 기반 동적 결재 양식으로 회사가 직접 양식을 구성할 수 있습니다.
- 정책 변경 이벤트를 Kafka로 발행해 AI 검색 인덱스와 후속 업무에 반영합니다.

### 3. 반복 인사 업무 자동화

- 연차 부여, 연차 사용 촉진 1차/2차 알림, 월 근태 마감, 급여 명세서 발급, 상여 발행, 퇴직금 정산을 자동화합니다.
- Quartz와 Spring Batch 기반으로 회사별 스케줄을 관리합니다.
- 주 52시간, 일일/월간 근무 한도, 잔여 연차 등 법령·정책 기준을 자동 검증합니다.

### 4. 전자결재와 후속 처리 자동화

- 휴가, 외근, 출장, 출퇴근 정정, 인사 발령, 계약 등 업무를 결재 흐름으로 처리합니다.
- 승인 완료 시 Kafka 이벤트를 통해 근태 차감, 캘린더 등록, 급여 반영, 인사 이력 생성이 자동 수행됩니다.
- 대리결재, 참조, 공람, 임시저장, 회수 등 실무 결재 흐름을 지원합니다.

### 5. 목표·평가 파이프라인

- 목표 설정, 활동 기록, 평가 설계, 다면 평가, 부서 간 보정, 결과 발행을 하나의 흐름으로 관리합니다.
- 평가 결과는 성과급 발행의 기준 데이터로 연결될 수 있습니다.
- AI 회의록에서 도출된 액션 아이템을 목표 시스템과 연결하는 확장 흐름을 고려했습니다.

### 6. ESG 활동 관리

- ESG 활동 신청, 승인, 포인트 적립, 캠페인 운영, 포인트 교환을 HRMS 안에서 처리합니다.
- 인사 데이터와 연결해 부서별/직급별 참여 현황을 분석할 수 있습니다.
- ESG 경영을 별도 부가 기능이 아니라 조직 참여 데이터로 관리할 수 있도록 설계했습니다.

### 7. 협업과 운영 편의 기능

- 실시간 채팅, 알림, 캘린더, 통합 검색을 제공합니다.
- Kafka, Redis Pub/Sub, SSE 기반 알림으로 결재·급여·근태 이벤트를 즉시 전달합니다.
- Elasticsearch 기반 통합 검색으로 흩어진 인사 데이터를 빠르게 조회합니다.

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 기술 스택

<table>
  <tr>
    <td width="160"><b>Backend · Spring</b></td>
    <td>
      <img src="https://img.shields.io/badge/Java-007396?style=flat-square&logo=openjdk&logoColor=white"/>
      <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white"/>
      <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white"/>
      <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white"/>
      <img src="https://img.shields.io/badge/Spring_Batch-6DB33F?style=flat-square&logo=spring&logoColor=white"/>
      <img src="https://img.shields.io/badge/Apache_Kafka-231F20?style=flat-square&logo=apachekafka&logoColor=white"/>
      <img src="https://img.shields.io/badge/Quartz-CC0000?style=flat-square"/>
      <img src="https://img.shields.io/badge/QueryDSL-4479A1?style=flat-square"/>
    </td>
  </tr>
  <tr>
    <td><b>Backend · AI</b></td>
    <td>
      <img src="https://img.shields.io/badge/Python-3776AB?style=flat-square&logo=python&logoColor=white"/>
      <img src="https://img.shields.io/badge/FastAPI-009688?style=flat-square&logo=fastapi&logoColor=white"/>
      <img src="https://img.shields.io/badge/OpenAI-412991?style=flat-square&logo=openai&logoColor=white"/>
      <img src="https://img.shields.io/badge/Pinecone-000000?style=flat-square"/>
      <img src="https://img.shields.io/badge/n8n-EA4B71?style=flat-square&logo=n8n&logoColor=white"/>
    </td>
  </tr>
  <tr>
    <td><b>Frontend</b></td>
    <td>
      <img src="https://img.shields.io/badge/React-20232A?style=flat-square&logo=react&logoColor=61DAFB"/>
      <img src="https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white"/>
      <img src="https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=vite&logoColor=white"/>
      <img src="https://img.shields.io/badge/Ant_Design-0170FE?style=flat-square&logo=antdesign&logoColor=white"/>
      <img src="https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white"/>
      <img src="https://img.shields.io/badge/TanStack_Query-FF4154?style=flat-square&logo=reactquery&logoColor=white"/>
      <img src="https://img.shields.io/badge/Zustand-443E38?style=flat-square"/>
    </td>
  </tr>
  <tr>
    <td><b>Database</b></td>
    <td>
      <img src="https://img.shields.io/badge/MariaDB-003545?style=flat-square&logo=mariadb&logoColor=white"/>
      <img src="https://img.shields.io/badge/Redis-FF4438?style=flat-square&logo=redis&logoColor=white"/>
      <img src="https://img.shields.io/badge/Elasticsearch-005571?style=flat-square&logo=elasticsearch&logoColor=white"/>
    </td>
  </tr>
  <tr>
    <td><b>Cloud / Infra</b></td>
    <td>
      <img src="https://img.shields.io/badge/AWS_EKS-FF9900?style=flat-square&logo=amazoneks&logoColor=white"/>
      <img src="https://img.shields.io/badge/AWS_ECR-FF9900?style=flat-square&logo=amazonaws&logoColor=white"/>
      <img src="https://img.shields.io/badge/Amazon_RDS-527FFF?style=flat-square&logo=amazonrds&logoColor=white"/>
      <img src="https://img.shields.io/badge/Amazon_S3-569A31?style=flat-square&logo=amazons3&logoColor=white"/>
      <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white"/>
    </td>
  </tr>
</table>

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 기술 문서

상세 기술은 아래 문서로 분리했습니다.

| 문서 | 내용 |
|------|------|
| [AI 챗봇 및 RAG 아키텍처](docs/technical/ai-rag-chatbot.md) | 3-Layer RAG, 회사별 벡터 격리, 의도 분류, n8n 오케스트레이션, 정책 자동 동기화 |
| [정책 커스터마이징과 동적 결재 양식](docs/technical/policy-customization.md) | 회사별 정책 관리, JSON 동적 폼, 결재 후속 처리, 역할/권한 설계 |
| [업무 자동화와 배치 처리](docs/technical/hr-automation-batch.md) | Quartz, Spring Batch, 연차 부여, 연차 촉진, 근태 마감, 급여/상여/퇴직금 자동화 |
| [이벤트 기반 시스템 연동](docs/technical/event-driven-flow.md) | Kafka, Outbox, Redis Pub/Sub, SSE, 서비스 간 후속 처리 |
| [ESG 활동 관리](docs/technical/esg-management.md) | ESG 활동, 포인트, 캠페인, 인사 데이터 기반 참여 현황 |
| [시스템 아키텍처](docs/technical/system-architecture.md) | MSA 구성, 서비스 역할, 데이터 흐름, 운영 인프라 |
| [트러블슈팅](docs/technical/troubleshooting.md) | 핵심 기능 구현 중 발생한 문제와 해결 과정 정리 |

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 시연 영상

프론트엔드 화면 기준으로 가능한 모든 시나리오의 증적물을 아래 구조에 맞춰 정리합니다.  
각 항목의 `증적` 칸에는 최종 GIF, MP4, 스크린샷, Notion/GitHub 첨부 링크를 연결합니다.

<details>
<summary><strong>Public · 인증/회사 가입</strong></summary>

| 화면 | 경로 | 증적 시나리오 | 증적 |
|------|------|---------------|------|
| 랜딩 | `/` | 서비스 진입, 로그인/회사 온보딩 이동 | <img width="845" alt="WORKFORCE" src="https://github.com/user-attachments/assets/a8d3b7b8-4880-491f-9073-0dad92340ea5" /> |
| 로그인 | `/login` | 일반 사용자 로그인, SaaS 운영자 로그인 분기 | 준비 중 |
| 비밀번호 찾기 | `/find-password` | 이메일 입력 후 재설정 요청 | 준비 중 |
| 비밀번호 재설정 | `/reset-password` | 재설정 토큰 기반 새 비밀번호 등록 | 준비 중 |
| 비밀번호 변경 | `/change-password` | 최초 로그인/강제 변경 플로우 | 준비 중 |
| 이메일 인증 | `/verify-email` | 이메일 인증 완료/실패 화면 | 준비 중 |
| 회사 온보딩 | `/company/onboarding` | 회사 생성, 기본 정보 입력, 초기 가입 완료 | 준비 중 |
| 접근 금지 | `/403` | 권한 없는 사용자 접근 차단 | 준비 중 |
| Not Found | `/404` | 존재하지 않는 URL 진입 | 준비 중 |

</details>

<details>
<summary><strong>공통 앱 셸 · 대시보드/검색/알림/AI</strong></summary>

| 화면/기능 | 경로 | 증적 시나리오 | 증적 |
|-----------|------|---------------|------|
| 대시보드 | `/app/dashboard` | 로그인 후 메인 위젯, 요약 지표 확인 | 준비 중 |
| HR 인사이트 | `/app/insights` | 인사 지표/분석 화면 조회 | 준비 중 |
| 캘린더 | `/app/calendar` | 회사 일정, 개인 일정, 결재 승인 일정 확인 | 준비 중 |
| 알림 목록 | `/app/notifications` | 알림 목록 조회, 읽음 처리, 관련 화면 이동 | 준비 중 |
| 헤더 알림 팝오버 | 공통 헤더 | 실시간 알림 수신, 알림 클릭 라우팅 | 준비 중 |
| 통합 검색 | 공통 헤더 | 메뉴/구성원/문서 검색 후 상세 이동 | 준비 중 |
| 조직도 모달 | 공통 앱 셸 | 조직 트리 조회, 구성원 상세 패널 확인 | 준비 중 |
| 멤버 채팅 | 공통 헤더/모달 | 1:1/그룹 채팅, 파일/이미지 메시지, 읽음 처리 | 준비 중 |
| AI 챗봇 | 우측 하단 FAB | 정책 질문, 실시간 조회 질문, 답변 출처 확인 | 준비 중 |
| AI 회의록 | 공통 헤더 모달 | 녹음 파일 업로드, 회의록 생성, 결과 확인 | 준비 중 |
| 마이페이지 | `/app/me` | 내 정보 조회 | 준비 중 |

</details>

<details>
<summary><strong>SaaS 운영자 콘솔</strong></summary>

| 화면 | 경로 | 증적 시나리오 | 증적 |
|------|------|---------------|------|
| 운영자 대시보드 | `/saas/dashboard` | SaaS 운영자 로그인 후 콘솔 진입 | 준비 중 |
| 자동 작업 스케줄 | `/saas/schedules` | 테넌트/작업별 스케줄 조회 및 관리 | 준비 중 |
| 세율표 관리 | `/saas/tax-table` | 기준연도별 세율표 조회/관리 | 준비 중 |
| 4대보험 요율 관리 | `/saas/tax-rate` | 보험 요율 조회/관리 | 준비 중 |

</details>

<details>
<summary><strong>초기 설정 · 조직/구성원/권한</strong></summary>

| 화면 | 경로 | 증적 시나리오 | 증적 |
|------|------|---------------|------|
| 관리자 온보딩 | `/app/onboarding` | 회사 초기 조직/정책/결재 기본 설정 | 준비 중 |
| 구성원 목록 | `/app/members` | 구성원 검색, 필터, 직원 등록 | 준비 중 |
| 구성원 상세 | `/app/members/$memberId` | 인사 정보, 소속, 이력 상세 확인 | 준비 중 |
| 조직 구조 | `/app/organization?tab=structure` | 조직 트리 생성/수정/이동 | 준비 중 |
| 직급 관리 | `/app/organization?tab=grades` | 직급 등록/수정/삭제 | 준비 중 |
| 직책 관리 | `/app/organization?tab=titles` | 직책 등록/수정/삭제 | 준비 중 |
| 역할·권한 | `/app/organization?tab=roles` | 기본 역할, 커스텀 권한 설정 | 준비 중 |
| 조직 개편 | `/app/organization?tab=restructure` | 조직 개편 시뮬레이션/적용 | 준비 중 |
| HR 정책 문서 | `/app/ai-documents` | 정책 문서 업로드, 목록 조회, 삭제, 챗봇 연동 증적 | 준비 중 |
| 자동 작업 관리 | `/app/admin/batch-schedule` | 배치 스케줄 조회/수정 | 준비 중 |

</details>

<details>
<summary><strong>전자결재 · 문서함/양식/계약</strong></summary>

| 화면 | 경로 | 증적 시나리오 | 증적 |
|------|------|---------------|------|
| 결재 작성 허브 | `/app/approvals?tab=compose` | 결재 양식 선택, 문서 작성, 임시저장, 상신 | 준비 중 |
| 결재 양식 설정 | `/app/approvals?tab=admin` | JSON 동적 양식 생성/수정, 필드 구성 | 준비 중 |
| 결재 대기함 | `/app/approvals?box=do-pending` | 결재 대기 문서 승인/반려 | 준비 중 |
| 결재 처리함 | `/app/approvals?box=do-acted` | 내가 처리한 문서 조회 | 준비 중 |
| 결재 예정함 | `/app/approvals?box=do-upcoming` | 다음 결재 예정 문서 확인 | 준비 중 |
| 내 기안 전체 | `/app/approvals?box=per-all` | 내가 올린 문서 상태별 조회 | 준비 중 |
| 임시저장함 | `/app/approvals?box=per-draft` | 임시저장 문서 이어쓰기/삭제 | 준비 중 |
| 참조/공람함 | `/app/approvals?box=per-viewers` | 참조/공람 문서 확인 | 준비 중 |
| 부재 위임 | `/app/approvals/absence-proxy` | 결재 위임 등록/해제 | 준비 중 |
| 공문 문서함 | `/app/approvals?box=per-official` | 공문 작성/발송/조회 | 준비 중 |
| 부서 문서함 | `/app/approvals/department` | 부서 기안/발송/수신 문서 조회 | 준비 중 |
| 부서 문서 고급검색 | `/app/approvals/department-search` | 조직/상태/유형별 검색 | 준비 중 |
| 내 기안 검색 | `/app/approvals/my-requests` | 내 기안 문서 목록, 상세 검색 | 준비 중 |
| 출퇴근 정정 신청 | `/app/approvals/correction-request` | 근태 화면에서 정정 결재 prefill 이동 | 준비 중 |
| 전자계약 발송 | `/app/contracts/send` | 계약 템플릿 선택, 대상자 선택, 일괄 발송 | 준비 중 |
| 계약 상세 딥링크 | `/app/approvals?tab=compose` | 계약 알림 클릭 후 상세/서명 흐름 확인 | 준비 중 |

</details>

<details>
<summary><strong>근태 · 휴가 · 출장</strong></summary>

| 화면 | 경로 | 증적 시나리오 | 증적 |
|------|------|---------------|------|
| 내 근태 | `/app/attendance` | 출근/퇴근, 일간/주간 근태 현황, 정정 신청 진입 | 준비 중 |
| 내 월근태 | `/app/attendance/monthly` | 월별 근태 내역 조회 | 준비 중 |
| 개인 근무 스케줄 | `/app/attendance/schedules/my` | 시차출퇴근 스케줄 선택/변경 신청 | 준비 중 |
| 초과근무 신청/조회 | `/app/attendance/overtime` | 연장근무 신청, 신청 이력 조회 | 준비 중 |
| 내 주간 근무시간 | `/app/attendance/work-time` | 주간 누적 근무시간, 52시간 관련 상태 확인 | 준비 중 |
| 휴가 계획 관리 | `/app/leave` | 잔여 연차 조회, 휴가 신청, 연차 촉진 회신 | 준비 중 |
| 출장 신청/이력 | `/app/work-trips` | 출장/외근 신청, 이력 조회 | 준비 중 |
| 전사 근태 현황 | `/app/attendance/company` | 일근태/월근태 현황, 이상 근태 확인 | 준비 중 |
| 근무스케줄 관리 | `/app/attendance/schedules` | 회사 근무 스케줄 정책 관리 | 준비 중 |
| 연장근로 정책 | `/app/attendance/overtime-policies` | 초과근무 정책 등록/수정 | 준비 중 |
| 시차 출퇴근 시간대 | `/app/attendance/flexible-slots` | 출근 슬롯 등록/수정 | 준비 중 |
| 연장근로 현황 | `/app/attendance/overtime-status` | 연장근로 사용 현황 집계 | 준비 중 |
| 휴무일/공휴일 관리 | `/app/attendance/holidays` | 공휴일 등록, 일괄 반영 | 준비 중 |
| 휴가 유형 관리 | `/app/leave/types` | 회사별 휴가 유형 등록/수정 | 준비 중 |
| 연차 정책 관리 | `/app/leave/policies` | 연차 부여/이월/촉진 정책 설정 | 준비 중 |
| 휴직 관리 | `/app/leave/absence` | 휴직 등록/승인 반영 내역 확인 | 준비 중 |
| 연차 통보 미응답자 관리 | `/app/leave/promotion-no-response` | 1차/2차 미응답 대상자 필터링 및 조치 | 준비 중 |

</details>

<details>
<summary><strong>급여 · 상여 · 퇴직금</strong></summary>

| 화면 | 경로 | 증적 시나리오 | 증적 |
|------|------|---------------|------|
| 급여 조회 | `/app/payroll` | 내 급여 명세 목록 조회 | 준비 중 |
| 급여 명세 상세 | `/app/payroll/$payrollId` | 지급/공제/세금 상세 확인 | 준비 중 |
| 수당 변경 신청 | `/app/payroll/allowances` | 개인 수당 변경 신청/이력 조회 | 준비 중 |
| 연봉 조회 | `/app/payroll/annual` | 내 연봉 정보 조회 | 준비 중 |
| 퇴직금 조회 | `/app/payroll/retirement` | 예상 퇴직금 시뮬레이션 | 준비 중 |
| 연봉 협상 이력 | `/app/payroll/negotiations` | 개인 연봉 협상 이력 조회 | 준비 중 |
| 급여 정산 관리 | `/app/payroll/admin` | 월별 정산, 직원별 명세 생성/확정/지급 | 준비 중 |
| 급여대장 상세 편집 | `/app/payroll/admin/$payrollId` | 명세 라인 수정, 확정/지급 처리 | 준비 중 |
| 세금·4대보험 집계 | `/app/payroll/tax-summary` | 월별 보험/원천세 집계 조회 | 준비 중 |
| 급여 정책 | `/app/salary/settings` | 지급/공제 항목, 급여 정책 설정 | 준비 중 |
| 호봉표 관리 | `/app/salary/pay-grade-table` | 호봉 테이블 등록/수정 | 준비 중 |
| 연차수당 정산 | `/app/salary/unused-leave` | 미사용 연차수당 대상 조회/정산 | 준비 중 |
| 퇴직급여 정책 | `/app/salary/retirement-policy` | 법정/DB/DC 퇴직급여 정책 설정 | 준비 중 |
| 연봉 협상 관리 | `/app/salary/negotiations` | 협상 대상자, 요청/승인 흐름 관리 | 준비 중 |
| 상여 정책 | `/app/salary/bonus-policy` | 정기상여/성과급/명절상여 정책 설정 | 준비 중 |

</details>

<details>
<summary><strong>목표 · 평가 · 면담</strong></summary>

| 화면 | 경로 | 증적 시나리오 | 증적 |
|------|------|---------------|------|
| 내 목표 | `/app/performance?view=my` | 개인 목표 생성, 활동 기록, 진척률 확인 | 준비 중 |
| 조직 목표 관리 | `/app/performance?view=org` | 팀/조직 목표 관리, 구성원 목표 확인 | 준비 중 |
| 전사 목표 현황 | `/app/performance?view=company` | 전사 목표 현황 조회 | 준비 중 |
| 평가 허브 | `/app/evaluations` | 내 평가, 평가 작성 진입, 결과 확인 | 준비 중 |
| 평가 운영 관리 | `/app/evaluations?view=overview` | 시즌/설계/운영 탭 확인 | 준비 중 |
| 평가 시즌 상세 | `/app/evaluations/seasons/$seasonId` | 그룹 편성, 평가자 배정, 보정, 결과 발행 | 준비 중 |
| 내 평가 결과 | `/app/evaluations` | 평가 결과 카드/목록 확인 | 준비 중 |
| 면담 목록 | `/app/meetings` | 면담 일정/기록 목록 조회, 생성 | 준비 중 |
| 면담 상세 | `/app/meetings/$meetingId` | 면담 내용 작성/수정, 액션 아이템 확인 | 준비 중 |

</details>

<details>
<summary><strong>ESG</strong></summary>

| 화면 | 경로 | 증적 시나리오 | 증적 |
|------|------|---------------|------|
| My ESG | `/app/esg` | ESG 활동 신청, 포인트 현황, 참여 현황 확인 | 준비 중 |
| ESG 샵 | `/app/esg/shop` | 포인트 상품 조회, 교환 신청 | 준비 중 |
| ESG 설정 | `/app/esg/admin` | ESG 활동 승인/반려, 캠페인/상품 관리 | 준비 중 |

</details>

<details>
<summary><strong>준비 중/레거시/리다이렉트 확인</strong></summary>

| 화면 | 경로 | 증적 시나리오 | 증적 |
|------|------|---------------|------|
| 메일 | `/app/mail` | 준비 중 화면 확인 | 준비 중 |
| AI 비서 페이지 | `/app/ai-assistant` | 준비 중 화면 확인, FAB 챗봇과 구분 | 준비 중 |
| 역할·권한 레거시 | `/app/roles` | `/app/organization?tab=roles` 리다이렉트 확인 | 준비 중 |
| 월근태 레거시 | `/app/attendance/company/monthly` | `/app/attendance/company` 리다이렉트 확인 | 준비 중 |
| 포괄임금 OT 레거시 | `/app/attendance/comprehensive-ot` | `/app/attendance/overtime-status` 리다이렉트 확인 | 준비 중 |

</details>

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 부록

- [ERD 전체 보기](https://www.erdcloud.com/d/Er4amBY2KpweBcKTk)
- [프로그램 사양서 및 단위테스트결과서](https://documenter.getpostman.com/view/51059789/2sBXqDrNZT)
- [요구사항 명세서](https://docs.google.com/spreadsheets/d/107CuIWefCSjBbwO0mnKdQJzK5WCFDKYtw5Rs18liJw8/edit?gid=63147339#gid=63147339)
- [WBS](https://docs.google.com/spreadsheets/d/107CuIWefCSjBbwO0mnKdQJzK5WCFDKYtw5Rs18liJw8/edit?gid=395838842#gid=395838842)
- [Figma 기획서](https://www.figma.com/proto/6suak3PIQpHGzS3UBatOF1/%ED%95%9C%ED%99%94-BEYOND-SW-CAMP-23%EA%B8%B0?node-id=618-102&viewport=696%2C533%2C0.2&t=o5woH0kckAYE5DgS-1&scaling=min-zoom&content-scaling=fixed&starting-point-node-id=618%3A102&page-id=612%3A67)
- [기획서 PDF](docs/WORKFORCE_프로젝트_기획서.pdf)

<br>
<div align="right"><a href="#목차">맨 위로</a></div>
