from fastapi import APIRouter, UploadFile, File, Header, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.document import service
from app.document.schema import DocumentUploadRes, DocumentListRes
from typing import List

router = APIRouter()

# 문서 업로드
@router.post("/documents/upload", response_model=DocumentUploadRes)
async def upload_document(
        file: UploadFile = File(...),
        x_user_company_id: str = Header(...),
        db: Session = Depends(get_db)):
    return await service.upload_document(
            file, x_user_company_id, db)

# 문서 목록 조회
@router.get("/documents", response_model=List[DocumentListRes])
def get_documents(
        x_user_company_id: str = Header(...),
        db: Session = Depends(get_db)):
    return service.get_documents(x_user_company_id, db)

# 문서 삭제
@router.delete("/documents/{document_id}")
def delete_document(
        document_id: str,
        x_user_company_id: str = Header(...),
        db: Session = Depends(get_db)):
    service.delete_document(document_id, x_user_company_id, db)
    return {"message": "문서 삭제 성공"}