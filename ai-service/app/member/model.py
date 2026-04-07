from sqlalchemy import Column, String, DateTime, Integer, Date
from sqlalchemy.dialects.mysql import CHAR
from datetime import datetime
from app.database import Base

class MemberCache(Base):
    __tablename__ = "member_cache"

    member_id = Column(CHAR(36), primary_key=True)
    company_id = Column(CHAR(36), nullable=False)
    name = Column(String(100), nullable=False)
    email = Column(String(255), nullable=True)
    sabun = Column(String(20), nullable=True)
    join_date = Column(Date, nullable=True)
    member_status = Column(String(20), nullable=True)
    organization_name = Column(String(100), nullable=True)
    job_grade_name = Column(String(100), nullable=True)
    job_title_name = Column(String(100), nullable=True)
    esg_score = Column(Integer, default=0, nullable=False)
    updated_at = Column(DateTime, default=datetime.now, onupdate=datetime.now)