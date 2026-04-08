from fastapi import APIRouter, Query
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
from datetime import datetime
from pathlib import Path
from core.auth import verify_api_key
from db.logs import op_logger
from services.minimax import minimax

router = APIRouter(tags=["health"])


class ApiResponse(BaseModel):
    success: bool
    data: dict | None = None
    message: str | None = None
    timestamp: str = datetime.utcnow().isoformat()


@router.get("/health", response_model=ApiResponse)
async def health():
    """Health check — no auth required."""
    voices, error = await minimax.get_voices()
    api_ok = error is None
    return ApiResponse(
        success=True,
        data={
            "status": "ok" if api_ok else "degraded",
            "minimax_connected": api_ok,
            "voice_count": len(voices) if voices is not None else 0,
        },
    )


@router.get("/logs", response_model=ApiResponse)
async def get_logs(limit: int = Query(50, ge=1, le=200), _auth: str = Query(default="", alias="X-API-Key")):
    logs = op_logger.get_logs(limit)
    return ApiResponse(success=True, data={"logs": logs})


@router.delete("/logs", response_model=ApiResponse)
async def clear_logs(_auth: str = Query(alias="X-API-Key")):
    op_logger.clear_logs()
    return ApiResponse(success=True, message="Logs cleared")


@router.get("/dashboard", response_class=HTMLResponse)
async def dashboard():
    """Serves the dashboard HTML. No auth required (API calls use server-side key)."""
    html_path = Path(__file__).parent.parent / "templates" / "dashboard.html"
    if html_path.exists():
        return HTMLResponse(content=html_path.read_text())
    return HTMLResponse(content="<h1>Dashboard not found</h1>", status_code=404)
