# WORKFORCE - 기록하는 HR에서 예측하는 HR로, 스마트 HR 통합 플랫폼



<hr>

## 목차
#### [팀 소개](#팀-소개)
#### [프로젝트 소개](#프로젝트-소개)
#### [주요 기능](#주요-모듈-기능-요약)
#### [기획서](#기획서-business-requirements)
#### [기술 스택](#기술-스택)
#### [시스템 아키텍처](#시스템-아키텍처)
#### [WBS](#wbs)
#### [요구사항 명세서](#요구사항-명세서)
#### [DDD](#ddd)
#### [ERD](#erd)
#### [와이어 프레임](#와이어-프레임)
#### [API 명세서](#api-명세서)
#### [단위 테스트](#단위-테스트)
#### [UI / UX 테스트](#ui--ux-단위-테스트)
#### [CI / CD 계획서](#ci--cd-계획서)
#### [프로젝트 회고](#프로젝트-회고)

---

## 팀 소개

| [![김정훈](url1)](github1) | [![박세민](url2)](github2) | [![이다은](url3)](github3) | [![이지연](url4)](github4) |
|------------------------|------------------------|------------------------|------------------------|
| 김정훈 | 박세민 | 이다은 | 이지연 |

<hr>

## 프로젝트 소개
"기록하는 HR → 예측하는 HR" 비전의 **데이터 기반 All-in-One 스마트 HRMS**입니다. 중소기업 HR 담당자의 **'Second Brain'** 역할을 수행하며, 인사 데이터를 중앙화하고 AI로 선제적 인사이트를 제공합니다.

#### 핵심 변화
- **스킬 기반 조직**: 직무 → 스킬 벡터로 전환
- **AI 퇴사 예측**: 최대 **95% 정확도**로 핵심 인재 이탈 예측

<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>

## 주요 모듈 기능 요약

#### MEM 회원 & 로그인
- 사업자번호 API 검증
- 이메일 기반 가입/비번 정책
- 자동 로그아웃 & 관리자 생성

#### HRM 조직·사용자 관리
```
사용자 CRUD | 조직 트리 | 근태 자동 계산
성과 평가 | RBAC 권한 | 필드 레벨 보안
```

#### MPG 마이페이지
- 연차 카드 & 캘린더
- 정보 수정 & 공개 설정

#### RVS 예약 시스템
- 자원 관리 & 중복 차단
- 결재 연동 & 통계 대시보드

#### ATM 근태·휴가
- 출퇴근·신청 승인
- 일일/연차 배치 자동화

#### SAM 급여 관리
- 자동 계산(수당·퇴직금)
- 연말정산 & 연봉 조정

#### DWM 결재 관리
- 템플릿·결재선 자동화
- 대리결재 & 워터마크

#### ELS 통합 검색
- 직원·문서 실시간 검색
- 권한 기반 필터링

#### PRM 성과 관리
- 셀프/부서 평가
- 목표 설정·증적 제출

#### PSM 혜택 매칭 (Phase별)
```
Phase1: 수동 50건 + UI
Phase2: 공공API + 알림
Phase3: 크롤러+LLM
Phase4: 대시보드
```

#### HRI HR 인텔리전스 (Phase1 즉시)
```
퇴사 점수(95% 정확도) | 리포트 자동화
면담 AI | 온보딩 추적
리텐션 원클릭 액션
```

<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>

## 기획서 (Business Requirements)
[![기획서 PDF](./docs/WORKFORCE_프로젝트_기획서.pdf)](./docs/WORKFORCE_프로젝트_기획서.pdf)

<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>

## 기술 스택
### 🧩 Backend - Spring
![Java](https://img.shields.io/badge/java-007396?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/spring%20boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/spring%20security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/spring%20data%20jpa-6DB33F?style=for-the-badge)
![JWT](https://img.shields.io/badge/jwt-000000?style=for-the-badge)
![Lombok](https://img.shields.io/badge/lombok-BC4521?style=for-the-badge)
![Gradle](https://img.shields.io/badge/gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Swagger](https://img.shields.io/badge/swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=white)
![WebSocket](https://img.shields.io/badge/websocket-000000?style=for-the-badge&logo=socketdotio&logoColor=white)


### 🤖 Backend - Python
![Python](https://img.shields.io/badge/python-3670A0?style=for-the-badge&logo=python&logoColor=ffdd54)
![FastAPI](https://img.shields.io/badge/FastAPI-005571?style=for-the-badge&logo=fastapi)
![ChatGPT](https://img.shields.io/badge/chatGPT-74aa9c?style=for-the-badge&logo=openai&logoColor=white)
![LangChain](https://img.shields.io/badge/langchain-%231C3C3C.svg?style=for-the-badge&logo=langchain&logoColor=white)

### 🎨 Frontend
![Vue.js](https://img.shields.io/badge/vue.js-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)
![Pinia](https://img.shields.io/badge/pinia-FFD859?style=for-the-badge)
![Vue Router](https://img.shields.io/badge/vue%20router-4FC08D?style=for-the-badge)
![Vite](https://img.shields.io/badge/vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![Axios](https://img.shields.io/badge/axios-5A29E4?style=for-the-badge)
![TypeScript](https://img.shields.io/badge/typescript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Chart.js](https://img.shields.io/badge/chart.js-FF6384?style=for-the-badge&logo=chartdotjs&logoColor=white)
![HTML5](https://img.shields.io/badge/html5-E34F26?style=for-the-badge&logo=html5&logoColor=white)
![CSS3](https://img.shields.io/badge/css3-1572B6?style=for-the-badge&logo=css3&logoColor=white)

### 🗄️ Database
![MariaDB](https://img.shields.io/badge/mariadb-003545?style=for-the-badge&logo=mariadb&logoColor=white)
![Amazon RDS](https://img.shields.io/badge/amazon%20rds-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white)

### ☁️ Cloud / Infrastructure
![AWS](https://img.shields.io/badge/aws-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)
![AWS IAM](https://img.shields.io/badge/aws%20iam-FF9900?style=for-the-badge)
![Amazon S3](https://img.shields.io/badge/amazon%20s3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)


### 🔄 CI / CD
![GitHub Actions](https://img.shields.io/badge/github%20actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)


### 🛠 Tools
![Git](https://img.shields.io/badge/git-F05032?style=for-the-badge&logo=git&logoColor=white)
![GitHub](https://img.shields.io/badge/github-181717?style=for-the-badge&logo=github)
![Notion](https://img.shields.io/badge/notion-000000?style=for-the-badge&logo=notion)
![Discord](https://img.shields.io/badge/discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)
![Figma](https://img.shields.io/badge/figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white)


<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>


## 요구사항 명세서
<img width="845" height="630" alt="" src="" />

<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>

## DDD
<img width="1190" height="643" alt="" src=" />
<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>

## ERD
<img width="1302" height="743" alt="" src="" />
<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>
