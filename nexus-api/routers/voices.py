from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from typing import Optional
from datetime import datetime
from core.auth import verify_api_key
from core.i18n import t
from core.config import settings
from services.minimax import minimax
from db.logs import op_logger

router = APIRouter(prefix="/voices", tags=["voices"])


class ApiResponse(BaseModel):
    success: bool
    data: Optional[dict | list] = None
    message: Optional[str] = None
    timestamp: str = datetime.utcnow().isoformat()


def ok(data=None, message="OK"):
    return ApiResponse(success=True, data=data, message=message)


def err(code: str, message: str):
    return ApiResponse(
        success=False,
        data={"code": code, "message": message},
        message=message,
    )


@router.get("", response_model=ApiResponse)
async def list_voices(locale: str = Query("en"), _auth: str = Depends(verify_api_key)):
    voices, error = await minimax.get_voices()
    if error:
        op_logger.log("list_voices", "minimax", "failed", error, locale)
        raise HTTPException(status_code=502, detail=err("MINIMAX_ERROR", error))
    count = len(voices) if voices else 0
    op_logger.log("list_voices", f"count={count}", "success", locale=locale)
    return ok({
        "voices": voices or [],
        "slot_used": count,
        "slot_max": settings.max_slots,
    }, t(locale, "op_list"))


@router.get("/{voice_id}", response_model=ApiResponse)
async def get_voice(voice_id: str, locale: str = Query("en"), _auth: str = Depends(verify_api_key)):
    voices, error = await minimax.get_voices()
    if error:
        raise HTTPException(status_code=502, detail=err("MINIMAX_ERROR", error))
    voice = next((v for v in (voices or []) if v.get("voice_id") == voice_id), None)
    if not voice:
        raise HTTPException(status_code=404, detail=err("VOICE_NOT_FOUND", t(locale, "err_voice_not_found")))
    return ok({"voice": voice})


@router.post("/{voice_id}/activate", response_model=ApiResponse)
async def activate_voice(voice_id: str, locale: str = Query("en"), _auth: str = Depends(verify_api_key)):
    success, error = await minimax.activate_voice(voice_id)
    if not success:
        op_logger.log("activate", voice_id, "failed", error, locale)
        raise HTTPException(status_code=502, detail=err("MINIMAX_ERROR", error or t(locale, "err_minimax")))
    op_logger.log("activate", voice_id, "success", locale=locale)
    return ok({"voice_id": voice_id}, t(locale, "op_activate"))


@router.delete("/{voice_id}", response_model=ApiResponse)
async def delete_voice(voice_id: str, locale: str = Query("en"), _auth: str = Depends(verify_api_key)):
    success, error = await minimax.delete_voice(voice_id)
    if not success:
        op_logger.log("delete", voice_id, "failed", error, locale)
        raise HTTPException(status_code=502, detail=err("MINIMAX_ERROR", error or t(locale, "err_minimax")))
    op_logger.log("delete", voice_id, "success", locale=locale)
    return ok({"voice_id": voice_id}, t(locale, "op_delete"))


class BatchDeleteRequest(BaseModel):
    voice_ids: list[str]
    confirm: bool = False


@router.post("/batch-delete", response_model=ApiResponse)
async def batch_delete_voices(body: BatchDeleteRequest, locale: str = Query("en"), _auth: str = Depends(verify_api_key)):
    if not body.confirm:
        raise HTTPException(status_code=400, detail=err("CONFIRM_REQUIRED", "Set confirm=true to proceed"))
    results = {"deleted": [], "failed": []}
    for vid in body.voice_ids:
        success, error = await minimax.delete_voice(vid)
        if success:
            results["deleted"].append(vid)
        else:
            results["failed"].append({"voice_id": vid, "error": error})
    op_logger.log(
        "batch_delete",
        f"count={len(body.voice_ids)}",
        f"success={len(results['deleted'])}, failed={len(results['failed'])}",
        locale=locale,
    )
    return ok(results, t(locale, "op_batch_delete"))
