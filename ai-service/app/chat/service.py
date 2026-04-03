from sqlalchemy.orm import Session
from app.chat.model import ChatHistory
from app.core.openai import get_embedding
from app.core.pinecone import search_vectors
from app.core.langchain import generate_answer
from app.chat.schema import ChatResponse

def chat(
        question: str,
        company_id: str,
        member_id: str,
        db: Session) -> ChatResponse:

    # 1. 질문 임베딩
    query_vector = get_embedding(question)

    # 2. Pinecone에서 유사 문서 검색
    matches = search_vectors(
            query_vector, company_id, top_k=5)

    # 3. 컨텍스트 생성
    if not matches:
        answer = "죄송합니다. 관련된 정책 문서를 찾을 수 없습니다. HR 담당자에게 문의해주세요."
        sources = []
    else:
        context = "\n\n".join([
            f"[{m.metadata['document_name']}]\n{m.metadata['content']}"
            for m in matches
        ])
        

        # 4. LangChain으로 답변 생성
        answer = generate_answer(context, question)

    # 5. 대화 이력 저장
    history = ChatHistory(
        company_id=company_id,
        member_id=member_id,
        question=question,
        answer=answer
    )
    db.add(history)
    db.commit()

    return ChatResponse(answer=answer)

def get_chat_history(
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

def delete_chat_history(
        company_id: str,
        member_id: str,
        db: Session):
    db.query(ChatHistory)\
        .filter(
            ChatHistory.company_id == company_id,
            ChatHistory.member_id == member_id)\
        .delete()
    db.commit()