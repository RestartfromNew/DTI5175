from typing import Optional
from datetime import datetime
import json
from pathlib import Path
from core.config import settings
from core.firebase import fb

LOG_FILE = settings.log_dir / "operations.jsonl"
MAX_LINES = 500


class OpLogger:
    def __init__(self):
        settings.log_dir.mkdir(parents=True, exist_ok=True)

    def log(
        self,
        action: str,
        target: str,
        result: str,
        detail: Optional[str] = None,
        locale: str = "en",
    ):
        entry = {
            "timestamp": datetime.utcnow().isoformat(),
            "action": action,
            "target": target,
            "result": result,
            "detail": detail,
            "locale": locale,
        }
        
        # Log to local file
        try:
            lines = self._read_lines()
            lines.append(json.dumps(entry, ensure_ascii=False))
            if len(lines) > MAX_LINES:
                lines = lines[-MAX_LINES:]
            LOG_FILE.write_text("\n".join(lines) + "\n")
        except Exception as e:
            print(f"Local logging failed: {e}")

        # Log to Firebase
        try:
            if fb.db:
                fb.db.collection("operations").add(entry)
        except Exception as e:
            print(f"Firebase logging failed: {e}")

    def get_logs(self, limit: int = 50) -> list[dict]:
        # Try Firebase first for logs
        try:
            if fb.db:
                docs = fb.db.collection("operations").order_by("timestamp", direction="DESCENDING").limit(limit).stream()
                return [doc.to_dict() for doc in docs]
        except Exception as e:
            print(f"Failed to fetch logs from Firebase: {e}")

        # Fallback to local file
        lines = self._read_lines()
        parsed = []
        for line in lines[-limit:]:
            try:
                parsed.append(json.loads(line))
            except Exception:
                pass
        return parsed

    def clear_logs(self):
        if LOG_FILE.exists():
            LOG_FILE.unlink()
        # Note: Clearing Firebase logs is usually not done via simple one-liner for collections.
        # Leaving it for now or could implement batch delete.

    def _read_lines(self) -> list[str]:
        if not LOG_FILE.exists():
            return []
        content = LOG_FILE.read_text().strip()
        if not content:
            return []
        return content.split("\n")


op_logger = OpLogger()
