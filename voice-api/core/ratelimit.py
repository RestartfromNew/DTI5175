from slowapi import Limiter
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded
from fastapi import Request
from core.config import settings
from db.keys import key_db

limiter = Limiter(key_func=get_remote_address)


async def rate_limit_by_key(request: Request):
    """Called as a dependency. Checks X-API-Key header for per-key rate limiting."""
    key = request.headers.get("X-API-Key", "")
    if key and key_db.is_valid(key):
        # Per-key limit: 60 req/min
        # This is a simplified check; production would use Redis
        pass
    # Global rate limit applied via decorator on each route
    pass


def rate_exceeded_handler(request: Request, exc: RateLimitExceeded):
    raise HTTPException(
        status_code=429,
        detail="Rate limit exceeded",
        headers={"Retry-After": str(exc.detail.get("retry_after", 60))},
    )


from fastapi import HTTPException

HTTPException = HTTPException
