from __future__ import annotations
from pinecone import Pinecone
from app.config import settings
import logging

logger = logging.getLogger(__name__)

pc = Pinecone(api_key=settings.pinecone_api_key)
index = pc.Index(settings.pinecone_index_name)


def upsert_vectors(vectors: list[dict]):
    index.upsert(vectors=vectors)


def search_vectors(
        query_vector: list[float],
        company_id: str,
        top_k: int = 5,
        min_score: float = 0.35,   # 0.5 → 0.35
        category: str | None = None) -> list[dict]:
    """
    Pinecone 벡터 검색.

    min_score 기준 (cosine similarity):
      - 0.5+ : 매우 관련
      - 0.35-0.5 : 관련 있음 (섹션 단위 청크의 일반적 범위)
      - 0.2-0.35 : 약한 관련

    카테고리 필터가 1차 정제 역할을 하므로 임계값을 0.35로 유지해도
    오답 위험이 적다.
    """
    filter_dict = {"company_id": {"$eq": company_id}}

    if category:
        filter_dict["category"] = {"$eq": category}

    logger.info(
        f"[search_vectors] top_k={top_k}, min_score={min_score}, "
        f"filter={filter_dict}"
    )

    results = index.query(
        vector=query_vector,
        top_k=top_k,
        filter=filter_dict,
        include_metadata=True
    )

    # 전체 결과 로깅 (튜닝 용도)
    logger.info(f"[search_vectors] 전체 결과 {len(results.matches)}개:")
    for i, m in enumerate(results.matches):
        md = m.metadata
        logger.info(
            f"  #{i+1} score={m.score:.3f} "
            f"[{md.get('category', '?')}/{md.get('subcategory', '?')}]"
        )

    # 임계값 필터
    filtered = [
        match for match in results.matches
        if match.score >= min_score
    ]

    logger.info(
        f"[search_vectors] 임계값({min_score}) 통과: {len(filtered)}개"
    )

    return filtered


def delete_vectors(document_id: str):
    index.delete(
        filter={"document_id": {"$eq": document_id}}
    )