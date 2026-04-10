# Nexus Backend

FastAPI service for managing MiniMax voice cloning and general data storage powered by Firebase. Features a dense information dashboard, REST API with key authentication, and EN/ZH/FR support.

## Quick Start

```bash
cd nexus-api
cp .env.example .env
# Edit .env and fill in your MINIMAX_API_KEY, ADMIN_API_KEY and FIREBASE_SERVICE_ACCOUNT_PATH
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
| `FIREBASE_SERVICE_ACCOUNT_PATH` | Optional: Path to Firebase service account JSON |
| `SERVER_HOST` | Bind address (default: `0.0.0.0`) |
| `SERVER_PORT` | Port (default: `8000`) |
| `LOG_DIR` | Directory for operation logs (default: `logs`) |

## Features

- **MiniMax Voice Management**: List, activate, and delete voice cloning slots.
- **Firebase Integration**: Logs and potentially other data are stored in Firestore.
- **API Authentication**: X-API-Key based security.
- **Multi-language**: EN/ZH/FR support for API and Dashboard.
- **Log Management**: Local file + Remote Firestore logging.

## Data Storage

- API keys: `db/keys.json` (key content only shown once at creation)
- Operation logs: `logs/operations.jsonl` (local) + Firestore `operations` collection (remote)
- Generic Firestore support via `db/firestore_db.py`

## Project Structure

```
nexus-api/
├── main.py              # FastAPI app + CORS + rate limiting
├── core/
│   ├── config.py        # .env settings
│   ├── firebase.py      # Firebase initialization
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
│   ├── firebase_db.py   # Generic Firestore helper
│   └── logs.py          # Operation log (jsonl + Firestore)
├── templates/
│   └── dashboard.html   # Single-page dashboard UI
└── logs/                # Operation logs (gitignored)
```
