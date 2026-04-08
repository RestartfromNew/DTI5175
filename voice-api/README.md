# MiniMax Voice Manager

FastAPI service for managing MiniMax voice cloning slots (10 max). Features a dense information dashboard, REST API with key authentication, and EN/ZH/FR support.

## Quick Start

```bash
cd voice-api
cp .env.example .env
# Edit .env and fill in your MINIMAX_API_KEY and ADMIN_API_KEY
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

Open `http://localhost:8000/dashboard` for the web UI, or `http://localhost:8000/docs` for the Swagger API docs.

## Configuration

Edit `.env`:

| Variable | Description |
|---|---|
| `MINIMAX_API_KEY` | Your MiniMax API key |
| `MINIMAX_BASE_URL` | API base URL (default: `https://api.minimax.io`) |
| `ADMIN_API_KEY` | Key with admin privileges for `/admin/*` routes |
| `SERVER_HOST` | Bind address (default: `0.0.0.0`) |
| `SERVER_PORT` | Port (default: `8000`) |
| `LOG_DIR` | Directory for operation logs (default: `logs`) |

## API Authentication

All endpoints (except `/health`, `/dashboard`, `/docs`) require the header:

```
X-API-Key: your-api-key
```

Admin endpoints (`/admin/*`) additionally require the key matching `ADMIN_API_KEY`.

## API Endpoints

### Voices

| Method | Path | Description |
|---|---|---|
| `GET` | `/voices` | List all cloned voices |
| `GET` | `/voices/{voice_id}` | Get single voice details |
| `POST` | `/voices/{voice_id}/activate` | Activate a voice slot |
| `DELETE` | `/voices/{voice_id}` | Delete a voice |
| `POST` | `/voices/batch-delete` | Batch delete (body: `{"voice_ids": [...], "confirm": true}`) |

All voice endpoints accept `?locale=en|zh|fr` for translated messages.

### Admin

| Method | Path | Description |
|---|---|---|
| `GET` | `/admin/keys` | List API keys (admin only) |
| `POST` | `/admin/keys` | Create API key (body: `{"name": "my-key"}`) |
| `DELETE` | `/admin/keys/{key_id}` | Revoke a key (admin only) |

### Utility

| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Health check |
| `GET` | `/logs` | Get operation logs |
| `DELETE` | `/logs` | Clear operation logs |
| `GET` | `/dashboard` | Web dashboard |

## Dashboard Features

- Slot usage bar (green/yellow/red at 7/10 thresholds)
- Voice table with activate + delete per row
- Bulk select + delete
- Batch activate all inactive slots
- Live operation log panel
- Language switcher (EN / 中文 / FR) — instant, no reload
- API status indicator with auto-refresh every 30s

## Data Storage

- API keys: `db/keys.json` (key content only shown once at creation)
- Operation logs: `logs/operations.jsonl` (rotates at 500 lines)
- No external database required

## Project Structure

```
voice-api/
├── main.py              # FastAPI app + CORS + rate limiting
├── core/
│   ├── config.py        # .env settings
│   ├── auth.py          # X-API-Key verification
│   ├── i18n.py          # EN/ZH/FR translation strings
│   └── ratelimit.py     # slowapi limiter
├── routers/
│   ├── voices.py        # /voices endpoints
│   ├── admin.py         # /admin/keys endpoints
│   └── health.py        # /health, /logs, /dashboard
├── services/
│   └── minimax.py       # MiniMax API client (async httpx)
├── db/
│   ├── keys.py          # API key storage (json)
│   └── logs.py          # Operation log (jsonl)
├── templates/
│   └── dashboard.html   # Single-page dashboard UI
└── logs/                # Operation logs (gitignored)
```
