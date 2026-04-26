"""수동 동기화 API (Kafka 없이 테스트용)"""
from fastapi import APIRouter, Depends, Path
from sqlalchemy.orm import Session
from app.database import get_db
from app.sync.leave_syncer import sync_leave_documents
from app.sync.attendance_syncer import sync_attendance_documents
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/admin/sync", tags=["sync"])


@router.post("/leave/{company_id}")
async def trigger_leave_sync(
        company_id: str = Path(..., description="회사 UUID"),
        db: Session = Depends(get_db)):
    """수동 트리거 - 휴가"""
    try:
        result = await sync_leave_documents(company_id, db)
        return {
            "companyId": company_id,
            "result": result,
            "message": "휴가 동기화 완료"
        }
    except Exception as e:
        logger.error(f"[sync_router] 휴가 동기화 실패: {e}")
        return {"companyId": company_id, "error": str(e)}


@router.post("/attendance/{company_id}")
async def trigger_attendance_sync(
        company_id: str = Path(..., description="회사 UUID"),
        db: Session = Depends(get_db)):
    """수동 트리거 - 근무/근태"""
    try:
        result = await sync_attendance_documents(company_id, db)
        return {
            "companyId": company_id,
            "result": result,
            "message": "근무/근태 동기화 완료"
        }
    except Exception as e:
        logger.error(f"[sync_router] 근무/근태 동기화 실패: {e}")
        return {"companyId": company_id, "error": str(e)}