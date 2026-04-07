from sqlalchemy.orm import Session
from app.chat.model import ChatHistory
from app.member.model import MemberCache
from app.core.openai import get_embedding
from app.core.pinecone import search_vectors
from app.core.langchain import generate_answer
from app.chat.schema import ChatResponse
import asyncio

async def chat(
        question: str,
        company_id: str,
        member_id: str,
        db: Session) -> ChatResponse:

    # 1. 직원 정보 조회
    member_info = db.query(MemberCache) \
        .filter(MemberCache.member_id == member_id) \
        .first()

    # 2. 질문 임베딩
    query_vector = await asyncio.to_thread(get_embedding, question)

    # 3. Pinecone 유사 문서 검색
    matches = await asyncio.to_thread(
        search_vectors, query_vector, company_id, top_k=5)

    # 4. 컨텍스트 생성
    context = ""
    if matches:
        context = "\n\n".join([
            f"[{m.metadata['document_name']}]\n{m.metadata['content']}"
            for m in matches
        ])

    # 5. 최근 대화 이력 조회
    recent_history = db.query(ChatHistory) \
        .filter(
        ChatHistory.company_id == company_id,
        ChatHistory.member_id == member_id) \
        .order_by(ChatHistory.created_at.desc()) \
        .limit(5) \
        .all()

    conversation_history = ""
    if recent_history:
        for h in reversed(recent_history):
            conversation_history += f"Q: {h.question}\nA: {h.answer}\n\n"

    # 6. 직원 컨텍스트 생성
    member_context = build_member_context(member_info)

    # 7. 문서 없어도 직원 정보 기반 질문은 LLM 호출
    answer = await asyncio.to_thread(
        generate_answer,
        context,
        question,
        conversation_history,
        member_context)

    # 8. 출처 문서명 추출
    sources = list(set([
        m.metadata['document_name'] for m in matches
    ])) if matches else []

    # 9. 대화 이력 저장
    history = ChatHistory(
        company_id=company_id,
        member_id=member_id,
        question=question,
        answer=answer
    )
    db.add(history)
    db.commit()

    return ChatResponse(answer=answer, sources=sources)


def build_member_context(member: MemberCache) -> str:
    if not member:
        return "직원 정보를 찾을 수 없습니다."

    context = f"""
[질문 직원 정보]
- 이름: {member.name}
- 사번: {member.sabun or "미등록"}
- 입사일: {member.join_date or "미등록"}
- 소속 조직: {member.organization_name or "미등록"}
- 직급: {member.job_grade_name or "미등록"}
- 직책: {member.job_title_name or "미등록"}
- 재직 상태: {member.member_status or "미등록"}
- ESG 점수: {member.esg_score}점
""".strip()

    return context


async def get_chat_history(
        company_id: str,
        member_id: str,
        db: Session) -> list[ChatHistory]:
    return db.query(ChatHistory)\
        .filter(
            ChatHistory.company_id == company_id,
            ChatHistory.member_id == member_id)\
        .order_by(ChatHistory.created_at.desc())\
        .limit(20)\
        .all()


async def delete_chat_history(
        company_id: str,
        member_id: str,
        db: Session):
    db.query(ChatHistory)\
        .filter(
            ChatHistory.company_id == company_id,
            ChatHistory.member_id == member_id)\
        .delete()
    db.commit()