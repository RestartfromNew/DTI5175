from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from core.auth import require_admin
from core.i18n import t
from db.keys import key_db

router = APIRouter(prefix="/admin", tags=["admin"])


class AddKeyRequest(BaseModel):
    name: str


class ApiResponse(BaseModel):
    success: bool
    data: dict | list | None = None
    message: str | None = None


@router.get("/keys", response_model=ApiResponse)
async def list_keys(_: str = Depends(require_admin)):
    keys = key_db.list_keys()
    return ApiResponse(success=True, data={"keys": keys}, message="OK")


@router.post("/keys", response_model=ApiResponse)
async def add_key(body: AddKeyRequest, _: str = Depends(require_admin)):
    key_id, full_key = key_db.add_key(body.name)
    return ApiResponse(
        success=True,
        data={"id": key_id, "key": full_key, "warning": "Store this key — it will not be shown again."},
        message="Key created",
    )


@router.delete("/keys/{key_id}", response_model=ApiResponse)
async def revoke_key(key_id: str, _: str = Depends(require_admin)):
    ok = key_db.revoke_key(key_id)
    if not ok:
        raise HTTPException(status_code=404, detail="Key not found")
    return ApiResponse(success=True, message="Key revoked")


@router.get("/users", response_model=ApiResponse)
async def list_users(_: str = Depends(require_admin)):
    from core.firebase import fb
    try:
        if fb.db:
            docs = fb.db.collection("users").stream()
            users = []
            for doc in docs:
                u = doc.to_dict()
                u["uid"] = doc.id
                users.append(u)
            return ApiResponse(success=True, data={"users": users})
        return ApiResponse(success=False, message="Firebase not initialized")
    except Exception as e:
        return ApiResponse(success=False, message=str(e))
