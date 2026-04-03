from pydantic import BaseModel
from datetime import datetime

class DocumentUploadRes(BaseModel):
    id: str
    company_id: str
    document_name: str
    created_at: datetime

    class Config:
        from_attributes = True

class DocumentListRes(BaseModel):
    id: str
    company_id: str
    document_name: str
    created_at: datetime

    class Config:
        from_attributes = True