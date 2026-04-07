from pydantic import BaseModel
from datetime import datetime
from typing import List

class ChatRequest(BaseModel):
    question: str

class ChatResponse(BaseModel):
    answer: str
    sources: List[str] = []

class ChatHistoryRes(BaseModel):
    id: str
    question: str
    answer: str
    created_at: datetime

    class Config:
        from_attributes = True