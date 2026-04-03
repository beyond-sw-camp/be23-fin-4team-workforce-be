from sqlalchemy import Column, String, DateTime, Text
from sqlalchemy.dialects.mysql import CHAR
from datetime import datetime
from app.database import Base
import uuid

class ChatHistory(Base):
    __tablename__ = "chat_history"

    id = Column(CHAR(36), primary_key=True,
            default=lambda: str(uuid.uuid4()))
    company_id = Column(CHAR(36), nullable=False)
    member_id = Column(CHAR(36), nullable=False)
    question = Column(Text, nullable=False)
    answer = Column(Text, nullable=False)
    created_at = Column(DateTime, default=datetime.now)