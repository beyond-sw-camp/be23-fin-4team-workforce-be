from app.core.openai import get_embedding
from app.core.pinecone import search_vectors
import asyncio

async def rag_search(
        question: str,
        company_id: str) -> dict:

    # 1. 질문 임베딩
    query_vector = await asyncio.to_thread(get_embedding, question)

    # 2. Pinecone 유사 문서 검색 (top_k 3으로 최소화)
    matches = await asyncio.to_thread(
        search_vectors, query_vector, company_id, top_k=3)

    # 3. 유사도 필터링 (0.5 이하만 - 엄격하게)
    filtered_matches = [m for m in matches if m.score < 0.5]

    if not filtered_matches:
        return {
            "context": "",
            "sources": []
        }

    # 4. 중복 문서 제거
    seen = set()
    unique_matches = []
    for m in filtered_matches:
        doc_name = m.metadata['document_name']
        if doc_name not in seen:
            seen.add(doc_name)
            unique_matches.append(m)

    # 5. 컨텍스트 최소화 (500자로 청크 제한)
    context_parts = []
    for m in unique_matches:
        content = m.metadata['content']
        # 500자 초과 시 자르기
        if len(content) > 500:
            content = content[:500]
        context_parts.append(
            f"[{m.metadata['document_name']}]\n{content}")

    context = "\n\n".join(context_parts)

    # 6. 출처 추출
    sources = list(set([
        m.metadata['document_name'] for m in unique_matches
    ]))

    return {
        "context": context,
        "sources": sources
    }