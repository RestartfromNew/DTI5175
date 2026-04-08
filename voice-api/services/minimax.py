import httpx
import ssl
from typing import Optional
from core.config import settings


class MiniMaxClient:
    def __init__(self):
        self.base_url = settings.minimax_base_url
        self.api_key = settings.minimax_api_key
        self._client = httpx.AsyncClient(timeout=30.0)

    async def _post(self, endpoint: str, payload: dict) -> Optional[dict]:
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }
        # Use unverified SSL context for dev; in production, use verified
        import ssl as ssl_module
        ssl_ctx = ssl_module._create_unverified_context()
        transport = httpx.AsyncHTTPTransport(verify=ssl_ctx)
        async with httpx.AsyncClient(transport=transport, timeout=30.0) as client:
            try:
                resp = await client.post(
                    f"{self.base_url}{endpoint}",
                    json=payload,
                    headers=headers,
                )
                return resp.json()
            except Exception:
                return None

    async def get_voices(self) -> tuple[Optional[list], Optional[str]]:
        """Returns (voices_list, error_message)"""
        res = await self._post("/v1/get_voice", {"voice_type": "voice_cloning"})
        if not res:
            return None, "Network error"
        code = res.get("base_resp", {}).get("status_code", -1)
        if code != 0:
            msg = res.get("base_resp", {}).get("status_msg", "Unknown error")
            return None, f"Code {code}: {msg}"
        # Try both 'voice_cloning' and 'voices' as the API keys can vary
        voices = res.get("voice_cloning") or res.get("voices") or []
        return voices, None

    async def activate_voice(self, voice_id: str) -> tuple[bool, Optional[str]]:
        """Returns (success, error_message)"""
        res = await self._post("/v1/t2a_v2", {
            "model": "speech-2.6-turbo",
            "text": "Hello",
            "voice_setting": {"voice_id": voice_id},
            "audio_setting": {"sample_rate": 32000, "bitrate": 128000, "format": "mp3", "channel": 1},
        })
        if not res:
            return False, "Network error"
        code = res.get("base_resp", {}).get("status_code", -1)
        if code == 0:
            return True, None
        msg = res.get("base_resp", {}).get("status_msg", "Unknown error")
        return False, f"Code {code}: {msg}"

    async def delete_voice(self, voice_id: str) -> tuple[bool, Optional[str]]:
        """Returns (success, error_message)"""
        res = await self._post("/v1/delete_voice", {
            "voice_type": "voice_cloning",
            "voice_id": voice_id,
        })
        if not res:
            return False, "Network error"
        code = res.get("base_resp", {}).get("status_code", -1)
        if code == 0:
            return True, None
        msg = res.get("base_resp", {}).get("status_msg", "Unknown error")
        return False, f"Code {code}: {msg}"

    async def close(self):
        await self._client.aclose()


minimax = MiniMaxClient()
