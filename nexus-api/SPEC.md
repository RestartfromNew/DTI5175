# Nexus Backend — SPEC.md

## 1. Concept & Vision

A self-hosted FastAPI service for managing MiniMax voice cloning slots. Clean, utilitarian UI with excellent readability — prioritize showing all data clearly (IDs, status, timestamps) over visual polish. Multi-language support (EN/中文/FR). API protected with API key authentication.

**Personality:** A professional internal tool. Dense information, zero ambiguity, fast to navigate.

---

## 2. Technical Stack

- **Framework:** FastAPI (Python 3.11+)
- **UI:** Swagger UI (built-in FastAPI docs) + simple HTML dashboard
- **i18n:** Custom lightweight translation module (no heavy deps)
- **Auth:** API key in `X-API-Key` header
- **Config:** `.env` file (no hardcoded secrets)
- **HTTP Client:** `httpx` (async) for MiniMax API calls

---

## 3. Functionality Specification

### 3.1 Voice Management

| Endpoint | Method | Description |
|---|---|---|
| `GET /voices` | List all cloned voices with full metadata |
| `POST /voices/{voice_id}/activate` | Trigger synthesis to activate a voice slot |
| `DELETE /voices/{voice_id}` | Delete a cloned voice and free the slot |
| `GET /voices/{voice_id}` | Get details of a single voice |
| `POST /voices/batch-delete` | Delete multiple voices at once |

### 3.2 API Key Management

| Endpoint | Method | Description |
|---|---|---|
| `GET /admin/keys` | List all API keys (masked) |
| `POST /admin/keys` | Add a new API key |
| `DELETE /admin/keys/{key_id}` | Revoke an API key |

### 3.3 Dashboard (HTML)

A simple but information-dense HTML page at `/dashboard`:

- Language switcher (EN / 中文 / FR) in header
- Voice list table: Name, Voice ID, Status, Created At, Actions
- "Refresh" button to re-fetch from MiniMax API
- "Activate All Slots" button (batch activate up to 10)
- "Delete Selected" checkbox bulk delete
- API status indicator (connected / error)
- Request log panel (last 50 operations with timestamp, action, result)

### 3.4 Multi-Language

Supported locales: `en`, `zh`, `fr`

Translation scope:
- All UI labels, buttons, headers
- API error messages
- Dashboard status messages
- Default voice name templates

### 3.5 Authentication & Security

- All endpoints except `/docs`, `/dashboard`, `/health` require `X-API-Key` header
- API keys stored hashed in local DB (`json` file or `sqlite`)
- Rate limiting: 60 req/min per API key
- Admin endpoints (`/admin/*`) require a separate admin API key
- MiniMax API key stored in `.env`, never exposed to frontend

### 3.6 Operations Log

Every API operation is logged with:
- Timestamp (ISO 8601)
- Action type
- Target (voice_id or "bulk")
- Result (success/failure)
- API response code

Logs stored in `logs/operations.jsonl`, rotatable by size.

---

## 4. MiniMax API Integration

### Endpoints Used

| MiniMax Endpoint | Purpose |
|---|---|
| `POST /v1/get_voice` | List voices (`voice_type: "voice_cloning"`) |
| `POST /v1/t2a_v2` | Activate voice (synthesize short audio) |
| `POST /v1/delete_voice` | Delete voice |

### Activation Logic

MiniMax only shows voices that have been "activated" (synthesized at least once). The activation sends a short "Hello" audio synthesis request.

### Slot Limits

MiniMax accounts have max 10 cloning slots. The UI should display `X/10` clearly.

---

## 5. File Structure

```
voice-api/
├── SPEC.md              # This file
├── README.md            # Setup & usage guide
├── requirements.txt
├── .env.example
├── .env                 # Local secrets (gitignored)
├── main.py              # FastAPI application entry
├── core/
│   ├── __init__.py
│   ├── config.py        # Settings from env
│   ├── auth.py          # API key verification
│   ├── i18n.py          # Translation strings
│   └── ratelimit.py     # Rate limiting
├── routers/
│   ├── __init__.py
│   ├── voices.py        # /voices endpoints
│   ├── admin.py         # /admin/keys endpoints
│   └── health.py        # /health, /dashboard
├── services/
│   ├── __init__.py
│   └── minimax.py       # MiniMax API client
├── db/
│   ├── __init__.py
│   ├── keys.py          # API key storage (sqlite)
│   └── logs.py          # Operations log
├── templates/
│   └── dashboard.html    # Single-page dashboard
└── logs/
    └── .gitkeep
```

---

## 6. Configuration (`.env`)

```env
MINIMAX_API_KEY=sk-api-xxx...
MINIMAX_BASE_URL=https://api.minimax.io
ADMIN_API_KEY=sk-admin-xxx...        # Key for admin endpoints
SERVER_HOST=0.0.0.0
SERVER_PORT=8000
LOG_DIR=logs
```

---

## 7. API Response Format

All responses follow:

```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed",
  "timestamp": "2026-04-02T21:40:00-04:00"
}
```

Error responses:

```json
{
  "success": false,
  "error": {
    "code": "VOICE_NOT_FOUND",
    "message": "Voice with ID xxx not found"
  },
  "timestamp": "2026-04-02T21:40:00-04:00"
}
```

---

## 8. Acceptance Criteria

1. Dashboard loads at `/dashboard` with current voice list
2. Language switcher changes all UI text instantly (no reload)
3. All `/voices/*` endpoints return proper JSON with correct status codes
4. Unauthorized requests (missing/wrong API key) return 401
5. Rate limit exceeded returns 429 with `Retry-After` header
6. Operations log records every mutating action
7. Slot usage displayed as "X/10" with color coding (green < 7, yellow 7-9, red = 10)
8. Batch delete requires explicit confirmation in request body
9. MiniMax API errors surfaced with meaningful messages
10. Server starts with `uvicorn main:app` and reads all config from `.env`
