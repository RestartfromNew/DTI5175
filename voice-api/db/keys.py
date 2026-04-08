import json
import uuid
import hashlib
import secrets
from pathlib import Path
from datetime import datetime
from typing import Optional
from core.config import settings

DB_FILE = Path("db/keys.json")


def _load() -> dict:
    if DB_FILE.exists():
        return json.loads(DB_FILE.read_text())
    return {"keys": []}


def _save(data: dict):
    DB_FILE.parent.mkdir(parents=True, exist_ok=True)
    DB_FILE.write_text(json.dumps(data, indent=2, ensure_ascii=False))


def _hash_key(key: str) -> str:
    return hashlib.sha256(key.encode()).hexdigest()


class KeyDB:
    def __init__(self):
        self._data = _load()

    def is_valid(self, key: str) -> bool:
        h = _hash_key(key)
        return any(k["hash"] == h and k.get("active", True) for k in self._data["keys"])

    def list_keys(self) -> list[dict]:
        keys = []
        for k in self._data["keys"]:
            keys.append({
                "id": k["id"],
                "name": k["name"],
                "key_preview": k["key"][:8] + "..." + k["key"][-4:],
                "created": k["created"],
                "active": k.get("active", True),
            })
        return keys

    def add_key(self, name: str) -> tuple[str, str]:
        """Returns (id, full_key). Full key is only shown once."""
        key = "vk-" + secrets.token_urlsafe(32)
        entry = {
            "id": str(uuid.uuid4())[:8],
            "name": name,
            "key": key,
            "hash": _hash_key(key),
            "created": datetime.utcnow().isoformat(),
            "active": True,
        }
        self._data["keys"].append(entry)
        _save(self._data)
        return entry["id"], key

    def revoke_key(self, key_id: str) -> bool:
        for k in self._data["keys"]:
            if k["id"] == key_id:
                k["active"] = False
                _save(self._data)
                return True
        return False

    def delete_key(self, key_id: str) -> bool:
        self._data["keys"] = [k for k in self._data["keys"] if k["id"] != key_id]
        _save(self._data)
        return True


key_db = KeyDB()
