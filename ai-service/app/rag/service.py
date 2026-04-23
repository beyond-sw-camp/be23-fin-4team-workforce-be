from app.core.openai import get_embedding, expand_query, detect_category
from app.core.pinecone import search_vectors
import asyncio
import logging

logger = logging.getLogger(__name__)


# 차단 마커 - context가 비어있을 때 사용
# n8n 프롬프트에서 이 마커를 감지하면 LLM 답변 대신 안내 문구만 반환
NO_CONTEXT_MARKER = "__NO_RELEVANT_DOCUMENT__"


async def rag_search(
        question: str,
        company_id: str) -> dict:
    """
    RAG 검색 다단계 파이프라인.

    단계:
      1. 카테고리 감지 (LLM)
      2. 질문 확장 (짧은 질문만)
      3. 임베딩 생성
      4. 카테고리 필터 적용 1차 검색
      5. 결과 없으면 전체 검색 (fallback)
      6. 중복 제거 + 컨텍스트 구성

    검색 실패 시 차단 마커 반환하여 LLM의 Hallucination 방지.
    """

    logger.info(f"[rag_search] ========================================")
    logger.info(f"[rag_search] Q: '{question}'")

    if not question or not question.strip():
        logger.info("[rag_search] 빈 질문, 차단 마커 반환")
        return {"context": NO_CONTEXT_MARKER, "sources": []}

    category = await asyncio.to_thread(detect_category, question)
    expanded_question = await asyncio.to_thread(expand_query, question)

    query_vector = await asyncio.to_thread(
        get_embedding, expanded_question)

    matches = []
    if category:
        logger.info(f"[rag_search] 카테고리 필터 검색: {category}")
        matches = await asyncio.to_thread(
            search_vectors,
            query_vector,
            company_id,
            top_k=5,
            min_score=0.35,
            category=category
        )

    if not matches:
        if category:
            logger.info(
                f"[rag_search] 카테고리({category}) 매칭 없음, 전체 검색 fallback"
            )
        else:
            logger.info(f"[rag_search] 카테고리 미감지, 전체 검색")

        matches = await asyncio.to_thread(
            search_vectors,
            query_vector,
            company_id,
            top_k=5,
            min_score=0.35,
            category=None
        )

    logger.info(f"[rag_search] 최종 매칭 {len(matches)}개")
    for i, m in enumerate(matches):
        md = m.metadata
        content_preview = md.get('content', '')[:60].replace('\n', ' ')
        logger.info(
            f"  #{i+1} score={m.score:.3f} "
            f"[{md.get('category', '?')}/{md.get('subcategory', '?')}] "
            f"'{content_preview}...'"
        )

    # 매칭 결과 없으면 차단 마커 반환
    if not matches:
        logger.info("[rag_search] 매칭 없음, 차단 마커 반환")
        return {"context": NO_CONTEXT_MARKER, "sources": []}

    # 중복 제거 + 컨텍스트 구성
    seen_content = set()
    context_parts = []
    sources = set()

    for m in matches:
        md = m.metadata
        content = md.get('content', '').strip()
        document_name = md.get('document_name', '문서')

        content_hash = hash(content)
        if content_hash in seen_content:
            continue
        seen_content.add(content_hash)

        if len(content) > 1200:
            content = content[:1200]

        context_parts.append(content)
        sources.add(document_name)

    context = "\n\n---\n\n".join(context_parts)

    logger.info(
        f"[rag_search] context {len(context)}자, sources={list(sources)}"
    )
    logger.info(f"[rag_search] ========================================")

    return {
        "context": context,
        "sources": list(sources)
    }