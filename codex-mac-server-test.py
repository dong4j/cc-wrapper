import subprocess
import json
import uuid
import threading
import time

proc = subprocess.Popen(
    ["codex", "mcp-server"],
    stdin=subprocess.PIPE,
    stdout=subprocess.PIPE,
    text=True
)

def send(method, params):
    req = {
        "jsonrpc": "2.0",
        "id": str(uuid.uuid4()),
        "method": method,
        "params": params
    }
    line = json.dumps(req)
    proc.stdin.write(line + "\n")
    proc.stdin.flush()

def read_loop():
    while True:
        line = proc.stdout.readline()
        if not line:
            break
        print("SERVER:", line.strip())

threading.Thread(target=read_loop, daemon=True).start()

time.sleep(0.2)

# Step 1: 正确的握手
send("initialize", {
    "protocolVersion": "2024-01-01",
    "clientInfo": {"name": "python-client", "version": "1.0"},
    "capabilities": {}
})

# Step 2: （可选）等 Codex 返回 capabilities

time.sleep(0.2)

# Step 3: 必须发送 initialized，否则 server 也会退出
send("initialized", {})

# Step 4: 现在你才可以安全调用 capabilities.get
time.sleep(0.2)
send("capabilities.get", {})

send("tools/list", {})

send("tools/call", {
    "name": "codex",
    "arguments": {
        "prompt": "讲一个简短的笑话"
    }
})
time.sleep(10.2)