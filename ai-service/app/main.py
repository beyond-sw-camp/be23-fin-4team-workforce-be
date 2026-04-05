from fastapi import FastAPI
from app.document.router import router as document_router
from app.chat.router import router as chat_router
from app.database import Base, engine
from app.config import settings
import py_eureka_client.eureka_client as eureka_client
import asyncio

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

@app.on_event("startup")
async def startup_event():
    await eureka_client.init_async(
        eureka_server=settings.eureka_server_url,
        app_name="ai-service",
        instance_port=8090,
        instance_host="localhost"
    )

@app.on_event("shutdown")
async def shutdown_event():
    await eureka_client.stop_async()