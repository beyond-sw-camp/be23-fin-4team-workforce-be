from fastapi import APIRouter, Header, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.chat import service
from app.chat.schema import ChatRequest, ChatResponse, ChatHistoryRes
from typing import List

router = APIRouter()

# 질문 전송
@router.post("/chat", response_model=ChatResponse)
async def chat(
        request: ChatRequest,
        x_user_company_id: str = Header(..., alias="X-User-CompanyId"),
        x_user_uuid: str = Header(..., alias="X-User-UUID"),
        db: Session = Depends(get_db)):
    return await service.chat(
            request.question,
            x_user_company_id,
            x_user_uuid,
            db)

# 대화 이력 조회
@router.get("/chat/history", response_model=List[ChatHistoryRes])
async def get_chat_history(
        x_user_company_id: str = Header(..., alias="X-User-CompanyId"),
        x_user_uuid: str = Header(..., alias="X-User-UUID"),
        db: Session = Depends(get_db)):
    return await service.get_chat_history(
            x_user_company_id,
            x_user_uuid,
            db)

# 대화 이력 삭제
@router.delete("/chat/history")
async def delete_chat_history(
        x_user_company_id: str = Header(..., alias="X-User-CompanyId"),
        x_user_uuid: str = Header(..., alias="X-User-UUID"),
        db: Session = Depends(get_db)):
    await service.delete_chat_history(
            x_user_company_id,
            x_user_uuid,
            db)
    return {"message": "대화 이력 삭제 성공"}