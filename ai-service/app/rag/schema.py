from pydantic import BaseModel
from typing import List

class RagSearchRequest(BaseModel):
    question: str

class RagSearchResponse(BaseModel):
    context: str
    sources: List[str] = []