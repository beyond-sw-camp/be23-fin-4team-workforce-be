from fastapi import FastAPI
from app.document.router import router as document_router
from app.chat.router import router as chat_router
from app.database import Base, engine
from app.config import settings
from app.member.consumer import start_consumer_thread
import py_eureka_client.eureka_client as eureka_client

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
    # Kafka consumer 먼저 시작 (Eureka 와 독립적으로)
    print("===== Kafka consumer 시작 시도 =====")
    start_consumer_thread()
    print("===== Kafka consumer 시작 완료 =====")

    # Eureka 등록 (실패해도 서비스 동작)
    try:
        await eureka_client.init_async(
            eureka_server=settings.eureka_server_url,
            app_name="ai-service",
            instance_port=8090,
            instance_host="localhost"
        )
    except Exception as e:
        print(f"Eureka 등록 실패 (무시): {e}")

@app.on_event("shutdown")
async def shutdown_event():
    try:
        await eureka_client.stop_async()
    except Exception as e:
        print(f"Eureka 종료 실패 (무시): {e}")