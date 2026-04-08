from flask import Flask, render_template_string, request, jsonify
import urllib.request
import json
import ssl
import traceback

API_KEY = "sk-api-52OCR6lwxIhoq1S998vNVRhT7qziYZQ7O41KOjMIui7abNsoYVQwrSTBuY30JdOi17s2KYh6OG6Mngr91Vt56pYPizTxSFIukzJaP8GRfnCHtCcVduKcYsU"
BASE_URL = "https://api.minimax.io"
SSL_CTX = ssl._create_unverified_context()

app = Flask(__name__)

def api_post(endpoint, payload):
    data = json.dumps(payload).encode()
    req = urllib.request.Request(
        f"{BASE_URL}{endpoint}",
        data=data,
        headers={
            "Authorization": f"Bearer {API_KEY}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, context=SSL_CTX) as r:
            res_data = json.loads(r.read().decode())
            return res_data
    except Exception as e:
        print(f"Request Error [{endpoint}]: {e}")
        return None

def fetch_voices():
    res = api_post("/v1/get_voice", {"voice_type": "voice_cloning"})
    if not res: return []
    # The dictionary keys returned by minimax can slightly vary ("voices" vs "voice_cloning")
    voices = res.get("voice_cloning") or res.get("voices") or []
    return voices

def delete_voice(voice_id):
    res = api_post("/v1/delete_voice", {
        "voice_type": "voice_cloning",
        "voice_id": voice_id,
    })
    if res and res.get("base_resp", {}).get("status_code") == 0:
        return True
    return False

HTML_TEMPLATE = """
<!DOCTYPE html>
<html>
<head>
    <title>MiniMax Voice Dashboard</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 20px; }
        .container { max-width: 900px; margin: 0 auto; background: #1e293b; padding: 30px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.3); }
        h1 { margin-top: 0; color: #38bdf8; border-bottom: 2px solid #334155; padding-bottom: 15px; }
        .header-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
        .slots { font-size: 1.2rem; font-weight: 600; background: #334155; padding: 10px 15px; border-radius: 8px; }
        .slots.red { color: #f87171; }
        .slots.green { color: #4ade80; }
        button { border: none; padding: 10px 15px; cursor: pointer; border-radius: 6px; font-weight: 600; transition: background 0.2s; }
        .btn-refresh { background: #3b82f6; color: white; }
        .btn-refresh:hover { background: #2563eb; }
        .btn-delete-all { background: #ef4444; color: white; }
        .btn-delete-all:hover { background: #dc2626; }
        .btn-delete { background: #ef4444; color: white; padding: 6px 12px; font-size: 0.9rem; }
        .btn-delete:hover { background: #dc2626; }
        table { width: 100%; border-collapse: collapse; margin-top: 20px; background: #0f172a; border-radius: 8px; overflow: hidden; }
        th, td { text-align: left; padding: 15px; border-bottom: 1px solid #334155; }
        th { background: #334155; font-weight: 600; color: #cbd5e1; }
        tr:last-child td { border-bottom: none; }
        #status-msg { margin-top: 15px; font-weight: bold; height: 20px; }
    </style>
</head>
<body>
    <div class="container">
        <h1>MiniMax Voice Clone Dashboard</h1>
        
        <div class="header-actions">
            {% set count = voices|length %}
            <div class="slots {% if count >= 10 %}red{% else %}green{% endif %}">
                Slot Usage: {{ count }} / 10
            </div>
            <div>
                <button class="btn-refresh" onclick="location.reload()">Refresh</button>
                <button class="btn-delete-all" onclick="deleteAll()">Delete All</button>
            </div>
        </div>
        
        <div id="status-msg"></div>

        <table id="voices-table">
            <tr>
                <th>Name / Note</th>
                <th>Voice ID</th>
                <th>Action</th>
            </tr>
            {% for v in voices %}
            <tr id="row-{{ v.voice_id }}">
                <td>{{ v.name | default('Unnamed Voice', true) }}</td>
                <td style="font-family: monospace;">{{ v.voice_id }}</td>
                <td>
                    <button class="btn-delete" onclick="deleteVoice('{{ v.voice_id }}')">Delete</button>
                </td>
            </tr>
            {% else %}
            <tr>
                <td colspan="3" style="text-align: center; color: #94a3b8;">No cloned voices found.</td>
            </tr>
            {% endfor %}
        </table>
    </div>

    <script>
        function showMsg(text, isError=false) {
            const msg = document.getElementById('status-msg');
            msg.innerText = text;
            msg.style.color = isError ? '#f87171' : '#4ade80';
            setTimeout(() => msg.innerText = '', 3000);
        }

        async function deleteVoice(id) {
            if(!confirm("Are you sure you want to delete voice ID: " + id + "?")) return;
            
            try {
                const res = await fetch('/api/delete', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({voice_id: id})
                });
                if(res.ok) {
                    const row = document.getElementById('row-' + id);
                    if(row) row.style.display = 'none';
                    showMsg("Deleted successfully!");
                } else {
                    showMsg("Failed to delete voice.", true);
                }
            } catch(e) {
                showMsg("Network error.", true);
            }
        }
        
        async function deleteAll() {
            if(!confirm("WARNING! This will clear ALL your voice slots. Proceed?")) return;
            
            document.getElementById('status-msg').innerText = "Deleting all voices...";
            document.getElementById('status-msg').style.color = "#3b82f6";

            try {
                const res = await fetch('/api/delete_all', { method: 'POST' });
                if(res.ok) {
                    window.location.reload();
                } else {
                    showMsg("Failed to delete some or all voices.", true);
                }
            } catch(e) {
                showMsg("Network error.", true);
            }
        }
    </script>
</body>
</html>
"""

@app.route("/")
def index():
    try:
        voices = fetch_voices()
    except Exception as e:
        print(f"Error fetching voices: {e}")
        voices = []
    return render_template_string(HTML_TEMPLATE, voices=voices)

@app.route("/api/delete", methods=["POST"])
def api_delete():
    vid = request.json.get("voice_id")
    if delete_voice(vid):
        return jsonify({"success": True})
    return jsonify({"error": "Failed"}), 500

@app.route("/api/delete_all", methods=["POST"])
def api_delete_all():
    voices = fetch_voices()
    success = True
    for v in voices:
        vid = v.get("voice_id")
        if vid:
            if not delete_voice(vid):
                success = False
    if success:
        return jsonify({"success": True})
    return jsonify({"error": "Failed to delete all"}), 500

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=80)
