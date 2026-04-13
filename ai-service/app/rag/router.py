from fastapi import APIRouter, Header, Depends
from app.rag import service
from app.rag.schema import RagSearchRequest, RagSearchResponse

router = APIRouter()

@router.post("/rag/search", response_model=RagSearchResponse)
async def rag_search(
        request: RagSearchRequest,
        x_user_company_id: str = Header(..., alias="X-User-CompanyId"),
        x_user_uuid: str = Header(..., alias="X-User-UUID")):
    return await service.rag_search(
        request.question,
        x_user_company_id)