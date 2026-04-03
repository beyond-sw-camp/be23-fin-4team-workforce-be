from sqlalchemy import Column, String, DateTime, Text
from sqlalchemy.dialects.mysql import CHAR
from datetime import datetime
from app.database import Base
import uuid

class HrDocument(Base):
    __tablename__ = "hr_document"

    id = Column(CHAR(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    company_id = Column(CHAR(36), nullable=False)
    document_name = Column(String(255), nullable=False)
    file_url = Column(String(500), nullable=True)
    content = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.now)
    updated_at = Column(DateTime, default=datetime.now, onupdate=datetime.now)