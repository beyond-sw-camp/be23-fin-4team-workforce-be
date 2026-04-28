from fastapi import APIRouter, Header
from app.rag import service
from app.rag.schema import RagSearchRequest, RagSearchResponse

router = APIRouter(prefix="/rag", tags=["rag"])


@router.post("/search", response_model=RagSearchResponse)
async def search(
        request: RagSearchRequest,
        x_user_company_id: str = Header(..., alias="X-User-CompanyId")):
    result = await service.rag_search(request.question, x_user_company_id)
    return result