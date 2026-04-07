from fastapi import UploadFile, HTTPException
from sqlalchemy.orm import Session
from app.document.model import HrDocument
from app.core.openai import get_embedding
from app.core.pinecone import upsert_vectors, delete_vectors
from pypdf import PdfReader
from docx import Document
import uuid
import io
import os

# 파일 업로드
async def upload_document(
        file: UploadFile,
        company_id: str,
        db: Session) -> HrDocument:

    # 1. 파일 확장자 검증
    ALLOWED_EXTENSIONS = [".pdf", ".docx", ".txt"]
    ext = os.path.splitext(file.filename)[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail="지원하지 않는 파일 형식입니다. (pdf, docx, txt)")

    # 2. 파일 읽기
    content = await file.read()

    # 3. 파일 크기 검증 (10MB)
    MAX_FILE_SIZE = 10 * 1024 * 1024
    if len(content) > MAX_FILE_SIZE:
        raise HTTPException(
            status_code=400,
            detail="파일 크기는 10MB 이하여야 합니다.")

    # 4. 파일명 검증 (빈 파일명 방지)
    if not file.filename or file.filename.strip() == "":
        raise HTTPException(
            status_code=400,
            detail="파일명이 없습니다.")

    # 5. 텍스트 추출
    text = extract_text(file.filename, content)

    # 6. 텍스트 추출 결과 검증 (빈 문서 방지)
    if not text or text.strip() == "":
        raise HTTPException(
            status_code=400,
            detail="문서에서 텍스트를 추출할 수 없습니다.")

    # 7. DB 저장
    document = HrDocument(
        company_id=company_id,
        document_name=file.filename,
        content=text
    )
    db.add(document)
    db.commit()
    db.refresh(document)

    # 8. 청킹 + 임베딩 + Pinecone 저장
    try:
        await process_document(
            document.id, company_id,
            file.filename, text)
    except Exception as e:
        db.delete(document)
        db.commit()
        raise HTTPException(
            status_code=500,
            detail=f"문서 처리 실패: {str(e)}")

    return document

# 텍스트 추출
def extract_text(filename: str, content: bytes) -> str:
    if filename.endswith(".pdf"):
        reader = PdfReader(io.BytesIO(content))
        return "\n".join(
            page.extract_text() for page in reader.pages)
    elif filename.endswith(".docx"):
        doc = Document(io.BytesIO(content))
        return "\n".join(para.text for para in doc.paragraphs)
    elif filename.endswith(".txt"):
        return content.decode("utf-8")
    else:
        raise HTTPException(
            status_code=400,
            detail="지원하지 않는 파일 형식입니다. (pdf, docx, txt)")

# 청킹 + 임베딩 + Pinecone 저장
async def process_document(
        document_id: str,
        company_id: str,
        document_name: str,
        text: str):

    chunks = chunk_text(text, chunk_size=1000, overlap=200)

    vectors = []
    for i, chunk in enumerate(chunks):
        embedding = get_embedding(chunk)
        vectors.append({
            "id": f"{document_id}-{i}",
            "values": embedding,
            "metadata": {
                "document_id": document_id,
                "company_id": company_id,
                "document_name": document_name,
                "content": chunk,
                "chunk_index": i
            }
        })

    upsert_vectors(vectors)

# 청킹
def chunk_text(
        text: str,
        chunk_size: int = 1000,
        overlap: int = 200) -> list[str]:
    chunks = []
    start = 0
    while start < len(text):
        end = start + chunk_size
        chunks.append(text[start:end])
        start += chunk_size - overlap
    return chunks

# 문서 목록 조회
def get_documents(
        company_id: str,
        db: Session) -> list[HrDocument]:
    return db.query(HrDocument)\
        .filter(
            HrDocument.company_id == company_id,
            HrDocument.del_yn == "NO")\
        .all()

# 문서 삭제
def delete_document(
        document_id: str,
        company_id: str,
        db: Session):

    document = db.query(HrDocument)\
        .filter(
            HrDocument.id == document_id,
            HrDocument.company_id == company_id,
            HrDocument.del_yn == "NO")\
        .first()

    if not document:
        raise HTTPException(
            status_code=404,
            detail="문서를 찾을 수 없습니다.")

    # Pinecone 삭제 실패 시 DB 삭제 안 함
    try:
        delete_vectors(document_id)
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Pinecone 삭제 실패: {str(e)}")

    # 소프트 삭제
    document.del_yn = "YES"
    db.commit()