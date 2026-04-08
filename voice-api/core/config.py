import os
from pathlib import Path
from dotenv import load_dotenv
from pydantic_settings import BaseSettings

load_dotenv()

class Settings(BaseSettings):
    minimax_api_key: str = ""
    minimax_base_url: str = "https://api.minimax.io"
    admin_api_key: str = ""
    server_host: str = "0.0.0.0"
    server_port: int = 8000
    log_dir: Path = Path("logs")
    max_slots: int = 10

    class Config:
        env_file = ".env"
        extra = "ignore"

settings = Settings()
