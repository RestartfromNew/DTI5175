from flask import Flask, render_template_string, request, jsonify
import urllib.request
import json
import ssl
import traceback

import firebase_admin
from firebase_admin import credentials, firestore, auth

API_KEY = "sk-api-52OCR6lwxIhoq1S998vNVRhT7qziYZQ7O41KOjMIui7abNsoYVQwrSTBuY30JdOi17s2KYh6OG6Mngr91Vt56pYPizTxSFIukzJaP8GRfnCHtCcVduKcYsU"
BASE_URL = "https://api.minimax.io"
SSL_CTX = ssl._create_unverified_context()

app = Flask(__name__)

# Initialize Firebase using the credentials detected on the VPS
try:
    cred = credentials.Certificate("/home/ubuntu/nexus-api/chat-chat-chat-fc723-firebase-adminsdk-fbsvc-afc5fef408.json")
    firebase_admin.initialize_app(cred)
    db = firestore.client()
except Exception as e:
    print(f"Failed to initialize Firebase: {e}")
    db = None

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

def fetch_firebase_users():
    if not db: return []
    users = []
    try:
        docs = db.collection("users").stream()
        for doc in docs:
            d = doc.to_dict()
            uid = doc.id
            
            name = "Unknown"
            try:
                auth_user = auth.get_user(uid)
                if auth_user.display_name:
                    name = auth_user.display_name
            except Exception:
                pass
                
            users.append({
                "uid": uid,
                "email": d.get("email", "No Email"),
                "name": name,
                "slot_used": d.get("slotUsed", 0),
                "slot_limit": d.get("slotLimit", 3)
            })
    except Exception as e:
        print(f"Error fetching firebase users: {e}")
    return users

def fetch_processed_payments():
    if not db: return []
    payments = []
    try:
        # Fetch last 50 successful payments
        docs = db.collection("processed_payments").limit(50).stream()
        for doc in docs:
            d = doc.to_dict()
            
            # Format timestamp
            ts = d.get("processedAt")
            ts_str = "Unknown"
            if ts:
                try:
                    # If it's a Firestore Timestamp, it has a to_datetime() method or is already a datetime
                    if hasattr(ts, "strftime"):
                        ts_str = ts.strftime("%Y-%m-%d %H:%M:%S")
                    else:
                        ts_str = str(ts)
                except:
                    ts_str = str(ts)

            payments.append({
                "pi_id": doc.id,
                "uid": d.get("uid", "N/A"),
                "slot_increment": d.get("slotIncrement", 0),
                "price_id": d.get("priceId", "N/A"),
                "processed_at": ts_str
            })
    except Exception as e:
        print(f"Error fetching payments: {e}")
    return payments

def get_user_data(uid):
    if not db: return {}
    doc = db.collection("users").document(uid).get()
    if doc.exists:
        return doc.to_dict()
    return {}

HTML_TEMPLATE = """
<!DOCTYPE html>
<html>
<head>
    <title>Management Dashboard (MiniMax & Firebase)</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background: #0f172a; color: #f8fafc; margin: 0; padding: 20px; }
        .container { max-width: 900px; margin: 0 auto; background: #1e293b; padding: 30px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.3); }
        h1 { margin-top: 0; color: #38bdf8; border-bottom: 2px solid #334155; padding-bottom: 15px; }
        h2 { color: #f472b6; margin-top: 40px; border-bottom: 2px solid #334155; padding-bottom: 10px; }
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
        
        /* Modal Styles */
        #modalOverlay { display:none; position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.6); z-index:999; backdrop-filter: blur(2px); }
        #userModal { display:none; position:fixed; top:50%; left:50%; transform:translate(-50%, -50%); background:#1e293b; padding:25px; border-radius:12px; z-index:1000; width:85%; max-width:600px; box-shadow:0 10px 25px rgba(0,0,0,0.5); border: 1px solid #475569; }
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
                <th>Owner (User)</th>
                <th>Voice ID</th>
                <th>Action</th>
            </tr>
            {% for v in voices %}
            <tr id="row-{{ v.voice_id }}">
                <td>{{ v.name | default('Unnamed Voice', true) }}</td>
                <td>
                    {% if voice_to_user[v.voice_id] %}
                        <span style="color: #60a5fa; cursor: pointer; text-decoration: underline;" onclick="showUserData('{{ voice_to_user[v.voice_id].uid }}')">
                            {{ voice_to_user[v.voice_id].name }}
                        </span>
                    {% else %}
                        <span style="color: #94a3b8; font-style: italic;">Ghost Voice</span>
                    {% endif %}
                </td>
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

        <!-- Firebase Section -->
        <h2>Firebase Authenticated Users</h2>
        <table id="users-table">
            <tr>
                <th>UID</th>
                <th>Slots Usage</th>
                <th>Name</th>
                <th>Email</th>
            </tr>
            {% for u in users %}
            <tr onclick="showUserData('{{ u.uid }}')" style="cursor: pointer; transition: background 0.2s;" onmouseover="this.style.background='#334155'" onmouseout="this.style.background='transparent'" title="Click to view full data">
                <td style="font-family: monospace; color: #60a5fa;">{{ u.uid }}</td>
                <td>
                    <span style="background: #0f172a; padding: 4px 8px; border-radius: 4px; font-weight: bold; border: 1px solid #334155; font-size: 0.9em; {% if u.slot_used >= u.slot_limit %}color: #f87171;{% else %}color: #4ade80;{% endif %}">
                        {{ u.slot_used }} / {{ u.slot_limit }}
                    </span>
                </td>
                <td>{{ u.name }}</td>
                <td>{{ u.email }}</td>
            </tr>
            {% else %}
            <tr>
                <td colspan="3" style="text-align: center; color: #94a3b8;">No Firebase users found or DB Error.</td>
            </tr>
            {% endfor %}
        </table>

        <!-- Payments Section -->
        <h2>Recent Payments (Stripe Success)</h2>
        <table id="payments-table">
            <tr>
                <th>Payment ID (PI)</th>
                <th>UID</th>
                <th>Slots +</th>
                <th>Processed At</th>
            </tr>
            {% for p in payments %}
            <tr>
                <td style="font-family: monospace; font-size: 0.85em; color: #94a3b8;">{{ p.pi_id }}</td>
                <td style="font-family: monospace; color: #60a5fa; cursor: pointer;" onclick="showUserData('{{ p.uid }}')">{{ p.uid }}</td>
                <td>
                    <span style="color: #4ade80; font-weight: bold;">+{{ p.slot_increment }}</span>
                </td>
                <td style="color: #94a3b8; font-size: 0.9em;">{{ p.processed_at }}</td>
            </tr>
            {% else %}
            <tr>
                <td colspan="4" style="text-align: center; color: #94a3b8;">No payment records found.</td>
            </tr>
            {% endfor %}
        </table>
    </div>

    <!-- User Data Popup Modal -->
    <div id="modalOverlay" onclick="closeModal()"></div>
    <div id="userModal">
        <h3 id="modalTitle" style="color:#f8fafc; margin-top:0;">User Data payload</h3>
        <pre id="modalContent" style="background:#0f172a; padding:15px; border-radius:8px; overflow-x:auto; color:#a5b4fc; font-size:14px; max-height:400px; overflow-y:auto; font-family: monospace;"></pre>
        <button onclick="closeModal()" class="btn-refresh" style="margin-top:15px; width: 100%;">Close</button>
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

        async function showUserData(uid) {
            try {
                document.getElementById('modalTitle').innerText = 'Loading data for UID: ' + uid + '...';
                document.getElementById('modalContent').innerText = 'Loading...';
                document.getElementById('modalOverlay').style.display = 'block';
                document.getElementById('userModal').style.display = 'block';

                const res = await fetch('/api/user/' + uid);
                const data = await res.json();
                
                document.getElementById('modalTitle').innerText = 'Data for UID: ' + uid;
                document.getElementById('modalContent').innerText = JSON.stringify(data, null, 2);
            } catch(e) {
                document.getElementById('modalContent').innerText = "Error fetching User Data";
            }
        }

        function closeModal() {
            document.getElementById('modalOverlay').style.display = 'none';
            document.getElementById('userModal').style.display = 'none';
        }

        // Close modal on Escape key
        document.addEventListener('keydown', function(event) {
            if (event.key === "Escape") {
                closeModal();
            }
        });
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
    
    users = fetch_firebase_users()
    payments = fetch_processed_payments()
    
    # Create a mapping of voice_id -> user_info
    voice_to_user = {}
    for u in users:
        # We need to fetch the full doc to get the 'voices' array
        user_doc = get_user_data(u["uid"])
        user_voices = user_doc.get("voices", [])
        for uv in user_voices:
            vid = uv.get("voiceId")
            if vid:
                voice_to_user[vid] = {
                    "name": u["name"],
                    "email": u["email"],
                    "uid": u["uid"]
                }

    return render_template_string(HTML_TEMPLATE, voices=voices, users=users, payments=payments, voice_to_user=voice_to_user)

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

@app.route("/api/user/<uid>")
def api_get_user(uid):
    data = get_user_data(uid)
    return jsonify(data)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=80)
