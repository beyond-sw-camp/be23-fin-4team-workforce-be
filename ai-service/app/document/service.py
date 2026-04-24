from fastapi import UploadFile, HTTPException
from sqlalchemy.orm import Session
from app.document.model import HrDocument
from app.core.openai import get_embedding
from app.core.pinecone import upsert_vectors, delete_vectors
from langchain_text_splitters import RecursiveCharacterTextSplitter
from pypdf import PdfReader
from docx import Document
import uuid
import io
import os
import re
import logging

logger = logging.getLogger(__name__)


# ============================================================
# 파일 업로드 메인 함수
# ============================================================
async def upload_document(
        file: UploadFile,
        company_id: str,
        db: Session) -> HrDocument:

    ALLOWED_EXTENSIONS = [".pdf", ".docx", ".txt", ".md"]
    ext = os.path.splitext(file.filename)[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail="지원하지 않는 파일 형식입니다. (pdf, docx, txt, md)")

    content = await file.read()

    MAX_FILE_SIZE = 10 * 1024 * 1024
    if len(content) > MAX_FILE_SIZE:
        raise HTTPException(
            status_code=400,
            detail="파일 크기는 10MB 이하여야 합니다.")

    if not file.filename or file.filename.strip() == "":
        raise HTTPException(
            status_code=400,
            detail="파일명이 없습니다.")

    text = extract_text(file.filename, content)

    if not text or text.strip() == "":
        raise HTTPException(
            status_code=400,
            detail="문서에서 텍스트를 추출할 수 없습니다.")

    # DB 저장
    document = HrDocument(
        company_id=company_id,
        document_name=file.filename,
        content=text
    )
    db.add(document)
    db.commit()
    db.refresh(document)

    # 청킹 + 임베딩 + Pinecone 저장
    try:
        await process_document(
            document.id, company_id,
            file.filename, text)
    except Exception as e:
        logger.error(f"[upload_document] 처리 실패: {e}")
        db.delete(document)
        db.commit()
        raise HTTPException(
            status_code=500,
            detail=f"문서 처리 실패: {str(e)}")

    return document


def extract_text(filename: str, content: bytes) -> str:
    if filename.endswith(".pdf"):
        reader = PdfReader(io.BytesIO(content))
        return "\n".join(
            page.extract_text() for page in reader.pages)
    elif filename.endswith(".docx"):
        doc = Document(io.BytesIO(content))
        return "\n".join(para.text for para in doc.paragraphs)
    elif filename.endswith(".txt") or filename.endswith(".md"):
        return content.decode("utf-8")
    else:
        raise HTTPException(
            status_code=400,
            detail="지원하지 않는 파일 형식입니다. (pdf, docx, txt, md)")


# ============================================================
# 문서 구조 파싱 (섹션 단위)
# ============================================================
def parse_document_structure(text: str) -> list[dict]:
    """
    문서를 [카테고리] 주제 블록 단위로 파싱.

    지원 형식:
      [휴가] 결혼 휴가
      ■ 설명
      ...

      [급여] 급여 지급일
      ■ 설명
      ...

    Returns:
        [
          {
            "category": "휴가",
            "subcategory": "결혼 휴가",
            "full_content": "[휴가] 결혼 휴가\n\n■ 설명\n...",
            "keywords": ["결혼", "휴가", "5일", ...]
          },
          ...
        ]
    """
    sections = []

    # [카테고리] 주제 패턴 매칭
    pattern = r'\[([^\]]+)\]\s+([^\n]+)(.*?)(?=\n\s*\[[^\]]+\]\s+|\Z)'

    matches = list(re.finditer(pattern, text, re.DOTALL))

    if not matches:
        # 구조가 없는 문서는 통째로 하나의 섹션으로 처리
        logger.info("[parse_document_structure] 구조 없음, 전체를 단일 섹션으로 처리")
        return [{
            "category": "일반",
            "subcategory": "문서 전체",
            "full_content": text.strip(),
            "keywords": extract_keywords(text)
        }]

    for match in matches:
        category = match.group(1).strip()
        subcategory = match.group(2).strip()
        body = match.group(3).strip()

        full_content = f"[{category}] {subcategory}\n\n{body}"

        sections.append({
            "category": category,
            "subcategory": subcategory,
            "full_content": full_content,
            "keywords": extract_keywords(full_content)
        })

    logger.info(
        f"[parse_document_structure] {len(sections)}개 섹션 추출: "
        f"{[(s['category'], s['subcategory']) for s in sections]}"
    )
    return sections


def extract_keywords(text: str) -> list[str]:
    """
    텍스트에서 핵심 키워드 추출.
    ■ 관련 키워드 섹션이 있으면 거기서 추출, 없으면 간단 추출.
    """
    # ■ 관련 키워드 섹션에서 추출
    keyword_match = re.search(
        r'■\s*관련\s*키워드\s*\n([^\n■]+)',
        text
    )
    if keyword_match:
        keywords_str = keyword_match.group(1).strip()
        keywords = [
            k.strip()
            for k in re.split(r'[,、\s]+', keywords_str)
            if k.strip() and len(k.strip()) > 1
        ]
        return keywords[:15]

    # 키워드 섹션 없으면 빈 리스트
    return []


# ============================================================
# 청킹: 섹션 기반 + 길이 초과 시 분할
# ============================================================
def chunk_section(section: dict, max_chunk_size: int = 1500) -> list[str]:
    """
    섹션을 청크로 변환.
    - 1500자 이하면 통째로 하나의 청크
    - 초과 시 RecursiveCharacterTextSplitter로 분할
    """
    content = section["full_content"]

    if len(content) <= max_chunk_size:
        return [content]

    # 섹션이 너무 크면 분할
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=800,
        chunk_overlap=150,
        separators=[
            "\n\n■",
            "\n\n",
            "\n",
            "。",
            ". ",
            " ",
            ""
        ],
        length_function=len,
    )
    return splitter.split_text(content)


# ============================================================
# 텍스트 전처리
# ============================================================
def preprocess_text(text: str) -> str:
    """
    목록 항목에 섹션 맥락 추가.
    - 단, 이번 버전에서는 섹션 청킹이 핵심이라 간소화된 전처리만 수행
    """
    # 연속 빈 줄 정리
    result = re.sub(r'\n{3,}', '\n\n', text)
    return result


# ============================================================
# 문서 처리: 파싱 → 청킹 → 임베딩 → Pinecone 저장
# ============================================================
async def process_document(
        document_id: str,
        company_id: str,
        document_name: str,
        text: str):

    logger.info(f"[process_document] 시작: {document_name}, {len(text)}자")

    # 1. 전처리
    preprocessed_text = preprocess_text(text)

    # 2. 섹션 단위 파싱
    sections = parse_document_structure(preprocessed_text)

    if not sections:
        raise ValueError("문서에서 섹션을 추출하지 못했습니다")

    # 3. 각 섹션을 청킹하고 메타데이터 자동 태깅
    vectors = []
    global_chunk_index = 0

    for section_idx, section in enumerate(sections):
        chunks = chunk_section(section, max_chunk_size=1500)

        for chunk_idx, chunk_text_content in enumerate(chunks):
            if len(chunk_text_content.strip()) < 20:
                continue

            embedding = get_embedding(chunk_text_content)

            vectors.append({
                "id": f"{document_id}-{global_chunk_index}",
                "values": embedding,
                "metadata": {
                    "document_id": str(document_id),
                    "company_id": str(company_id),
                    "document_name": document_name,
                    "content": chunk_text_content,
                    "category": section["category"],
                    "subcategory": section["subcategory"],
                    "keywords": section["keywords"],
                    "chunk_index": global_chunk_index,
                    "section_index": section_idx,
                    "chunk_in_section": chunk_idx,
                }
            })
            global_chunk_index += 1

    if not vectors:
        raise ValueError("유효한 청크가 생성되지 않았습니다")

    logger.info(
        f"[process_document] 총 {len(vectors)}개 청크 생성, Pinecone 저장 중"
    )

    upsert_vectors(vectors)

    logger.info(f"[process_document] 완료: {document_name}")


# ============================================================
# 문서 조회/삭제
# ============================================================
def get_documents(
        company_id: str,
        db: Session) -> list[HrDocument]:
    return db.query(HrDocument) \
        .filter(
        HrDocument.company_id == company_id,
        HrDocument.del_yn == "NO") \
        .all()


def delete_document(
        document_id: str,
        company_id: str,
        db: Session):

    document = db.query(HrDocument) \
        .filter(
        HrDocument.id == document_id,
        HrDocument.company_id == company_id,
        HrDocument.del_yn == "NO") \
        .first()

    if not document:
        raise HTTPException(
            status_code=404,
            detail="문서를 찾을 수 없습니다.")

    try:
        delete_vectors(document_id)
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Pinecone 삭제 실패: {str(e)}")

    document.del_yn = "YES"
    db.commit()