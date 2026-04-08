from fastapi import Request, HTTPException, Depends
from fastapi.security import APIKeyHeader
from typing import Optional
from core.config import settings
from db.keys import key_db

header_scheme = APIKeyHeader(name="X-API-Key", auto_error=False)


async def verify_api_key(key: Optional[str] = Depends(header_scheme)) -> str:
    if not key:
        raise HTTPException(status_code=401, detail="Missing API key")
    if key == settings.admin_api_key:
        return "admin"
    if key_db.is_valid(key):
        return "user"
    raise HTTPException(status_code=401, detail="Invalid API key")


async def require_admin(key: str = Depends(verify_api_key)) -> str:
    if key != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")
    return key
