import urllib.request
import json
import ssl

# MiniMax API Configuration
API_KEY = "sk-api-52OCR6lwxIhoq1S998vNVRhT7qziYZQ7O41KOjMIui7abNsoYVQwrSTBuY30JdOi17s2KYh6OG6Mngr91Vt56pYPizTxSFIukzJaP8GRfnCHtCcVduKcYsU"
BASE_URL = "https://api.minimax.io"

def api_call(endpoint, payload):
    url = f"{BASE_URL}{endpoint}"
    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json"
    }
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method="POST")
    
    try:
        context = ssl._create_unverified_context()
        with urllib.request.urlopen(req, context=context) as response:
            return json.loads(response.read().decode("utf-8"))
    except Exception as e:
        print(f"网络错误: {e}")
        return None

def list_voices():
    print(f"\n--- 1. 正在查询已激活的语音列表 ---")
    payload = {"voice_type": "voice_cloning"}
    res_data = api_call("/v1/get_voice", payload)
    
    if res_data and res_data.get("base_resp", {}).get("status_code") == 0:
        voices = res_data.get("voices", [])
        if not voices:
            print("未发现已激活的克隆语音。")
        else:
            print(f"找到 {len(voices)} 个语音：")
            for i, v in enumerate(voices):
                print(f"{i+1}. 名称: {v.get('name')} | ID: {v.get('voice_id')}")
        return voices
    return []

def activate_voice(voice_id):
    print(f"\n尝试合成激活 ID: {voice_id} ...")
    payload = {
        "model": "speech-2.6-turbo",
        "text": "Hello", 
        "voice_setting": {"voice_id": voice_id},
        "audio_setting": {"sample_rate": 32000, "bitrate": 128000, "format": "mp3", "channel": 1}
    }
    res_data = api_call("/v1/t2a_v2", payload)
    if res_data and res_data.get("base_resp", {}).get("status_code") == 0:
        print("✅ 激活成功！")
        return True
    print(f"❌ 激活失败: {res_data.get('base_resp', {}).get('status_msg') if res_data else '网络错误'}")
    return False

def delete_voice(voice_id):
    print(f"\n正在尝试删除语音 ID: {voice_id} ...")
    payload = {
        "voice_type": "voice_cloning",
        "voice_id": voice_id
    }
    res_data = api_call("/v1/delete_voice", payload)
    
    if res_data and res_data.get("base_resp", {}).get("status_code") == 0:
        print(f"✅ 成功删除语音: {voice_id}")
        return True
    else:
        msg = res_data.get("base_resp", {}).get("status_msg") if res_data else "未知错误"
        print(f"❌ 删除失败: {msg}")
        return False

def main():
    while True:
        print("\n" + "="*40)
        print("MiniMax 语音槽位管理器 (9/10 状态清理)")
        print("="*40)
        print("1. 列出已激活语音")
        print("2. 激活特定 Voice ID (使其出现在列表中)")
        print("3. 删除特定 Voice ID (释放 Slot 插槽)")
        print("0. 退出")
        choice = input("\n请选择操作: ").strip()
        
        if choice == "1":
            list_voices()
        elif choice == "2":
            v_id = input("请输入要激活的 Voice ID: ").strip()
            if v_id: activate_voice(v_id)
        elif choice == "3":
            v_id = input("请输入要删除的 Voice ID: ").strip()
            if v_id: delete_voice(v_id)
        elif choice == "0":
            break
        else:
            print("无效选择。")

if __name__ == "__main__":
    main()
