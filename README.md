<div align="center">
<img width="845" alt="WORKFORCE" src="https://github.com/user-attachments/assets/a8d3b7b8-4880-491f-9073-0dad92340ea5" />

**효율적인 인사 온보딩을 위한 맞춤형 AI 기반 HR 관리 시스템**
  
</div>

> 결재, 계약, 근태, 급여, 목표/평가, ESG, 채팅까지 하나의 시스템에서 통합 관리할 수 있는 맞춤형 인사관리 플랫폼입니다.
> 회사별 정책에 맞게 자유롭게 설정할 수 있도록 설계되어, 복잡한 인사 업무를 자동화하고 AI 챗봇을 통해 쉽고 빠르게 운영할 수 있도록 지원합니다.

<hr>

## 팀원 소개

<div align="center">

| ![김정훈](url1) | ![박세민](url2) | ![이다은](url3) | ![이지연](url4) |
|----------------|-----------------|-----------------|-----------------|
| [김정훈](https://github.com/jeonghuny) | [박세민](https://github.com/semin980520) | [이다은](https://github.com/leeda973) | [이지연](https://github.com/jiyean99) |

</div>

---

## 목차

1. [배경 및 문제 인식](#배경-및-문제-인식)
2. [기술 스택](#기술-스택)
3. [주요 기술](#주요-기술)
4. [시스템 아키텍처](#시스템-아키텍처)
5. [주요 기능](#주요-기능)
6. [트러블슈팅](#트러블슈팅)
7. [프로젝트 시연](#프로젝트 시연)

---

## 배경 및 문제 인식

자체 인사관리 시스템을 구축하기 어려운 조직은 시중 프로그램을 도입하더라도 각 회사 정책과 맞지 않거나, 기능이 복잡해 제대로 활용하지 못하는 문제가 있습니다.
**WORKFORCE**는 회사별 정책에 시스템이 맞춰지고, AI가 사람의 반복 업무를 대신 처리하는 맞춤형 인사관리 플랫폼입니다.

### 1. 시장 분석

국내 HR 시장 규모는 **2025년 약 2조 8,500억 원**에 달할 것으로 추산되며, 채용 시장만 연평균 **6.6% 성장**이 전망됩니다.<sup>[[MTN, 2023]](https://news.mtn.co.kr/news-detail/2023022310563781713)</sup> 글로벌 HR SaaS 시장 또한 2024년 기준 **약 234억 달러** 규모로, HR 테크는 전 세계적으로 가장 빠르게 성장하는 B2B SaaS 영역 중 하나입니다.<sup>[[Wiseguy Reports, 2024]](https://www.wiseguyreports.com/ko/reports/hr-saas-system-market)</sup>

그러나 HR 운영 현장에서는 디지털 성숙도와 전담 인력 확보 수준의 차이로 인해 시스템 도입 이후에도 활용 격차가 발생합니다. 디지털 성숙도 조사에서는 평균 **41.4점** 수준의 낮은 준비도가 확인되었고, 인사 담당자의 **10명 중 8명은 인사 업무 외 다른 업무를 겸직**하고 있어 자사 실정에 맞는 HR 정책 설정과 운영에 구조적 어려움을 겪고 있습니다.<sup>[[KDI, 디지털 성숙도 조사]](https://eiec.kdi.re.kr/policy/domesticView.do?ac=0000156198)</sup><sup>[[다우오피스HR, 2024]](https://hr.daouoffice.com/blog/sme-hr-manager-recruitment-burnout)</sup>

통상임금·근로시간 단축 등 노동제도 변화가 인사관리의 주요 이슈로 부상하면서, HR 테크 기업들이 자동화·AI를 앞세운 SaaS 솔루션으로 시장 선점에 본격 나서고 있습니다.<sup>[[디지털데일리, 2024]](https://www.ddaily.co.kr/page/view/2024050511345817576)</sup>

### 2. 기존 HR 시스템의 3가지 핵심 문제

기존 HR 시스템의 문제는 세 가지 축으로 요약됩니다.

**① 획일성** — 결재 양식·휴가 종류·급여 항목이 고정되어 있어 회사 정책에 맞게 커스터마이징할 수 없습니다. 여러 솔루션을 함께 도입해도 정작 자사 정책을 반영하지 못하는 경우가 많습니다.

**② 단절** — 결재·근태·급여·성과 시스템이 따로 운영되어, 승인 후 후속 처리(연차 차감, 근태 반영, 성과-보상 연계)를 사람이 수동으로 연결해야 합니다.

**③ 관리 부담** — 주 52시간 위반 감지, 연차 사용 촉진 의무(근로기준법 제61조), 급여 계산, 신규 입사자 온보딩 질문 응대 등 반복 업무가 전부 사람 손에 달려 있어 담당자 1인이 과부하를 떠안습니다.

### 3. 해결방안

| 문제 | WORKFORCE 해결 방식                                                                          |
|------|------------------------------------------------------------------------------------------|
| 획일성 | JSON 동적 폼 스키마로 결재 양식 자유 구성, 회사별 휴가·공휴일·급여 항목·조직 구조 독립 관리. 가입 즉시 기본 템플릿 자동 시드             |
| 단절 | Kafka 이벤트 기반으로 결재 승인 → 근태·급여·인사 발령·캘린더 자동 후속 처리. 목표 설정 → 평가 → 연계 구조를 설계     |
| 관리 부담 | 주 52시간 자동 감지·알림, 연차 촉진 1·2차 배치 자동화, Quartz 기반 급여 전 과정 자동 정산, RAG AI 챗봇으로 온보딩·정책 질문 자동 응대 |

> 📎 [Figma 기획서 보기](https://www.figma.com/proto/6suak3PIQpHGzS3UBatOF1/%ED%95%9C%ED%99%94-BEYOND-SW-CAMP-23%EA%B8%B0?node-id=618-102&viewport=696%2C533%2C0.2&t=o5woH0kckAYE5DgS-1&scaling=min-zoom&content-scaling=fixed&starting-point-node-id=618%3A102&page-id=612%3A67) · [기획서 PDF 다운로드](docs/WORKFORCE_프로젝트_기획서.pdf)

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 기술 스택

<table>
  <tr>
      <td valign="middle" width="150">
        <div align="center"><sup><b>Backend · Spring</b></sup></div>
      </td>
      <td>
      <img src="https://img.shields.io/badge/Java-007396?style=flat-square&logo=openjdk&logoColor=white"/>
      <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat-square&logo=springboot&logoColor=white"/>
      <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white"/>
      <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=flat-square&logo=spring&logoColor=white"/>
      <img src="https://img.shields.io/badge/Spring_Cloud-6DB33F?style=flat-square&logo=spring&logoColor=white"/>
      <img src="https://img.shields.io/badge/Spring_Batch-6DB33F?style=flat-square&logo=spring&logoColor=white"/>
      <img src="https://img.shields.io/badge/Apache_Kafka-231F20?style=flat-square&logo=apachekafka&logoColor=white"/>
      <img src="https://img.shields.io/badge/Quartz-CC0000?style=flat-square"/>
      <img src="https://img.shields.io/badge/QueryDSL-4479A1?style=flat-square"/>
      <img src="https://img.shields.io/badge/JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white"/>
      <img src="https://img.shields.io/badge/Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black"/>
      <img src="https://img.shields.io/badge/WebSocket-010101?style=flat-square&logo=socketdotio&logoColor=white"/>
      <img src="https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white"/>
    </td>
  </tr>
  <tr>
    <td valign="middle" width="150">
      <div align="center"><sup><b>Backend · Python</b></sup></div>
    </td>
    <td>
      <img src="https://img.shields.io/badge/Python-3776AB?style=flat-square&logo=python&logoColor=white"/>
      <img src="https://img.shields.io/badge/FastAPI-009688?style=flat-square&logo=fastapi&logoColor=white"/>
      <img src="https://img.shields.io/badge/OpenAI-412991?style=flat-square&logo=openai&logoColor=white"/>
      <img src="https://img.shields.io/badge/Pinecone-000000?style=flat-square"/>
    </td>
  </tr>
  <tr>
    <td valign="middle" width="150">
      <div align="center"><sup><b>Frontend</b></sup></div>
    </td>
    <td>
      <img src="https://img.shields.io/badge/React-20232A?style=flat-square&logo=react&logoColor=61DAFB"/>
      <img src="https://img.shields.io/badge/TypeScript-3178C6?style=flat-square&logo=typescript&logoColor=white"/>
      <img src="https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=vite&logoColor=white"/>
      <img src="https://img.shields.io/badge/Ant_Design-0170FE?style=flat-square&logo=antdesign&logoColor=white"/>
      <img src="https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white"/>
      <img src="https://img.shields.io/badge/Zustand-443E38?style=flat-square"/>
      <img src="https://img.shields.io/badge/TanStack_Router-FF4154?style=flat-square"/>
      <img src="https://img.shields.io/badge/TanStack_Query-FF4154?style=flat-square&logo=reactquery&logoColor=white"/>
      <img src="https://img.shields.io/badge/React_Hook_Form-EC5990?style=flat-square&logo=reacthookform&logoColor=white"/>
      <img src="https://img.shields.io/badge/Axios-5A29E4?style=flat-square"/>
      <img src="https://img.shields.io/badge/Chart.js-FF6384?style=flat-square&logo=chartdotjs&logoColor=white"/>
    </td>
  </tr>
  <tr>
    <td valign="middle" width="150">
      <div align="center"><sup><b>Database</b></sup></div>
    </td>
    <td>
      <img src="https://img.shields.io/badge/MariaDB-003545?style=flat-square&logo=mariadb&logoColor=white"/>
      <img src="https://img.shields.io/badge/Redis-FF4438?style=flat-square&logo=redis&logoColor=white"/>
      <img src="https://img.shields.io/badge/Elasticsearch-005571?style=flat-square&logo=elasticsearch&logoColor=white"/>
    </td>
  </tr>
  <tr>
    <td valign="middle" width="150">
      <div align="center"><sup><b>Cloud / Infra</b></sup></div>
    </td>
    <td>
      <img src="https://img.shields.io/badge/AWS_EKS-FF9900?style=flat-square&logo=amazoneks&logoColor=white"/>
      <img src="https://img.shields.io/badge/AWS_ECR-FF9900?style=flat-square&logo=amazonaws&logoColor=white"/>
      <img src="https://img.shields.io/badge/Amazon_RDS-527FFF?style=flat-square&logo=amazonrds&logoColor=white"/>
      <img src="https://img.shields.io/badge/Amazon_S3-569A31?style=flat-square&logo=amazons3&logoColor=white"/>
      <img src="https://img.shields.io/badge/CloudFront-FF9900?style=flat-square&logo=amazonaws&logoColor=white"/>
      <img src="https://img.shields.io/badge/AWS_IAM-FF9900?style=flat-square"/>
      <img src="https://img.shields.io/badge/n8n-EA4B71?style=flat-square&logo=n8n&logoColor=white"/>
      <img src="https://img.shields.io/badge/cert--manager-326CE5?style=flat-square&logo=kubernetes&logoColor=white"/>
      <img src="https://img.shields.io/badge/ingress--nginx-009639?style=flat-square&logo=nginx&logoColor=white"/>
    </td>
  </tr>
  <tr>
    <td valign="middle" width="150">
      <div align="center"><sup><b>CI / CD</b></sup></div>
    </td>
    <td>
      <img src="https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white"/>
      <img src="https://img.shields.io/badge/GitHub_Packages-181717?style=flat-square&logo=github&logoColor=white"/>
    </td>
  </tr>
  <tr>
    <td valign="middle" width="150">
      <div align="center"><sup><b>Tools</b></sup></div>
    </td>
    <td>
      <img src="https://img.shields.io/badge/Git-F05032?style=flat-square&logo=git&logoColor=white"/>
      <img src="https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white"/>
      <img src="https://img.shields.io/badge/Notion-000000?style=flat-square&logo=notion&logoColor=white"/>
      <img src="https://img.shields.io/badge/Discord-5865F2?style=flat-square&logo=discord&logoColor=white"/>
      <img src="https://img.shields.io/badge/Figma-F24E1E?style=flat-square&logo=figma&logoColor=white"/>
    </td>
  </tr>
</table>

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 주요 기술

<details>
<summary><strong>Common 공통 라이브러리</strong></summary>

<br>

- **구성 개요**
    - 5개 마이크로서비스(member, salary, approval, evaluation, ai)가 공통으로 사용하는 코드를 별도 라이브러리 `workforce-common` 으로 분리
    - GitHub Packages 에 Maven 으로 배포해 각 서비스의 `build.gradle` 의존성에서 끌어다 사용
    - 같은 코드를 여러 서비스에 복붙하지 않고, 변경 시 한 곳만 수정 후 버전 올리면 모든 서비스에 일괄 반영

- **공통 엔티티 베이스**
    - `BaseTimeEntity` 클래스 - 모든 도메인 엔티티가 상속받아 생성일·수정일 컬럼을 자동으로 관리
    - 직접 setter 호출하지 않아도 INSERT 시 `createdAt` 자동 기록, UPDATE 시 `updatedAt` 자동 갱신

- **API 응답 형식 통일**
    - 모든 컨트롤러가 `ApiResponse<T>` 한 가지 봉투로 응답 - `{ "data": ..., "message": "...", "status": 200 }`
    - 에러는 `CommonErrorDto` 로 동일한 키 구조로 응답해 프런트엔드가 한 군데에서 일관된 처리 가능
    - 서비스마다 다른 응답 형식을 만들면 프런트가 분기 처리해야 하는 문제 차단

- **공통 예외 처리**
    - `BusinessException` - 비즈니스 룰 위반 시 던지는 표준 예외 (HTTP 상태 + 메시지)
    - `CommonExceptionHandler` - 모든 서비스에 동일한 예외 처리 로직을 한 번에 적용 (`@RestControllerAdvice` 자동 등록)
    - 서비스마다 별도로 try/catch 안 해도 되고, 에러 응답 형식이 어긋날 일 없음

- **선언적 권한 체크 (`@CheckPermission` AOP)**
    - 컨트롤러 메서드에 어노테이션 한 줄만 붙이면 권한 검증 자동 수행
    - **13개 리소스** (구성원·연차·급여·결재 등) × **4개 액션** (READ/CREATE/UPDATE/DELETE) × **4단계 범위** (본인/팀/부서/전사)
    - 반복되는 권한 검증 코드를 비즈니스 로직에서 분리, 권한 정책 변경 시 어노테이션 정의만 수정하면 전체 적용

- **인프라 설정 표준화**
    - Redis - Pub/Sub 메시지, 마스터 데이터 캐싱, 세션 저장 표준 설정
    - Kafka Producer - 도메인 이벤트 발행용 표준 설정 + 직렬화 룰
    - S3 - 파일 업로드/다운로드 (프로필 사진, 결재 첨부 등) 공통 설정
    - ShedLock - 분산 환경에서 같은 스케줄 잡이 여러 노드에서 중복 실행되지 않도록 락 관리
    - 각 서비스가 인프라 설정을 따로 작성하지 않고 라이브러리만 의존하면 끝

- **알림 타입 통합 정의**
    - `NotificationType` enum 에 40종 이상의 알림 종류 정의 (급여 발행/연차 승인/주 52시간 경고 등)
    - 알림 발송은 Kafka 기반 비동기 - 비즈니스 트랜잭션이 알림 전송 지연에 영향받지 않음
    - 알림 종류 추가 시 enum 한 곳만 수정 -> 모든 서비스가 같은 정의를 공유

- **도메인 이벤트 클래스 모음**
    - 28개 이상의 이벤트 클래스 정의 (근태·결재·계약·인사·급여 도메인 전반)
    - 한 서비스에서 발행한 이벤트를 다른 서비스가 같은 클래스 정의로 받아 처리 - 페이로드 형식 안 어긋남
    - 예: salary-service 의 `PayrollCreatedEvent` 를 ai-service 가 받아서 벡터 DB 갱신

</details>

<details>
<summary><strong>AI 챗봇 (3-Layer RAG + 멀티턴)</strong></summary>

<br>

- **구성 개요**
    - 회사별 정책·결재 양식·실시간 데이터를 기반으로 답하는 사내 AI 챗봇
    - 단순한 일반 챗봇과 달리 **회사마다 다른 데이터를 따로 학습** (회사 A 의 휴가 정책이 회사 B 에 노출되지 않음)
    - 전체 흐름: 사용자 질문 -> 의도 분류 -> 카테고리별 분기 처리 -> 답변 생성

- **3-Layer 지식 저장소**
    - 챗봇이 답할 때 참고하는 자료를 세 단계로 분리해 저장 (검색 우선순위 제어 가능)
    - **Layer 1 (Platform)** - 시스템 공통 가이드 (메뉴 위치·사용법). 모든 회사 공유
    - **Layer 2 (DB Sync)** - 회사 정책 자동 동기화. 휴가/급여/근태/결재 정책이 변경되면 Kafka 메시지로 벡터 저장소가 자동 갱신
    - **Layer 3 (HR Uploaded)** - HR 담당자가 직접 업로드한 회사 내부 규정·가이드 문서. 회사별 격리

- **의도 분류 (8개 카테고리)**
    - 사용자가 입력한 질문을 8개 카테고리 중 하나로 자동 분류 - **멤버 / 캘린더 / ESG / 급여 / 근태 / 정책 / 결재 / 목표**
    - 카테고리에 따라 답변 생성 방식이 갈라짐
    - 분류기는 GPT-4o-mini 가 처리 (간단한 분류는 모델 비용 작음)

- **카테고리별 분기 처리**
    - 정책성 질문 (예: "연차 며칠 받아?") - 벡터 저장소에서 회사 정책 문서 검색 후 답변
    - 실시간 데이터 질문 (예: "내 잔여 연차 얼마야?") - 백엔드 API 직접 호출해서 그 직원의 현재 잔고 조회 후 답변
    - 사내 양식 검색 (예: "휴가 신청서 어디서 받아?") - 양식 인덱스에서 결재 양식 ID 반환 + 즉시 신청 화면 링크 제공

- **벡터 검색 (Pinecone)**
    - 회사 정책·결재 양식·휴가 규정 문서를 OpenAI `text-embedding-3-small` 임베딩 모델로 벡터화해 저장
    - 사용자 질문도 같은 모델로 벡터화한 후 코사인 유사도 기준 상위 N개 문서 추출
    - **Layer 별 가산점 적용** - 시스템 공통 가이드(Layer 1)와 회사 정책(Layer 2)에는 +0.10 가산점, HR 업로드 문서보다 우선순위
    - 회사 ID 필터로 다른 회사 문서가 절대 섞이지 않게 격리

- **답변 생성 (GPT-4o-mini)**
    - 검색된 정책 문서 + 이전 대화 맥락 + 사용자 질문을 한 프롬프트로 묶어 LLM 에 전달
    - LLM 이 회사 정책에 맞춘 답변을 생성, 출처 근거도 함께 노출 가능
    - 모델은 응답 속도와 비용을 고려해 mini 버전 사용

- **워크플로우 오케스트레이션 (n8n)**
    - 의도 분류 -> 카테고리 분기 -> 벡터 검색 -> LLM 호출 -> 답변 후처리 일련의 흐름을 비주얼 워크플로우 도구 n8n 으로 구성
    - 코드 수정 없이 워크플로우 노드 변경만으로 흐름 조정 가능 (예: 새 카테고리 추가, 분기 룰 변경)
    - 운영 화면에서 단계별 입출력을 즉시 디버깅 가능

- **자동 동기화 (Kafka Consumer)**
    - 휴가·급여·근태·결재 정책이 변경되면 도메인 이벤트가 Kafka 토픽 `rag.sync.{leave,attendance,salary,approval}` 으로 발행
    - ai-service 의 Kafka Consumer 가 그 메시지를 받아 벡터 인덱스 갱신
    - 사용자가 정책 수정 직후 챗봇에 물어봐도 최신 정보 기반 답변 (수동 새로고침 불필요)

- **멀티턴 대화 (이전 맥락 유지)**
    - 최근 5턴 + 30분 이내 대화 이력을 함께 LLM 에 전달
    - "연차 며칠 받아?" 답변 후 직원이 "그거 유급이야?" 라고 물어도 이전 답변을 맥락으로 인식해 정확히 답변
    - 단답형 후속 질문도 매끄럽게 처리

- **기술 구성**
    - Python FastAPI - 챗봇 API 서버
    - LangChain - LLM 호출 / 프롬프트 관리 / 벡터 검색 통합 도구
    - OpenAI SDK - 임베딩 및 답변 생성
    - Pinecone - 벡터 저장소 (회사 ID 별 namespace 분리)
    - n8n - 워크플로우 오케스트레이션
    - Eureka 서비스 등록으로 Spring 마이크로서비스 (member/salary/approval) 와 동일 게이트웨이를 통해 호출

</details>

<details>
<summary><strong>Quartz + Spring Batch 기반 급여,근태 배치 처리</strong></summary>

<br>

- **시스템 구성 개요**
    - 급여 정산, 근태 마감, 연차 부여 등 반복적으로 실행되어야 하는 업무를 Quartz 스케줄러 + Spring Batch 로 자동화
    - 회사별로 급여 지급일·근태 마감일·연차 정책이 다르기 때문에 회사 단위로 독립된 배치 트리거를 등록해 운영
    - 법령 의무 성격을 띤 배치(주 52시간 점검, 일일 근태 확정, 연차 촉진 등)는 SaaS 운영자만 통제하는 전사 공통 배치로 분리
    - 전체 흐름: Quartz cron 트리거 → Spring Batch Job (단일 tasklet Step) → Worker (인-프로세스 비즈니스 로직 + 자체 청크 처리)

**① 전사 공통 배치 (SaaS 운영자만 통제)**

| 배치 | 실행 주기 | 처리 내용 |
|------|------|------|
| 초과근무 신청 만료 | 매시 정각 | 사후 신청 만료 시각 도래분 자동 만료 처리 |
| 시차출퇴근 자동 배정 | 매일 01:00 | 스케줄 선택 마감일까지 직원이 미선택 시 기본 스케줄 자동 배정 |
| 일일 근태 초안 생성 | 매일 02:00 | 출퇴근 기록 기반 일일근태 기록 초안 일괄 생성 |
| 미사용 연차 수당 정산 | 매일 02:50 | 이월 불가 연차 체크 + 수당 지급 정책 직원에게 자동 급여 항목 추가 (입사일 기준 부여 회사) |
| 주 52시간 초과 감지 | 매일 03:00 | 전일 기준 주간 근로시간 한도 위반 검사 + 직원·관리자 알림 |
| 연차 만료 처리 | 매일 03:00 | 만료일 도래 연차 잔고를 사용 불가로 전환 |
| 연차 사용 촉진 알림 | 매일 06:30 | 근로기준법 제61조 1차(6개월 전)/2차(2개월 전) 촉진 자동 알림 (1차·2차 기간 커스텀 가능) |
| 일일 근태 확정 | 매일 14:00 | 전날 정정 신청 마감 후 일일근태를 FINALIZED 상태로 확정, 근태 이상 감지 |

**② 회사별 배치 (회사 단위 트리거 - 회사 관리자가 cron 자유 조정 가능)**

| 배치 | 기본 주기 | 처리 내용 |
|------|------|------|
| 연차 부여·이월 | 매월 1일 05:00 | 입사기념일/회계연도/월차(1년 미만자) 정책에 맞춰 연차 부여 + 이월 정책 적용 (입사일 기준 부여 대상자 포함) |
| 정기상여 자동 지급 | 매월 1일 09:00 | 상여정책에 따른 지급 주기(분기/반기/월) 도래 시 정기상여 발행, 매월 확인하여 지급 |
| 월 근태 마감 | 매월 3일 00:00 | 전월 일일 근태 현황 집계 → 월 근태 장부 생성 |
| 급여명세서 산출 | 매월 20일 01:00 | 25일 지급 5일 전 미리 산출 - 4대보험·소득세·가산수당·일할계산·수당 자동 포함 |
| 퇴직금 정산 | 매월 20일 02:00 | 직전 3개월 평균임금 기반 퇴직금 자동 산출 (시행령 제2조 휴직 분모 제외 + 통상시급 일액 비교 + 미사용 연차 수당 정산) |
| 명세서 발송 | 매일 09:00 | 회사별 보정 지급일 일치 시 인앱 알림 발송 (휴일/주말/임시공휴일 자동 보정) |
| 연차 이월 | 매년 1/1 00:00 | 이월 정책 적용 (회계연도 기준 부여 회사) 잔여 연차 이월 처리 |

**③ 설계 포인트**

- **회사별 트리거 분리**
    - 하나의 작업 정의(예: 급여 산출)를 회사 수만큼 복제해 각 회사마다 별도 트리거로 등록
    - 트리거 이름에 회사 ID를 합쳐 식별자로 사용
    - A사는 매월 25일, B사는 매월 말일처럼 회사마다 다른 시각에 돌게 설정 가능
    - 한 회사 일정 변경이 다른 회사에 영향 없음

- **회사 관리자가 직접 일정 조정**
    - 회사 관리자 화면에서 새 일정을 입력하면 즉시 그 회사 트리거만 갈아끼움
    - 메모리뿐 아니라 일정 보관 테이블에도 함께 반영해 서버 재시작 후에도 유지
    - 변경 시점에 이미 실행 중인 작업은 끝까지 돌고, 다음 실행부터 새 일정 적용
    - 서버 재배포 필요 없음

- **전사 일정 vs 회사별 일정 권한 분리**
    - 법령으로 강제되는 작업(주 52시간 점검, 근태 일일 마감, 연차 촉진)은 워크포스 본사 운영자만 조정
    - 회사 정책에 해당하는 작업(급여 지급일, 출퇴근 슬롯 마감일, 연차 부여일)은 회사 관리자만 조정
    - 회사 관리자가 다른 회사 작업을 변경하려 하면 식별자에 본인 회사 ID가 포함됐는지 검사해 차단

- **서버 재시작 안전성**
    - 일정 정보를 메모리가 아닌 데이터베이스에 보관(11개 전용 테이블)
    - 재시작·재배포 시 자동으로 복구되어 빠뜨리는 작업 없음
    - 일정 표현식이나 다음 실행 시각도 같은 테이블에서 즉시 갱신됨

- **한국 시간대 명시**
    - 스케줄러 기본 시간대가 서버 운영체제(컨테이너 환경에서는 흔히 UTC)를 따라가서, 한국 오전 9시로 등록한 작업이 실제로는 오후 6시에 도는 사고가 잘 남
    - 모든 트리거에 한국 시간대를 명시 적용해 차단

- **명세서 발송 휴일 자동 보정**
    - 매일 09:00 폴링하면서 그 시점의 휴일 테이블 + 정책을 즉시 반영
    - 25일이 일요일이면 24일 또는 26일로 자동 이동, 임시공휴일 지정도 발화 시점에 그대로 반영
    - 회사 정책 변경(지급일·보정 룰)도 다음 폴링부터 즉시 반영, 별도 갱신 잡 불필요

- **명세서 발송 알림 시나리오**
    - 회사 보정 지급일 일치 시 직원에게 인앱 알림 자동 발행 (`급여 명세서 발행` 이벤트 → Kafka → 알림 저장 + WebSocket 푸시)
    - 직원이 알림 클릭 → 명세 상세 페이지 → PDF 다운로드 버튼으로 즉시 명세서 확인
    - 메일 발송 부담 없이 출근 직후 알림 받는 자연스러운 흐름

- **커밋 후 발행 + 발행함 패턴**
    - 배치 작업 중간에 다른 서비스로 이벤트를 곧바로 발행하면 트랜잭션이 롤백돼도 이미 외부로 나간 이벤트는 되돌릴 수 없음
    - `@TransactionalEventListener(AFTER_COMMIT)` 으로 묶어 트랜잭션이 정상 커밋된 직후에만 발행
    - 더 안전한 경로로 이벤트를 별도 발행함 테이블에 먼저 적어두고, 폴링 워커가 5초마다 그 테이블을 읽어 메시지 큐로 흘려보내는 구조 적용
    - 메시지 큐가 잠시 죽어도 이벤트 유실 없음

- **중복 실행 방지**
    - 연차는 이미 받은 사람 ID 집합을 미리 조회해 건너뜀
    - 급여는 회사·직원·지급일·급여 종류 조합으로 데이터베이스 유일성 제약 → 중복 생성 자체가 차단됨
    - 이전에 삭제된 이력도 정리해서 재생성 가능하도록 구분

- **회사 탈퇴 시 자동 정리**
    - 회사가 서비스에서 빠질 때 그 회사가 등록했던 모든 회사별 작업 트리거를 일괄 제거
    - 일정 보관 테이블에서도 동시에 삭제되므로 잔여 트리거가 빈 회사 데이터를 찾으며 도는 사고 없음

</details>

<details>
<summary><strong>동기/비동기 통신 설계 및 이벤트 기반 후속 처리</strong></summary>

<br>

- **구성 개요**
    - MSA 환경에서 전자결재 서비스가 계정관리(member-service), 근태, 급여(salary-service) 와 통신하는 상황이 잦음
    - 결재 트랜잭션 안에서 **즉시 결과가 필요한 데이터인가** vs **승인 이후 후속 처리인가** 를 기준으로 통신 방식 분리
    - 즉시 필요한 데이터만 동기 호출, 나머지는 전부 비동기 메시지로 분리해 서비스 간 느슨한 결합 확보

- **동기 통신 (FeignClient) - 즉시 결과 필요할 때만 최소 적용**
    - HTTP 호출 기반, 호출한 쪽이 응답 받을 때까지 기다림
    - 다른 서비스가 죽으면 호출한 쪽도 멈추므로 **꼭 필요한 케이스에만** 사용
    - 적용 케이스 한정:
        - 결재라인 편집 화면에서 후보자 리스트를 즉시 보여줘야 할 때 (member-service 호출)
        - 기안 생성 트랜잭션 안에서 기안자의 직책·조직 정보를 함께 검증·저장할 때
        - 승인 트랜잭션 안에서 결재자 서명 이미지를 즉시 문서에 박아 저장할 때

- **비동기 통신 (Kafka) - 후속 처리는 전부 메시지로 분리**
    - 결재 승인 트랜잭션은 결재 도메인 데이터 저장만 하고 끝, 다른 서비스에 영향을 줘야 할 작업은 이벤트로 발행
    - 받는 서비스가 잠시 죽어 있어도 메시지 큐에 쌓여 있다가 살아나면 소비 - **결재 승인 자체가 막히지 않음**
    - 도메인 이벤트 종류:

| 요청 타입 | 발행 이벤트 | 후속 처리 |
|---|---|---|
| VACATION | LeaveApprovalEvent | 연차 잔고 차감 |
| ATTENDANCE | OvertimeApprovalEvent | 연장근무 정산 반영 |
| ATTENDANCE | AttendanceCorrectionEvent | 출퇴근 정정 반영 |
| ATTENDANCE | EarlyLeaveEvent | 조퇴 근태 반영 |
| HR_MOVEMENT | LeaveOfAbsenceEvent | 휴직·복직 상태 변경 |
| HR_MOVEMENT | ResignationEvent | 퇴직 프로세스 시작 |
| SALARY | AllowanceApprovalEvent | 수당 급여 정산 반영 |
| 공통 | ScheduleApprovalEvent | 근무일정 반영 |
| 공통 | CalendarApprovalEvent | 캘린더 일정 등록 |

- **검색 인덱스 동기화는 Outbox 패턴으로 별도 분리**
    - 결재 도메인 변경 이벤트를 곧바로 Kafka 로 보내면 트랜잭션 롤백 시 이미 발행된 메시지를 되돌릴 수 없음
    - 도메인 변경과 같은 트랜잭션 안에서 outbox 테이블에 row 적재 -> 별도 폴링 워커가 5초 간격으로 미발행 row 만 골라 발행
    - 발행 완료된 row 는 7일 후 자동 정리 (cron 잡으로 처리)
    - Kafka 가 잠시 죽어도 outbox 에 메시지 남아 있어 유실 없음

- **설계 원칙 한 줄 요약**
    - "결재 트랜잭션 안에서 꼭 필요한 데이터만 동기 호출, 나머지는 전부 메시지로 분리"
    - 한 서비스 장애가 다른 서비스 트랜잭션을 막지 않는 구조

</details>

<details>
<summary><strong>회의 녹음 AI 요약</strong></summary>

<br>

- **시스템 구성 개요**
    - 인사관리 플랫폼은 회사 내부 회의가 잦은 만큼 회의 내용 기록 + 후속 액션 정리가 자주 필요
    - 녹음 파일을 올리면 자동으로 텍스트로 변환하고 LLM 이 정리된 회의록을 생성
    - 회의에서 도출된 액션 아이템은 목표·평가 시스템에 연결되어 추적 가능

- **처리 파이프라인 (녹음 -> 텍스트 -> 회의록)**
    - 1단계 - 사용자가 녹음 파일을 챗봇 화면 또는 회의 페이지에서 업로드
    - 2단계 - 음성 인식(STT) 모듈이 음성을 텍스트로 변환
    - 3단계 - LLM 이 그 텍스트를 정해진 형식으로 정리해 회의록 생성

- **음성 인식 (STT)**
    - OpenAI `whisper-1` API 사용
    - 지원 포맷: webm / m4a / mp3 / wav 등 일반 녹음 포맷
    - 최대 파일 크기 25MB - 한 시간 분량 회의는 보통 충분히 들어감
    - 변환 결과는 단일 텍스트 블록으로 받아 다음 단계로 전달

- **회의록 생성 (LLM)**
    - 모델: GPT-4o-mini, `temperature 0.3` (창의성 낮춰 정확도 우선)
    - 출력 형식 고정 - 다음 5개 섹션을 항상 같은 구조로 생성:
        - 회의 요약 (한 줄)
        - 주요 결정사항
        - 액션 아이템 (담당자·기한 포함)
        - 주요 논의 내용
        - 이후 일정 및 후속 사항
    - 형식이 고정돼 있어 회의록 검색·자동 추적이 용이

- **재요약 기능**
    - 별도 엔드포인트 `/ai/transcribe/summary` - 이미 작성된 텍스트나 회의록을 다시 요약 가능
    - 회의록을 다른 길이·관점으로 보고 싶을 때 활용

- **성과 관리 연동**
    - 회의록에서 도출된 액션 아이템(담당자·기한)을 목표·평가 시스템에 연결
    - 회의에서 결정된 일이 그대로 잊히지 않고 추적 가능한 작업으로 이어짐

- **기술 구성**
    - Python FastAPI - AI 서비스 API 서버
    - OpenAI SDK - Whisper(STT) 와 GPT-4o-mini(LLM) 호출
    - Eureka 서비스 등록 - Spring 마이크로서비스와 같은 게이트웨이를 통해 통합 호출

</details>

<details>
<summary><strong>SSE 실시간 알림</strong></summary>

<br>

- **시스템 구성 개요**
    - 결재 승인, 근태 이상 감지, 계약 발송, 급여 명세서 발행 등 주요 이벤트 발생 시 사용자 화면에 즉시 알림
    - 사용자가 새로고침하지 않아도 알림 종 모양 뱃지가 실시간으로 갱신됨
    - 다중 서버 인스턴스가 떠 있어도 어느 서버에 연결된 사용자든 알림이 빠짐없이 전달

- **3단계 파이프라인 (Kafka -> Redis Pub/Sub -> SSE)**
    - 1단계 - 도메인 서비스가 이벤트 발생 시 Kafka 토픽에 메시지 발행
    - 2단계 - 알림 컨슈머가 Kafka 메시지를 받아 Redis 채널에 다시 발행
    - 3단계 - 모든 서버 인스턴스의 SSE 핸들러가 Redis 구독 -> 자기에게 연결된 사용자에게 전달

- **Kafka (1단계 - 발행)**
    - 각 도메인 서비스(급여, 근태, 전자결재 등)가 도메인 이벤트를 알림 토픽에 발행
    - 발행 시점은 트랜잭션 커밋 직후 - 롤백된 작업의 알림이 잘못 나갈 일 없음

- **Redis Pub/Sub (2단계 - 다중 인스턴스 브로드캐스트)**
    - 사용자가 여러 서버 인스턴스 중 하나에만 연결되어 있는데, 알림은 어느 서버가 발행해도 전달돼야 함
    - Redis 채널을 모든 서버가 구독해 두면, 한 서버가 발행한 알림이 모든 서버로 즉시 퍼짐
    - 알림 발행한 서버 ≠ 사용자가 연결된 서버 인 경우에도 정상 전달

- **SSE (3단계 - 클라이언트 푸시)**
    - 서버에서 클라이언트로의 단방향 실시간 연결
    - WebSocket 대비 단순 (HTTP 위에서 동작, 양방향 불필요한 알림에 적합)
    - 사용자가 페이지 열면 SSE 연결 자동 생성 -> 알림 발생 시 서버가 푸시 -> 화면 종 모양 뱃지 +1

- **운영 측면**
    - 사용자가 페이지 닫으면 SSE 연결도 끊김 - 별도 정리 작업 불필요
    - 사용자가 잠시 오프라인이어도 알림 자체는 DB에 저장되어 있어 다시 접속하면 누적된 알림 확인 가능
    - 즉 SSE 는 "실시간 채널" 역할, DB 저장은 "안전망" 역할로 분리

</details>

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 시스템 아키텍처

<!-- TODO: 아키텍처 다이어그램 이미지 -->

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 주요 기능
<details>
<summary><strong>조직 · 구성원 관리</strong></summary>

<br>

| 기능 | 상세 |
|---|---|
| 조직 관리 | 회사별 다단계 조직 트리 - 본부/팀/파트 등 자유롭게 계층 구성. 조직마다 직급(과장, 사원 등)과 직책(팀장·팀원)을 별도 관리하며, 직원이 부서를 옮기면 결재 후속 처리로 인사 발령 이력이 자동으로 기록 |
| 역할·권한 | 13개 리소스(구성원, 연차, 급여, 결재 등) × 4개 액션(조회, 등록, 수정, 삭제) × 4단계 범위(본인/팀/부서/전사)로 권한 정의, 회사 가입 시 기본 역할 4개(시스템관리자/HR관리자/팀장/팀원) 자동 생성, 회사가 직접 커스텀 역할도 추가 가능 |
| 회사별 설정 | 공휴일, 휴가 종류, 근무 정책, 급여 정책, 상여 정책, 퇴직급여 정책을 회사 단위로 독립 운영 |
| SaaS 온보딩 | 회사 가입 시 기본 조직 트리, 기본 역할, 연 단위 공휴일, 표준 급여 항목 템플릿이 자동으로 시드 - 가입 직후 바로 운영 가능 |
| 조직 검색 | 사번·이름·이메일·부서·직급으로 빠른 검색 + 회사 ID 필터 |

</details>

<details>
<summary><strong>근태 관리</strong></summary>

<br>

| 기능 | 상세 |
|---|---|
| 출퇴근 기록 | 웹에서 출근/퇴근 클릭 -> 출결 로그 저장 -> 일별 근태 자동 집계, 슬림 토글 + 하루 도넛 차트로 정상근무·연장·외근 비중을 즉시 시각 확인 |
| 연장·야간·휴일 근무 | 결재 양식 기반 사전·사후 신청 -> 승인 시 회사 초과근무 정책에 따라 가산수당 자동 산출 (1.5배·2배 등 정책별 차등). 주 52시간 한도와 일일·월간 한도까지 자동 검증 |
| 출퇴근 정정 | 누락·오류 시 결재로 정정 신청 -> 승인 시 근태 정정 이벤트 발행 -> 일별 근태 집계가 자동 갱신. 직원 셀프 정정 별도 경로 없이 모두 결재 워크플로우로 통일해 변경 이력 보존 |
| 휴가 관리 | 회사별 휴가 종류 자유 설정. 신청 시 잔고 사전 체크, 승인 시 비관적 락(같은 행을 두 사람이 동시에 변경하지 못하게 잠금)으로 동시성 안전한 잔고 차감, 비연속 날짜 신청(예: 1일·5일·10일 따로따로) 지원 |
| 연차 자동 부여 | 매월 1일 새벽 5시 배치 워커가 회사 정책에 맞춰 연차 자동 부여 - 입사기념일 기준·회계연도 기준 둘 다 지원, 1년 미만자에게는 월차도 자동 부여 |
| 연차 사용 촉진 | 근로기준법 제61조에 따른 1차(만료 6개월 전) / 2차(만료 2개월 전) 자동 알림 - 미사용 잔여일을 직원별로 안내하고, 본인이 그대로 사용 계획 신청까지 한 화면에서 처리 가능 |
| 출장·외근 | 외근·출장 근태를 별도 카테고리로 구분, 일별 근태 캘린더에 별도 색상·태그로 표기 |
| 시차출퇴근 | 회사가 정의한 출근 슬롯(예: 8시·9시·10시 시작) 중 직원이 매월 선택. 마감일까지 미선택 시 매일 새벽 1시 배치가 기본 슬롯 자동 배정 |
| 일일 근태 마감 | 매일 새벽 2시 초안 생성(휴가 자동 반영 + 근로시간 분류 + 이상 감지) -> 오후 2시 미퇴근자 자동 퇴근 보강 후 봉인. 이중 단계로 직원 셀프 정정 시간 확보 |
| 월 마감 | 매월 3일 새벽 배치 워커가 전월 일별 근태를 집계해 월별 근태 장부를 생성. 이후 정산은 월 장부를 기준으로 |

</details>

<details>
<summary><strong>급여 관리</strong></summary>

<br>

| 기능 | 상세 |
|---|---|
| 급여 정책 | 회사별 정산일·지급월(현재 달 / 이전 달)·말일 보정·연봉제 vs 호봉제 분기. 한 회사에 활성 정책 1개만 유지하며, 신규 정책 등록 시 기존 활성 정책은 자동 종료 |
| 급여 항목 템플릿 | 회사가 자체적으로 사용할 지급·공제 항목 템플릿 등록. **고정 금액 적용 여부** 확인으로 "기본 금액이 정해진 항목" vs "직원마다 차등 입력하는 항목" 구분 |
| 자동 4대보험·소득세 | 워크포스 본사가 연도별 4대보험·소득세 비율을 한 곳에서 관리. 정산 시 국민연금·건강보험·장기요양보험·고용보험·소득세·지방소득세가 자동 산출되어 별도 라인으로 추가됨 |
| 통상임금·가산수당 | 통상임금 해당 항목들을 합산해 통상시급 산정 -> 월별 근태 장부의 연장·야간·휴일 근무 분에 1.5배·2배 자동 적용 |
| 일할 계산 | 입사·퇴사월에 자동으로 일할 계산 적용 -> 모든 지급 항목에 동일 비율로 비례 차감. 휴직 기간도 분모에서 제외 |
| 무급휴직 보정 | 직원 휴직 이력 확인 -> 무급 휴직 기간만큼 정산 일수에서 차감 -> 일할 계산 분모 자동 조정 |
| 수당 관리 (옵트인) | 회사 공통 항목과 개인 차등 항목 모두 **명시 부여 방식**으로만 적용. 자동으로 모든 직원에게 적용되는 흐름 제거하고 관리자가 직원별로 직접 부여한 수당만 정산 라인에 들어감 -> "받지 말아야 할 직원이 자동으로 받는" 사고 차단 |
| 수당 부여 모달 | 직원 선택 후에만 항목 선택 가능. 그 직원에게 이미 부여된 고정 항목은 dropdown 에서 자동 제외, 차등 항목은 선택 시 그 직원이 현재 받는 금액을 자동 채움 |
| 수당 영구 종료 | 영구 종료 시 부여 마스터 기록 + 미지급(DRAFT) 명세서의 같은 항목 라인도 함께 정리. 이미 지급된 명세서는 보존 |
| 명세서 일괄 산출 | 자동 배치 누락이나 정책 변경 시 관리자가 [명세서 생성] 버튼으로 회사 단위 일괄 산출. 자동 배치와 동일한 처리 경로 재사용 -> 결과 일관성 보장 |
| 미사용 연차 수당 | 매일 새벽 2시 50분 배치가 만료 도래 연차 잔고 + 정책 조건(이월 불가 + 정산 허용) 검사 -> 통상시급 × 8h × 미사용 일수 자동 산정 후 다음 달 명세서에 라인 추가 |
| 소급 정산 | 정책·기본급 사후 변경 시 과거 명세서와 차액 산출 -> 별도 소급 명세서 생성, 기존 명세서는 그대로 보존 |
| 다중 선택 일괄 처리 | 정산 처리 탭에서 체크박스 다중 선택 후 [일괄 확정 / 일괄 지급 / 일괄 삭제]. 이미 지급 완료된 명세서는 삭제 대상에서 자동 제외 |
| 명세서 상태 흐름 | 작성 중·수정 가능 -> 확정·수정 불가 -> 지급 완료, 직원 화면은 검토 중인 임시 명세서가 직원에게 보이지 않음 |

</details>

<details>
<summary><strong>상여 · 성과급 관리</strong></summary>

<br>

| 기능 | 상세 |
|---|---|
| 보너스 정책 | 한 회사에 정기상여·성과급·명절상여 세 종류를 한 정책에서 통합 관리. **정기상여**(연 누계 한도·분할 횟수), **성과급**(1회당 최대 비율·평가 등급별 차등율), **명절상여**(정액 또는 정률), **공통 조건**(지급 대상·최소 근속 개월 수·휴직자 제외 토글) 한 화면에서 한 번에 설정 |
| 한도 검증 | 발행 화면에서 정책 한도 초과 입력을 자동 차단. 입력 필드의 최댓값 동적 적용 + 폼 검증 이중 처리로 실수 방지 |
| 정기상여 | 분기·반기 등 정해진 회차로 자동 발행. 회차당 권장 비율 안내(연 누계 한도 ÷ 회수). 매월 1일 09시 배치가 지급 월 도래 여부 자체 검증 |
| 성과급 | 평가 시즌 결과 [평가 결과 불러오기] 버튼으로 prefill - 등급별 차등율(S 15% / A 10% / B 5% / C 0% 등) 자동 매핑해 직원별 행 생성. 관리자가 일부 행만 수정 가능 |
| 명절상여 | 정책에서 정액 또는 정률 사전 설정. 발행 시 비율 자동 적용, 발행 화면에서는 수정 불가(정책 일관성 보장) |
| 발행 흐름 | 시뮬 미리보기로 직원별 산출액 확인 -> [지급 ON/OFF 토글]로 개별 직원 제외 가능 -> [발행]으로 성과급/명절상여 명세서 일괄 생성 |
| 알림 | 직원에게 정기상여 발행 시 인앱 알림 자동 발송. 직원이 자기 명세서 화면에서 즉시 확인 가능 |

</details>

<details>
<summary><strong>퇴직금 관리</strong></summary>

<br>

- **시스템 구성 개요**
    - 회사가 사용하는 퇴직 제도(법정 퇴직금 / 확정급여형 / 확정기여형)를 정책으로 등록
    - 직원 사직서 결재 승인 시 자동으로 퇴직금 산출 + 별도 명세서 생성
    - 평균임금·기본급·근속일수·미사용 연차 수당까지 모두 반영

| 제도 | 산식 | 운용 방식 |
|---|---|---|
| 법정 퇴직금 (LEGAL) | 평균임금 일액 × 30 × 근속일수 ÷ 365 = "1년 근속할 때마다 한 달치 평균임금" | 회사가 사내에 적립, 퇴사 시 회사 자금에서 지급 |
| 확정급여형 (DB) | 산식은 법정과 동일 | 외부 운용기관(보험사·증권사)에 사용자 명의로 적립, 운용 손익은 회사가 부담 |
| 확정기여형 (DC) | 기본급 × DC 기여율(%) × 12 × 근속연수 = "매월 (기본급 × 기여율) 만큼 직원 계좌 적립" | 직원 개인 가입자 계좌에 매월 적립, 운용 손익은 직원이 부담. 법정 최저 기여율 8.33%(=1/12) |

- **평균임금 일액**
    - 시행령 제2조 - 직전 3개월 임금 총액 ÷ 그 기간의 총 일수
    - 임금 총액에는 기본급 + 정기상여 + 각종 수당 + 연장·야간·휴일근로수당 모두 포함
    - 휴직 기간은 분모·분자에서 함께 제외

- **퇴직 시뮬레이션**
    - 직원 본인이 [퇴직 시뮬레이션] 화면에서 예상 퇴사일 입력 -> 즉시 예상 퇴직금 미리보기
    - 미사용 연차 수당 자동 추정(현재 잔여 연차 × 통상시급 × 8h)
    - 한국 국세청 산식 기반 퇴직소득세 추정값까지 함께 계산해 세후 금액 노출

- **자동 적립 라인 (DC)**
    - 매월 정산 시 직원 기본급 × DC 기여율 자동 산출 -> 명세서에 별도 라인 추가
    - 사용자(회사 외부 운용기관 송금)는 별도 외부 시스템으로 송금하되, 명세에는 자동 반영

</details>

<details>
<summary><strong>전자결재 · 계약</strong></summary>

<br>

| 기능 | 설명 |
|------|------|
| 전자결재 | 결재 요청(상신) -> 단계별 승인·반려 -> 후속 처리 자동 트리거. 임시저장·결재 회수·대리결재·참조·공람 모두 지원 |
| 동적 결재 양식 | JSON 기반 양식 정의로 회사가 직접 결재 양식 제작. 텍스트·날짜·셀렉트·텍스트영역 등 다양한 입력 필드 자유 구성, 코드 수정 없이 추가/제거 가능 |
| 결재 후속 처리 | 승인 완료 시 요청 타입별 자동 처리 - 휴가 잔고 차감, 근태 정산 반영, 인사 발령 이력 생성, 캘린더 연동 등. 도메인 이벤트(Kafka) 비동기 발행으로 결재 트랜잭션과 분리 |
| 임시저장·회수 | 작성 중 임시저장 가능, 상신 후에도 결재 진행 전이면 회수 가능 |
| 결재 정정 신청 | 일일 근태 정정·출퇴근 시각 수정도 결재 양식으로 처리 -> 승인 시 근태 즉시 재계산 + 봉인된 일별 근태도 LOCKED 가 아니면 갱신 |

- **전자계약 서명 프로세스**
    - 계약서 생성부터 서명 완료까지 전체 생명주기 관리
    - **계약 상태 흐름**: 생성 -> 발송 -> 서명완료 / 반려 / 취소

| 단계 | 설명 |
|---|---|
| 자동 정보 채움 | 계약서 발송 시 다른 서비스 호출(`FeignClient`)로 인사 정보(member-service) + 급여 정보(salary-service) 조회해 계약서 필드 자동 채움 - 입사일·연봉·직책 등 |
| 법인인감 자동 서명 | 회사 측 서명은 등록된 법인인감 이미지(`sealImageUrl`) 로 자동 처리, 직원 서명만 대기 상태로 발송 |
| 일괄 발송 | 일괄 발송함으로 다수 직원에게 동일 계약서 한 번에 생성·발송 (예: 신입 30명에게 근로계약서 일괄) |
| 재발행 이력 추적 | 이전 계약 ID + 개정 회차 조합으로 재발행 이력 체인 연결 - 어느 계약이 어느 계약을 갱신했는지 추적 가능 |
| 서명 완료 PDF | 양측 서명이 모두 완료되면 자동으로 PDF 생성해 클라우드(Url) 저장. 직원 마이페이지에서 언제든 다운로드 |
| 계약 번호 자동 채번 | 계약 번호는 회사별로 순차 번호 생성 - 누락·중복 방지 |

</details>

<details>
<summary><strong>목표 · 성과 관리</strong></summary>

<br>

| 기능 | 상세 |
|---|---|
| 목표 관리 | 시즌(분기·반기·연간) 단위 OKR/KPI 설정. 직원이 활동 기록을 누적하면 진척률(완료/전체)이 자동 계산되어 시즌 마감 시점에 즉시 확인 |
| 평가 설계 | 평가 시즌별 평가 항목·배점·가중치를 자유 구성. 평가 항목별로 어느 평가자(자기/상사/동료/하급자) 가 응답할지 매핑 가능 |
| 다면 피드백 | 자기·상사·동료·하급자 평가 응답을 평가 응답 테이블에 수집. 익명·실명 정책을 회사가 선택, 동료 평가는 익명 디폴트 |
| 부서 간 보정 | 보정 단계로 부서별 점수 편향 제거. 등급 분포 가이드(예: S 5%, A 20%, B 60%) 표시해 관리자가 시각적으로 분포 조정 |
| 결과 발행 | 평가 시즌이 확정 상태가 되면 평가결과 발행 이벤트 발행 -> 알림 자동 생성 + 성과급 화면의 prefill 데이터로 자동 연동 |
| 리포트 | 평가 결과 자동 리포트 - 부서별·직급별·등급별 분포 시각화. 관리자 화면에서 차트로 즉시 확인 |
| 회의록 액션 연동 | 회의 녹음 AI 요약에서 도출된 액션 아이템(담당자·기한)을 목표 시스템에 직접 연결 - 회의에서 결정된 일이 자연스럽게 추적 가능한 작업으로 이어짐 |

</details>

<details>
<summary><strong>ESG 활동 관리</strong></summary>

<br>

| 기능 | 설명 |
|------|------|
| ESG 활동 | 환경·사회·거버넌스 활동 신청 -> 관리자 승인 -> 포인트 적립. 캠페인(예: "한 달 텀블러 사용") 등록·진행 관리 |
| 포인트 교환 | 적립 포인트로 사내 ESG 쇼핑몰에서 물품 교환. 회사가 상품 카탈로그 직접 등록, 재고 관리 |
| 참여 현황 | 인사 데이터(부서·직급) 와 자동 연동해 부서별·직급별 ESG 참여 현황 분석. 회사 ESG 평가 보고서에 활용 가능 |
| 캠페인 | 회사 단위 ESG 캠페인 등록·기간 설정·참여자 모집·결과 집계 일관 관리 |

</details>

<details>
<summary><strong>협업 · 공통</strong></summary>

<br>

| 기능 | 설명 |
|------|------|
| 실시간 채팅 | 웹소켓(`STOMP`) 기반 사내 메신저. 1:1 / 그룹 채팅 모두 지원, 파일 공유, 메시지 검색 |
| 통합 검색 | 검색엔진 기반으로 결재·인사·조직 등 흩어진 데이터를 한 번에 검색. 도메인 변경 시 발행함(Outbox) 패턴으로 5초마다 인덱스 자동 동기화 -> 항상 최신 데이터 검색 가능 |
| 알림 | Kafka -> Redis 발행/구독 -> 서버 -> 클라이언트 푸시(SSE) 3단 파이프라인. 결재·근태·계약·급여 명세서 발행 등 주요 이벤트 발생 시 화면 새로고침 없이 즉시 알림 종 모양 뱃지 갱신 |
| 캘린더 | 결재 승인 건(휴가·출장·외근 등) 자동 캘린더 등록. 회사 공휴일 + 회사별 정의 기념일 + 직원 본인 일정 한 화면에서 조회 |
| AI 챗봇 | 회사 정책·결재 양식·실시간 데이터 기반 사내 챗봇. "내 잔여 연차 얼마야?" 같은 실시간 데이터 질문은 백엔드 API 직접 호출, "휴가 어떻게 쓰는 거야?" 같은 정책성 질문은 회사별 벡터 저장소 검색 |
| AI 회의록 | 회의 녹음 파일 업로드 -> 음성 인식 -> LLM(GPT-4o-mini) 으로 5개 섹션 구조화 회의록 자동 생성 (요약·결정사항·액션 아이템·논의 내용·후속 일정). 액션 아이템은 목표 시스템에 자동 연결 가능 |

</details>
<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 트러블슈팅

<!-- TODO: 주요 기술적 문제 → 원인 → 해결 사례 3개 정도 -->

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 프로젝트 시연

## 5. 프로젝트 시연

<details>
<summary>프로젝트 시연 보기</summary>

<br/>

<details>
<summary>[관리자]  </summary>
<div markdown="1">

<img width="1330" height="1547" alt="" src="./asset/demo/회원가입_일반.gif" />
  - OAuth 2.0 인증 플로우

</div>
</details>

## 부록

- [ERD 전체 보기](https://www.erdcloud.com/d/Er4amBY2KpweBcKTk)
- [프로그램 사양서 및 단위테스트결과서](https://documenter.getpostman.com/view/51059789/2sBXqDrNZT)
- [요구사항 명세서](https://docs.google.com/spreadsheets/d/107CuIWefCSjBbwO0mnKdQJzK5WCFDKYtw5Rs18liJw8/edit?gid=63147339#gid=63147339)
- [WBS](https://docs.google.com/spreadsheets/d/107CuIWefCSjBbwO0mnKdQJzK5WCFDKYtw5Rs18liJw8/edit?gid=395838842#gid=395838842)
- [Figma 기획서](https://www.figma.com/proto/6suak3PIQpHGzS3UBatOF1/%ED%95%9C%ED%99%94-BEYOND-SW-CAMP-23%EA%B8%B0?node-id=618-102&viewport=696%2C533%2C0.2&t=o5woH0kckAYE5DgS-1&scaling=min-zoom&content-scaling=fixed&starting-point-node-id=618%3A102&page-id=612%3A67)
- [기획서 PDF](docs/WORKFORCE_프로젝트_기획서.pdf)
