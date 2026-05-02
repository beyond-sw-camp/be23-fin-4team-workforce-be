from pydantic_settings import BaseSettings
import logging


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

    gateway_url: str = "http://localhost:8080"

    eureka_server_url: str

    class Config:
        env_file = ".env"


settings = Settings()


# RAG 디버깅을 위한 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(name)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)