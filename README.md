<div align="center">
<img width="845" alt="WORKFORCE" src="https://github.com/user-attachments/assets/a8d3b7b8-4880-491f-9073-0dad92340ea5" />

**효율적인 인사 온보딩을 위한 중소기업 맞춤형 AI 기반 HR 관리 시스템**
  
</div>

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

---

## 배경 및 문제 인식

자체 인사관리 시스템을 구축하기 어려운 중소·중견기업은 시중 프로그램을 도입하더라도 각 회사 정책과 맞지 않거나, 기능이 복잡해 제대로 활용하지 못하는 문제가 있습니다.

**WORKFORCE**는 회사별 정책에 맞게 설정하여 사용할 수 있고, 사용 중 어려운 부분은 AI 챗봇에게 물어보며 쉽게 해결할 수 있는 인사관리 플랫폼입니다.

### 1. 시장 분석

국내 중소기업은 평균 5개 가량 기업용 솔루션을 사용하는 것으로 나타났고, 이 가운데 4분의 1은 직원 혼자 기업용 솔루션을 관리하고 있습니다. 전문 담당자와 예산이 부족해 실정에 맞는 정책 설정에 어려움을 호소하고 있습니다.

한편 중소기업기술정보진흥원에 따르면, 매출 500억원 이상 중소기업의 HR 시스템 구축률은 2021년 41%에서 2024년 79%까지 빠르게 증가하고 있습니다. 통상임금과 근로제도 변화가 중소기업 인사관리의 주요 이슈로 자리잡으면서, 노동규제와 인력 운영 환경에 맞춰 HR 테크 기업들이 자동화·지능화를 앞세운 솔루션을 통해 시장 선점에 본격 나서고 있습니다.

### 2. 해결방안

* **회사 맞춤형 설정**: JSON 동적 폼 스키마로 결재 양식을 자유롭게 구성하고, 회사별 휴가 종류·공휴일·조직 구조를 독립 관리
* **급여·근태 자동화**: 급여 계산·상여금·퇴직금·세금·소급 정산을 자동 처리하고, 연차 촉진 알림·52시간 초과근로 위반 감지
* **AI 챗봇 안내**: 사용법과 회사 정책을 자연어로 질문하면 해당 회사 상황에 맞는 답변 제공, 특정 담당자에게 질문이 몰리는 문제 해소
* **성과관리 체계 구축**: 목표 설정 → 활동 기록 → 평가 → 피드백 → 보정(Calibration)까지 하나의 흐름으로 관리

> 📎 [Figma 기획서 보기](https://www.figma.com/proto/6suak3PIQpHGzS3UBatOF1/%ED%95%9C%ED%99%94-BEYOND-SW-CAMP-23%EA%B8%B0?node-id=618-102&viewport=696%2C533%2C0.2&t=o5woH0kckAYE5DgS-1&scaling=min-zoom&content-scaling=fixed&starting-point-node-id=618%3A102&page-id=612%3A67) · [기획서 PDF 다운로드](docs/WORKFORCE_프로젝트_기획서.pdf)

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 기술 스택

### Backend - Spring
![Java](https://img.shields.io/badge/java-007396?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/spring%20boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/spring%20security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/spring%20data%20jpa-6DB33F?style=for-the-badge)
![JWT](https://img.shields.io/badge/jwt-000000?style=for-the-badge)
![Lombok](https://img.shields.io/badge/lombok-BC4521?style=for-the-badge)
![Gradle](https://img.shields.io/badge/gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Swagger](https://img.shields.io/badge/swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=white)
![WebSocket](https://img.shields.io/badge/websocket-000000?style=for-the-badge&logo=socketdotio&logoColor=white)

### Backend - Python
![Python](https://img.shields.io/badge/python-3670A0?style=for-the-badge&logo=python&logoColor=ffdd54)
![FastAPI](https://img.shields.io/badge/FastAPI-005571?style=for-the-badge&logo=fastapi)
![ChatGPT](https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white)
![LangChain](https://img.shields.io/badge/langchain-%231C3C3C.svg?style=for-the-badge&logo=langchain&logoColor=white)

### Frontend
![Vue.js](https://img.shields.io/badge/vue.js-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)
![Pinia](https://img.shields.io/badge/pinia-FFD859?style=for-the-badge)
![Vue Router](https://img.shields.io/badge/vue%20router-4FC08D?style=for-the-badge)
![Vite](https://img.shields.io/badge/vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![Axios](https://img.shields.io/badge/axios-5A29E4?style=for-the-badge)
![TypeScript](https://img.shields.io/badge/typescript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Chart.js](https://img.shields.io/badge/chart.js-FF6384?style=for-the-badge&logo=chartdotjs&logoColor=white)
![HTML5](https://img.shields.io/badge/html5-E34F26?style=for-the-badge&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/css3-1572B6?style=for-the-badge&logo=css3&logoColor=white)

### Database
![MariaDB](https://img.shields.io/badge/mariadb-003545?style=for-the-badge&logo=mariadb&logoColor=white)
![Amazon RDS](https://img.shields.io/badge/amazon%20rds-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white)

### Cloud / Infrastructure
![AWS](https://img.shields.io/badge/aws-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)
![AWS IAM](https://img.shields.io/badge/aws%20iam-FF9900?style=for-the-badge)
![Amazon S3](https://img.shields.io/badge/amazon%20s3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)

### CI / CD
![GitHub Actions](https://img.shields.io/badge/github%20actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

### Tools
![Git](https://img.shields.io/badge/git-F05032?style=for-the-badge&logo=git&logoColor=white)
![GitHub](https://img.shields.io/badge/github-181717?style=for-the-badge&logo=github)
![Notion](https://img.shields.io/badge/notion-000000?style=for-the-badge&logo=notion)
![Discord](https://img.shields.io/badge/discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)
![Figma](https://img.shields.io/badge/figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white)

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 주요 기술

<details>
<summary><strong>동기/비동기 통신 설계 및 이벤트 기반 후속 처리</strong></summary>

MSA 환경에서 전자결재 서비스가 member·salary 서버와 통신하는 상황이 다수 발생했습니다. 결재 트랜잭션 내에서 즉시 필요한 데이터인지, 승인 이후 후속 처리인지에 따라 동기·비동기 방식을 구분하여 설계했습니다.

**동기 통신 (FeignClient)** — 결재 트랜잭션 내 즉시 결과가 필요한 경우에만 최소한으로 적용

- 결재라인 편집 화면에서 즉시 후보자 리스트를 확인하는 경우
- 기안 생성 트랜잭션 안에서 기안자 직책·조직 정보를 함께 검증·저장하는 경우
- 승인 트랜잭션 안에서 결재자 서명 이미지를 즉시 문서에 저장하는 경우

**비동기 통신 (Kafka)** — 결재 트랜잭션과 분리 가능한 후속 처리는 전부 비동기로 분리. 서비스 간 느슨한 결합으로 특정 서비스 장애가 결재 승인 자체에 영향을 주지 않도록 설계

| 요청 타입 | 발행 이벤트 | 후속 처리 |
|------|------|------|
| VACATION | LeaveApprovalEvent | 연차 잔고 차감 |
| ATTENDANCE | OvertimeApprovalEvent | 연장근무 정산 반영 |
| ATTENDANCE | AttendanceCorrectionEvent | 출퇴근 정정 반영 |
| ATTENDANCE | EarlyLeaveEvent | 조기퇴근 근태 반영 |
| HR_MOVEMENT | LeaveOfAbsenceEvent | 휴직·복직 상태 변경 |
| HR_MOVEMENT | ResignationEvent | 퇴직 프로세스 시작 |
| SALARY | AllowanceApprovalEvent | 수당 급여 정산 반영 |
| 공통 | ScheduleApprovalEvent | 근무일정 반영 |
| 공통 | CalendarApprovalEvent | 캘린더 일정 등록 |

검색 동기화는 Outbox 패턴으로 별도 관리 — 5초 간격 폴링 발행, 7일 이후 자동 정리

> 결재 트랜잭션 내에서 즉시 필요한 데이터인지 여부를 기준으로, 동기 호출은 최소화하고 나머지는 전부 비동기로 분리하는 방향으로 설계했습니다.

</details>

<details>
<summary><strong>회의 녹음 AI 요약</strong></summary>

인사관리 플랫폼에서 회의는 일상적으로 발생하는 업무입니다. 회의 내용을 기록하고, 이후 일정 및 후속 사항을 체계적으로 정리할 수 있도록 AI 기반 회의록 자동 생성 기능을 추가했습니다.

**처리 파이프라인**: 녹음 파일 업로드 → STT 텍스트 변환 → LLM 회의록 생성

- **음성 인식 (STT)**: OpenAI Whisper API(`whisper-1`)로 음성을 텍스트로 변환. webm, m4a, mp3, wav 등 지원, 최대 25MB
- **회의록 생성 (LLM)**: GPT-4o-mini(temperature 0.3)로 구조화된 회의록 자동 생성
  - 회의 요약 (한 줄)
  - 주요 결정사항
  - 액션 아이템 (담당자·기한 포함)
  - 주요 논의 내용
  - 이후 일정 및 후속 사항
- **재요약 기능**: 기존 텍스트를 다시 요약하는 별도 엔드포인트(`/ai/transcribe/summary`) 제공
- **성과 관리 연동**: 회의에서 도출된 액션 아이템을 목표·평가 시스템과 연결하여 추적 가능

**기술 구성**: FastAPI + OpenAI SDK, Eureka 서비스 등록으로 Spring 마이크로서비스와 통합

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

| 기능 | 설명 |
|------|------|
| 조직 관리 | 회사별 조직 구조, 직급(Job Grade), 직책(Job Title) 관리 |
| 역할·권한 | 13개 리소스 × 4개 액션 × 4단계 범위(본인/팀/부서/전사). 기본 역할 4개 자동 생성 + 커스텀 역할 추가 |
| 회사별 설정 | 공휴일, 휴가 종류, 근무 정책 등 회사별 독립 관리 |

</details>

<details>
<summary><strong>근태 · 급여 관리</strong></summary>

| 기능 | 설명 |
|------|------|
| 근태 관리 | 출퇴근 기록, 연장·야간·휴일근무 신청, 출퇴근 정정, 조기퇴근, 외근/출장 관리 |
| 휴가 관리 | 회사별 휴가 종류 설정, 연차 잔고 자동 차감(Pessimistic Locking), 연차 사용 촉진 자동 알림 |
| 급여 자동 연산 | 급여 계산, 상여금, 퇴직금, 세금, 소급 정산까지 자동 처리. 근태/급여/상여 배치 모듈로 대량 데이터 처리 |
| 노동법 준수 | 근기법 제61조 연차 촉진 1차/2차 자동 알림, 주 52시간 초과근로 위반 감지, 근태 이상 실시간 감지 |

</details>

<details>
<summary><strong>전자결재 · 계약</strong></summary>

| 기능 | 설명 |
|------|------|
| 전자결재 | 결재 요청(상신) → 단계별 승인/반려 → 후속 처리 자동화. 임시저장, 결재 회수, 대리결재, 참조/공람 지원 |
| 동적 결재 양식 | JSON 기반 formSchema로 회사가 직접 결재 양식을 만들고 필드를 자유롭게 구성. text, date, select, textarea 등 다양한 필드 타입 지원 |
| 결재 후속 처리 | 승인 완료 시 요청 타입별 자동 처리 — 휴가 잔고 차감, 근태 정산 반영, 인사 발령 이력 생성, 캘린더 연동 등 |
| 전자계약 | 계약서 생성 → 발송 → 서명 → PDF 저장. 일괄 발송, 법인인감 자동 서명, 계약 재발행 이력 관리 |

**전자계약 서명 프로세스**

계약서 생성부터 서명 완료까지의 전체 생명주기를 관리합니다.

**계약 상태 흐름**: CREATED → SENT → SIGNED / REJECTED / CANCELED

- **자동 정보 채움**: 발송 시 FeignClient로 member-service(인사 정보), salary-service(급여 정보) 조회하여 계약서 필드 자동 채움
- **법인인감 자동 서명**: 회사 측(COMPANY) 서명은 sealImageUrl로 자동 처리, 직원 서명만 대기
- **일괄 발송**: ContractBatch로 다수 직원에게 동일 계약서 일괄 생성·발송
- **재발행 이력 추적**: previousContractId, revision으로 재발행 이력 연결
- **서명 완료 PDF**: 양측 서명 완료 시 signedPdfUrl에 계약서 PDF 저장
- **계약 번호 자동 채번**: ContractNumberService로 순차 번호 생성

</details>

<details>
<summary><strong>목표 · 성과 관리</strong></summary>

| 기능 | 설명 |
|------|------|
| 목표 관리 | 목표 설정 → 활동 기록 → 진척률 관리 |
| 평가 관리 | 평가 시즌 관리, 평가 설계(Design), 다면 피드백, 부서 간 보정(Calibration) |
| 리포트 | 평가 결과 리포트 자동 생성 |

</details>

<details>
<summary><strong>ESG 활동 관리</strong></summary>

| 기능 | 설명 |
|------|------|
| ESG 활동 | 활동 신청/승인, 포인트 적립, 캠페인 관리 |
| 포인트 교환 | 적립 포인트로 물품 교환 쇼핑 |
| 참여 현황 | 인사 데이터 연동으로 부서별/직급별 ESG 참여 현황 분석 |

</details>

<details>
<summary><strong>협업 · 공통</strong></summary>

| 기능 | 설명 |
|------|------|
| 실시간 채팅 | WebSocket(STOMP) 기반 사내 메신저, 파일 공유 |
| 통합 검색 | Elasticsearch 기반으로 결재, 인사, 조직 등 흩어진 데이터를 한 번에 검색 |
| 알림 | Kafka → Redis Pub/Sub → SSE 파이프라인으로 결재·근태·계약 등 실시간 알림 |
| 캘린더 | 결재 승인 건 캘린더 자동 연동, 회사 공휴일 관리 |

</details>

<br>
<div align="right"><a href="#목차">맨 위로</a></div>

---

## 트러블슈팅

<!-- TODO: 주요 기술적 문제 → 원인 → 해결 사례 3개 정도 -->

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
