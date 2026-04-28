from __future__ import annotations
from openai import OpenAI
from app.config import settings
import logging

logger = logging.getLogger(__name__)

client = OpenAI(api_key=settings.openai_api_key)


def get_embedding(text: str) -> list[float]:
    """텍스트를 임베딩 벡터로 변환"""
    response = client.embeddings.create(
        model="text-embedding-3-small",
        input=text
    )
    return response.data[0].embedding


def expand_query(question: str) -> str:
    """
    사용자 질문을 RAG 검색에 유리하도록 관련 키워드로 확장.

    핵심 규칙:
      - 직접적인 동의어/유사 표현만 추가
      - 상위 개념이나 카테고리명은 절대 추가 금지
        (예: "결혼 휴가" → "특별휴가", "경조사" 추가하면 안 됨)
      - 긴 질문(20자 초과)은 이미 충분한 맥락이 있으므로 확장하지 않음
    """
    # 짧은 질문만 확장
    if len(question) > 20:
        logger.info(f"[expand_query] 긴 질문 원본 유지: '{question}'")
        return question

    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {
                    "role": "system",
                    "content": (
                        "당신은 HR 챗봇의 검색 최적화 어시스턴트입니다. "
                        "사용자 질문에 직접 관련된 동의어만 추가하세요.\n\n"
                        "[규칙]\n"
                        "1. 원본 질문의 핵심 키워드를 그대로 유지하세요.\n"
                        "2. 동일한 의미의 동의어만 추가하세요.\n"
                        "3. 상위 개념이나 카테고리명은 절대 추가하지 마세요.\n"
                        "   - '결혼 휴가'에 '특별휴가', '경조사', '경조휴가' 추가 금지\n"
                        "   - '연차'에 '휴가', '특별휴가' 추가 금지\n"
                        "4. 다른 유사 항목을 연상시키는 단어를 추가하지 마세요.\n"
                        "   - '결혼'에 '출산', '사망' 추가 금지\n"
                        "5. 최대 5개 키워드 이내로 제한하세요.\n"
                        "6. 공백으로 구분된 키워드 목록만 반환하세요.\n\n"
                        "[좋은 예시]\n"
                        "질문: 결혼하면 휴가 얼마?\n"
                        "답변: 결혼 혼인 웨딩 일수\n\n"
                        "질문: 연차 이월 가능?\n"
                        "답변: 연차 이월 다음해 잔여\n\n"
                        "질문: 배우자 출산 휴가?\n"
                        "답변: 배우자 출산 남편 아내\n\n"
                        "[나쁜 예시 - 절대 하지 마세요]\n"
                        "질문: 결혼 휴가 며칠?\n"
                        "잘못된 답변: 결혼 휴가 특별휴가 경조사 경조휴가\n"
                        "(다른 휴가 종류까지 들어가서 검색 정확도가 떨어짐)"
                    )
                },
                {"role": "user", "content": f"질문: {question}\n답변:"}
            ],
            temperature=0.1,
            max_tokens=80
        )
        expanded = response.choices[0].message.content.strip()

        if not expanded:
            return question

        logger.info(f"[expand_query] '{question}' → '{expanded}'")
        return expanded

    except Exception as e:
        logger.error(f"[expand_query] 실패: {e}, 원본 반환")
        return question


def detect_category(question: str) -> str | None:
    """
    사용자 질문에서 HR 카테고리를 감지.

    카테고리 종류:
      - 휴가: 연차, 경조, 병가, 출산, 공가, 예비군 등
      - 급여: 월급, 지급일, 수당, 공제, 세금 등
      - 근무: 출퇴근, 야근, 유연근무, 재택, 주52시간 등
      - 결재: 결재양식, 문서번호, 승인, 반려 등
      - 목표: 목표 설정, KR, KPI 등
      - 평가: 평가 시즌, 자기평가, 동료평가 등
      - 기타: 위에 해당하지 않는 경우

    카테고리를 명확히 판단할 수 없으면 None 반환하여 전체 검색 수행.
    """
    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {
                    "role": "system",
                    "content": (
                        "당신은 HR 챗봇의 카테고리 분류 어시스턴트입니다. "
                        "사용자 질문을 아래 7가지 카테고리 중 하나로 분류하세요.\n\n"
                        "[카테고리]\n"
                        "- 휴가: 연차, 반차, 결혼, 출산, 경조, 병가, 예비군, 민방위, 공가, 특별휴가\n"
                        "- 급여: 월급, 기본급, 수당, 공제, 세금, 지급일, 상여금, 4대보험\n"
                        "- 근무: 출퇴근, 지각, 야근, 연장근로, 유연근무, 재택, 주52시간, 스케줄\n"
                        "- 결재: 결재양식, 문서번호, 결재라인, 승인, 반려, 전결, 대결\n"
                        "- 목표: 목표 설정, OKR, KPI, KR, 진행률\n"
                        "- 평가: 평가 시즌, 자기평가, 동료평가, 상향평가, 하향평가, 등급, 캘리브레이션\n"
                        "- 기타: 위에 해당하지 않거나 명확히 판단 불가\n\n"
                        "[규칙]\n"
                        "1. 카테고리 이름 하나만 정확히 반환하세요.\n"
                        "2. 다른 설명이나 이유를 추가하지 마세요.\n"
                        "3. 명확히 판단 불가능하면 '기타'를 반환하세요.\n\n"
                        "[예시]\n"
                        "질문: 결혼 휴가 며칠? → 휴가\n"
                        "질문: 월급 언제 들어와? → 급여\n"
                        "질문: 야근 신청 어떻게? → 근무\n"
                        "질문: 안녕 → 기타"
                    )
                },
                {"role": "user", "content": question}
            ],
            temperature=0.0,
            max_tokens=10
        )
        category = response.choices[0].message.content.strip()

        valid_categories = [
            "휴가", "급여", "근무", "결재", "목표", "평가"
        ]
        if category in valid_categories:
            logger.info(f"[detect_category] '{question}' → {category}")
            return category

        logger.info(f"[detect_category] '{question}' → 기타 (필터 없음)")
        return None

    except Exception as e:
        logger.error(f"[detect_category] 실패: {e}")
        return None