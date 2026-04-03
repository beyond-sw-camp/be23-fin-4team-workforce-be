from fastapi import FastAPI
from app.document.router import router as document_router
from app.chat.router import router as chat_router
from app.database import Base, engine

# 테이블 자동 생성
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="AI Service",
    description="HR AI 챗봇 서비스",
    version="1.0.0"
)

app.include_router(document_router, prefix="/ai")
app.include_router(chat_router, prefix="/ai")

@app.get("/health")
def health_check():
    return {"status": "ok"}