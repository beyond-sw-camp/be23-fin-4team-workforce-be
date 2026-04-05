from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    openai_api_key: str
    pinecone_api_key: str
    pinecone_index_name: str

    db_host: str
    db_port: int
    db_name: str
    db_user: str
    db_password: str

    redis_host: str
    redis_port: int

    kafka_bootstrap_servers: str

    eureka_server_url: str  # ← 추가

    class Config:
        env_file = ".env"

settings = Settings()