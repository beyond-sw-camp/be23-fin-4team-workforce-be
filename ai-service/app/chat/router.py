from fastapi import APIRouter, Header, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.chat import service
from app.chat.schema import ChatRequest, ChatResponse, ChatHistoryRes
from typing import List

router = APIRouter()

# 질문 전송
@router.post("/chat", response_model=ChatResponse)
def chat(
        request: ChatRequest,
        x_user_company_id: str = Header(...),
        x_user_id: str = Header(...),
        db: Session = Depends(get_db)):
    return service.chat(
            request.question,
            x_user_company_id,
            x_user_id,
            db)

# 대화 이력 조회
@router.get("/chat/history", response_model=List[ChatHistoryRes])
def get_chat_history(
        x_user_company_id: str = Header(...),
        x_user_id: str = Header(...),
        db: Session = Depends(get_db)):
    return service.get_chat_history(
            x_user_company_id,
            x_user_id,
            db)

# 대화 이력 삭제
@router.delete("/chat/history")
def delete_chat_history(
        x_user_company_id: str = Header(...),
        x_user_id: str = Header(...),
        db: Session = Depends(get_db)):
    service.delete_chat_history(
            x_user_company_id,
            x_user_id,
            db)
    return {"message": "대화 이력 삭제 성공"}
