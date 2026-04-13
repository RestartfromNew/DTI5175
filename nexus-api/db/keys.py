import json
import uuid
import hashlib
import secrets
from pathlib import Path
from datetime import datetime
from typing import Optional
from core.config import settings
from core.firebase import fb

DB_FILE = Path("db/keys.json")


def _hash_key(key: str) -> str:
    return hashlib.sha256(key.encode()).hexdigest()


class KeyDB:
    def __init__(self):
        self._collection_name = "api_keys"

    @property
    def collection(self):
        if fb.db:
            return fb.db.collection(self._collection_name)
        return None

    def _load_local(self) -> dict:
        if DB_FILE.exists():
            try:
                return json.loads(DB_FILE.read_text())
            except Exception:
                pass
        return {"keys": []}

    def _save_local(self, data: dict):
        DB_FILE.parent.mkdir(parents=True, exist_ok=True)
        DB_FILE.write_text(json.dumps(data, indent=2, ensure_ascii=False))

    def is_valid(self, key: str) -> bool:
        h = _hash_key(key)
        
        # Try Firebase
        try:
            if self.collection:
                docs = self.collection.where("hash", "==", h).where("active", "==", True).limit(1).stream()
                if any(docs):
                    return True
        except Exception:
            pass

        # Fallback to local
        data = self._load_local()
        return any(k["hash"] == h and k.get("active", True) for k in data["keys"])

    def list_keys(self) -> list[dict]:
        # Try Firebase
        try:
            if self.collection:
                docs = self.collection.stream()
                keys = []
                for doc in docs:
                    k = doc.to_dict()
                    keys.append({
                        "id": k["id"],
                        "name": k["name"],
                        "key_preview": k["key"][:8] + "..." + k["key"][-4:],
                        "created": k["created"],
                        "active": k.get("active", True),
                    })
                return keys
        except Exception:
            pass

        # Fallback to local
        data = self._load_local()
        keys = []
        for k in data["keys"]:
            keys.append({
                "id": k["id"],
                "name": k["name"],
                "key_preview": k["key"][:8] + "..." + k["key"][-4:],
                "created": k["created"],
                "active": k.get("active", True),
            })
        return keys

    def add_key(self, name: str) -> tuple[str, str]:
        key = "vk-" + secrets.token_urlsafe(32)
        entry = {
            "id": str(uuid.uuid4())[:8],
            "name": name,
            "key": key,
            "hash": _hash_key(key),
            "created": datetime.utcnow().isoformat(),
            "active": True,
        }
        
        # Save to Local
        data = self._load_local()
        data["keys"].append(entry)
        self._save_local(data)

        # Save to Firebase
        try:
            if self.collection:
                self.collection.document(entry["id"]).set(entry)
        except Exception:
            pass
            
        return entry["id"], key

    def revoke_key(self, key_id: str) -> bool:
        # Local update
        data = self._load_local()
        found = False
        for k in data["keys"]:
            if k["id"] == key_id:
                k["active"] = False
                found = True
        if found:
            self._save_local(data)

        # Firebase update
        try:
            if self.collection:
                self.collection.document(key_id).update({"active": False})
                return True
        except Exception:
            pass
            
        return found

    def delete_key(self, key_id: str) -> bool:
        # Local
        data = self._load_local()
        data["keys"] = [k for k in data["keys"] if k["id"] != key_id]
        self._save_local(data)

        # Firebase
        try:
            if self.collection:
                self.collection.document(key_id).delete()
                return True
        except Exception:
            pass
            
        return True


key_db = KeyDB()
