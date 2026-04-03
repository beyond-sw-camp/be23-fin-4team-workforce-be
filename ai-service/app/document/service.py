from fastapi import UploadFile, HTTPException
from sqlalchemy.orm import Session
from app.document.model import HrDocument
from app.core.openai import get_embedding
from app.core.pinecone import upsert_vectors, delete_vectors
from pypdf import PdfReader
from docx import Document
import uuid
import io

# 문서 업로드
async def upload_document(
        file: UploadFile,
        company_id: str,
        db: Session) -> HrDocument:

    # 1. 파일 읽기
    content = await file.read()

    # 2. 텍스트 추출
    text = extract_text(file.filename, content)

    # 3. DB 저장
    document = HrDocument(
        company_id=company_id,
        document_name=file.filename,
        content=text
    )
    db.add(document)
    db.commit()
    db.refresh(document)

    # 4. 청킹 + 임베딩 + Pinecone 저장
    await process_document(document.id, company_id,
            file.filename, text)

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

    # 1. 청킹 (1000자 단위, 200자 overlap)
    chunks = chunk_text(text, chunk_size=1000, overlap=200)

    # 2. 각 청크 임베딩 + Pinecone 저장
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
        .filter(HrDocument.company_id == company_id)\
        .all()

# 문서 삭제
def delete_document(
        document_id: str,
        company_id: str,
        db: Session):

    document = db.query(HrDocument)\
        .filter(HrDocument.id == document_id,
                HrDocument.company_id == company_id)\
        .first()

    if not document:
        raise HTTPException(
            status_code=404,
            detail="문서를 찾을 수 없습니다.")

    # Pinecone에서 삭제
    delete_vectors(document_id)

    # DB에서 삭제
    db.delete(document)
    db.commit()