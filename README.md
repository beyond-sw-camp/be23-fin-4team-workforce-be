<div align="center">
<img width="845" alt="WORKFORCE" src="https://github.com/user-attachments/assets/a8d3b7b8-4880-491f-9073-0dad92340ea5" />
</div>

<hr>

## 목차
- [팀 소개 및 프로젝트 소개](#팀-소개-및-프로젝트-소개)
- [주요 기능](#주요-모듈-기능-요약)
- [기획서](#기획서)
- [기술 스택](#기술-스택)
- [시스템 아키텍처](#시스템-아키텍처)
- [요구사항 명세서](#요구사항-명세서)
- [WBS](#wbs)
- [ERD](#erd)
- [화면설계서](#화면설계서)
- [프로그램 사양서 및 단위테스트결과서](#프로그램-사양서-및-단위테스트결과서)
- [단위 테스트](#단위-테스트)
- [UI / UX 테스트](#ui--ux-단위-테스트)
- [CI / CD 계획서](#ci--cd-계획서)
- [프로젝트 회고](#프로젝트-회고)

---

## 팀 소개 및 프로젝트 소개

<div align="center">

| ![김정훈](url1) | ![박세민](url2) | ![이다은](url3) | ![이지연](url4) |
|----------------|-----------------|-----------------|-----------------|
| [김정훈](github1) | [박세민](github1) | [이다은](github1) | [이지연](github1) |

</div>

"기록하는 HR → 예측하는 HR" 비전의 **데이터 기반 All-in-One 스마트 HRMS**입니다. 중소기업 HR 담당자의 **'Second Brain'** 역할을 수행하며, 인사 데이터를 중앙화하고 AI로 선제적 인사이트를 제공합니다.

<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>

## 주요 모듈 기능 요약

<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>

## 기획서

- [Figma 기획서 보기](https://www.figma.com/proto/6suak3PIQpHGzS3UBatOF1/%ED%95%9C%ED%99%94-BEYOND-SW-CAMP-23%EA%B8%B0?node-id=618-102&viewport=696%2C533%2C0.2&t=o5woH0kckAYE5DgS-1&scaling=min-zoom&content-scaling=fixed&starting-point-node-id=618%3A102&page-id=612%3A67)
- [기획서 PDF 다운로드](docs/WORKFORCE_프로젝트_기획서.pdf)

<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>

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
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>

## 요구사항 명세서
- [요구사항 명세서 보기](https://docs.google.com/spreadsheets/d/107CuIWefCSjBbwO0mnKdQJzK5WCFDKYtw5Rs18liJw8/edit?gid=63147339#gid=63147339)

<img width="845" alt="WORKFORCE 요구사항 명세서" src="./docs/WORKFORCE_요구사항명세서.png" />

<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>

## WBS
- [WBS 보기](https://docs.google.com/spreadsheets/d/107CuIWefCSjBbwO0mnKdQJzK5WCFDKYtw5Rs18liJw8/edit?gid=395838842#gid=395838842)

<img width="845" alt="WORKFORCE WBS" src="./docs/WORKFORCE_WBS.png" />

<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>

## ERD

- [ERD 전체 보기](https://www.erdcloud.com/d/Er4amBY2KpweBcKTk)

<img width="1302" alt="WORKFORCE ERD" src="./docs/WORKFORCE_ERD.png" />

<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>

## 화면설계서

- [Figma 전체 보기](https://www.figma.com/design/FftNc7FU8cuQZS2VnKm3NR/be23-fin-team4-workforce?node-id=0-1&t=AZ9PaJjekutgn9fR-1)

<img width="1302" alt="WORKFORCE ERD" src="" />

<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>


## 프로그램 사양서 및 단위테스트결과서

- 🔗 [프로그램 사양서 및 단위테스트결과서](https://documenter.getpostman.com/view/51059789/2sBXqDrNZT)

<img width="1302" alt="WORKFORCE ERD" src="여기에_이미지_URL_넣기" />

<br>
<div align="right">
  <a href="#목차">🔝 맨 위로</a>
</div>
<hr>
