from openai import OpenAI
from app.config import settings

client = OpenAI(api_key=settings.openai_api_key)

def get_embedding(text: str) -> list[float]:
    response = client.embeddings.create(
        model="text-embedding-3-small",
        input=text
    )
    return response.data[0].embedding