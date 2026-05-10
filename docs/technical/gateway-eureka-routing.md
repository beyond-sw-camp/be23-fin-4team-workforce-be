# 서비스 디스커버리와 Gateway 라우팅

## 개요

백엔드는 Eureka Server와 Spring Cloud Gateway를 사용합니다. 각 도메인 서비스는 Eureka Client로 등록되고, Gateway는 외부 요청의 단일 진입점으로 인증/라우팅을 담당합니다.

## 구성

| 모듈 | 역할 |
|------|------|
| `eureka` | 서비스 디스커버리 서버 |
| `gateway` | JWT 검증, actor type 검증, CORS, 라우팅 |
| 도메인 서비스 | Eureka Client로 등록 후 Gateway 뒤에서 API 제공 |

## Gateway 책임

- CORS preflight 처리
- 공개 경로 통과
- JWT 검증
- SaaS 운영자와 일반 멤버 경로 분리
- 내부 서비스에 사용자/회사 컨텍스트 헤더 주입
- 서비스별 route로 요청 전달

## 공개 경로

대표적으로 로그인, 회사 온보딩, 비밀번호 재설정, STOMP 연결, 내부 정책 조회 API가 공개 경로에 포함되어 있습니다.  
공개 경로는 인증 필터를 우회하지만, 실제 운영에서는 Gateway 외부 노출 범위와 네트워크 정책을 함께 고려해야 합니다.

## Eureka 사용 이유

- 서비스 인스턴스 주소를 하드코딩하지 않고 동적으로 찾습니다.
- 로컬/운영 환경에서 서비스 수가 바뀌어도 Gateway 라우팅 구조를 유지할 수 있습니다.
- AI 서비스도 FastAPI에서 Eureka 등록 로직을 사용합니다.
