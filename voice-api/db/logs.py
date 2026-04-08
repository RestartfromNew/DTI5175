import json
import os
from pathlib import Path
from datetime import datetime
from typing import Optional
from core.config import settings

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
        lines = self._read_lines()
        lines.append(json.dumps(entry, ensure_ascii=False))
        if len(lines) > MAX_LINES:
            lines = lines[-MAX_LINES:]
        settings.log_dir.mkdir(parents=True, exist_ok=True)
        LOG_FILE.write_text("\n".join(lines) + "\n")

    def get_logs(self, limit: int = 50) -> list[dict]:
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

    def _read_lines(self) -> list[str]:
        if not LOG_FILE.exists():
            return []
        content = LOG_FILE.read_text().strip()
        if not content:
            return []
        return content.split("\n")


op_logger = OpLogger()
