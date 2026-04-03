from pinecone import Pinecone
from app.config import settings

pc = Pinecone(api_key=settings.pinecone_api_key)
index = pc.Index(settings.pinecone_index_name)

def upsert_vectors(vectors: list[dict]):
    index.upsert(vectors=vectors)

def search_vectors(
        query_vector: list[float],
        company_id: str,
        top_k: int = 5) -> list[dict]:

    results = index.query(
        vector=query_vector,
        top_k=top_k,
        filter={"company_id": {"$eq": company_id}},
        include_metadata=True
    )
    return results.matches

def delete_vectors(document_id: str):
    index.delete(
        filter={"document_id": {"$eq": document_id}}
    )