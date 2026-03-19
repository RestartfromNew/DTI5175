"""
MiniMax 克隆声音一键清空工具
用法: python delete_all_voices.py
"""

import urllib.request
import json
import ssl

API_KEY = "sk-api-52OCR6lwxIhoq1S998vNVRhT7qziYZQ7O41KOjMIui7abNsoYVQwrSTBuY30JdOi17s2KYh6OG6Mngr91Vt56pYPizTxSFIukzJaP8GRfnCHtCcVduKcYsU"
BASE_URL = "https://api.minimax.io"
SSL_CTX = ssl._create_unverified_context()


def api_post(endpoint: str, payload: dict) -> dict | None:
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
            return json.loads(r.read().decode())
    except Exception as e:
        print(f"  ❌ 网络错误: {e}")
        return None


def fetch_all_voice_ids() -> list[str]:
    """从 MiniMax 拉取所有已克隆声音的 voice_id 列表"""
    print("正在从 MiniMax 获取克隆声音列表 ...")
    res = api_post("/v1/get_voice", {"voice_type": "voice_cloning"})
    if not res:
        return []

    status = res.get("base_resp", {}).get("status_code", -1)
    if status != 0:
        msg = res.get("base_resp", {}).get("status_msg", "未知错误")
        print(f"  ❌ 获取失败 (code {status}): {msg}")
        return []

    # API 返回字段名为 "voice_cloning"
    voices = res.get("voice_cloning", [])
    ids = [v["voice_id"] for v in voices if "voice_id" in v]
    return ids


def delete_voice(voice_id: str) -> bool:
    res = api_post("/v1/delete_voice", {
        "voice_type": "voice_cloning",
        "voice_id": voice_id,
    })
    if res and res.get("base_resp", {}).get("status_code") == 0:
        print(f"  ✅ 已删除: {voice_id}")
        return True
    msg = res.get("base_resp", {}).get("status_msg", "未知错误") if res else "无响应"
    print(f"  ❌ 删除失败 [{voice_id}]: {msg}")
    return False


def main():
    print("=" * 50)
    print("  MiniMax 克隆声音 — 一键清空工具")
    print("=" * 50)

    voice_ids = fetch_all_voice_ids()

    if not voice_ids:
        print("\n✅ 当前没有克隆声音，无需清理。")
        return

    print(f"\n共找到 {len(voice_ids)} 个克隆声音：")
    for i, vid in enumerate(voice_ids, 1):
        print(f"  {i:2d}. {vid}")

    print(f"\n⚠️  即将删除以上全部 {len(voice_ids)} 个声音，此操作不可恢复！")
    confirm = input("确认删除？输入 YES 继续，其他任意键取消: ").strip()
    if confirm != "YES":
        print("已取消。")
        return

    print("\n开始删除 ...")
    ok, fail = 0, 0
    for vid in voice_ids:
        if delete_voice(vid):
            ok += 1
        else:
            fail += 1

    print("\n" + "=" * 50)
    print(f"完成！成功 {ok} 个，失败 {fail} 个。")
    if fail == 0:
        print("✅ 所有克隆声音已清空，槽位已全部释放。")
    else:
        print("⚠️  部分删除失败，请重新运行脚本再试。")


if __name__ == "__main__":
    main()
