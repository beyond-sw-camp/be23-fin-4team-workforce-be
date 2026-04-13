from fastapi import FastAPI
from app.document.router import router as document_router
from app.rag.router import router as rag_router
from app.database import Base, engine
from app.config import settings
import py_eureka_client.eureka_client as eureka_client

Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="AI Service",
    description="HR AI 서비스",
    version="1.0.0"
)

app.include_router(document_router, prefix="/ai")
app.include_router(rag_router, prefix="/ai")

@app.get("/health")
def health_check():
    return {"status": "ok"}

@app.on_event("startup")
async def startup_event():
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