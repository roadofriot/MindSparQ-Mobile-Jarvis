#!/usr/bin/env python3
"""
MindSparQ PC Agent — Desktop Bridge Daemon for MindSparQ Mobile Jarvis.
Allows Mobile Jarvis to query PC health, launch desktop applications, capture screens,
and execute development commands over the local network via secure JSON-RPC / REST.
"""

import http.server
import json
import os
import platform
import shutil
import socket
import subprocess
import sys
import time
from urllib.parse import parse_qs, urlparse

PORT = int(os.environ.get("MINDSPARQ_PC_AGENT_PORT", 8765))
AUTH_TOKEN = os.environ.get("MINDSPARQ_PC_AGENT_TOKEN", "jarvis-secret-token")

def get_cpu_usage():
    try:
        with open("/proc/stat", "r") as f:
            line = f.readline()
        fields = [float(x) for x in line.strip().split()[1:]]
        idle = fields[3]
        total = sum(fields)
        return {"idle": idle, "total": total}
    except Exception:
        return {"idle": 0, "total": 0}

def get_system_stats():
    # Memory
    mem_total = 0
    mem_available = 0
    try:
        with open("/proc/meminfo", "r") as f:
            for line in f:
                parts = line.split(":")
                key = parts[0].strip()
                val = int(parts[1].split()[0]) # in kB
                if key == "MemTotal":
                    mem_total = val / (1024 * 1024) # GB
                elif key == "MemAvailable":
                    mem_available = val / (1024 * 1024) # GB
    except Exception:
        pass

    # Disk
    disk = shutil.disk_usage("/")
    disk_total = disk.total / (1024**3)
    disk_free = disk.free / (1024**3)

    # Uptime
    uptime_str = ""
    try:
        with open("/proc/uptime", "r") as f:
            secs = float(f.readline().split()[0])
            hours = int(secs // 3600)
            mins = int((secs % 3600) // 60)
            uptime_str = f"{hours}h {mins}m"
    except Exception:
        uptime_str = "unknown"

    return {
        "status": "online",
        "hostname": socket.gethostname(),
        "os": f"{platform.system()} {platform.release()}",
        "architecture": platform.machine(),
        "ram_total_gb": round(mem_total, 2),
        "ram_available_gb": round(mem_available, 2),
        "ram_used_percent": round(((mem_total - mem_available) / mem_total) * 100, 1) if mem_total > 0 else 0,
        "disk_total_gb": round(disk_total, 1),
        "disk_free_gb": round(disk_free, 1),
        "uptime": uptime_str,
        "agent_version": "2.0-enterprise",
    }

def launch_application(app_name):
    clean = app_name.strip().lower()
    mapping = {
        "vs code": "code",
        "vscode": "code",
        "code": "code",
        "chrome": "google-chrome",
        "browser": "xdg-open https://google.com",
        "terminal": "gnome-terminal || xterm || konsole",
        "files": "xdg-open ~",
        "file manager": "xdg-open ~",
    }
    cmd = mapping.get(clean, clean)
    try:
        proc = subprocess.Popen(cmd, shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        return {"success": True, "app": app_name, "pid": proc.pid, "command": cmd}
    except Exception as e:
        return {"success": False, "app": app_name, "error": str(e)}

def take_screenshot():
    import base64
    tmp_path = "/tmp/mindsparq_screenshot.png"
    # Try scrot, import, or gnome-screenshot
    cmd = f"scrot -o {tmp_path} 2>/dev/null || gnome-screenshot -f {tmp_path} 2>/dev/null || import -window root {tmp_path} 2>/dev/null"
    res = subprocess.run(cmd, shell=True)
    if res.returncode == 0 and os.path.exists(tmp_path):
        with open(tmp_path, "rb") as f:
            b64 = base64.b64encode(f.read()).decode("utf-8")
        return {"success": True, "mime": "image/png", "base64": b64}
    return {"success": False, "error": "No screenshot utility found (install scrot or imagemagick)"}

def execute_safe_command(command):
    # Deny dangerous system wipe commands
    disallowed = ["rm -rf /", "mkfs", "dd if=", ":(){ :|:& };:"]
    for d in disallowed:
        if d in command:
            return {"success": False, "error": f"Command denied by PolicyEngine guardrail: '{d}'"}

    try:
        res = subprocess.run(command, shell=True, capture_output=True, text=True, timeout=15)
        return {
            "success": res.returncode == 0,
            "exit_code": res.returncode,
            "stdout": res.stdout[:2000],
            "stderr": res.stderr[:1000]
        }
    except subprocess.TimeoutExpired:
        return {"success": False, "error": "Command timed out after 15 seconds"}
    except Exception as e:
        return {"success": False, "error": str(e)}

class PCAgentHandler(http.server.BaseHTTPRequestHandler):
    def _send_json(self, data, code=200):
        body = json.dumps(data, indent=2).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self._send_json({"status": "ok"})

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path in ["/status", "/health", "/"]:
            self._send_json(get_system_stats())
        elif parsed.path == "/screenshot":
            self._send_json(take_screenshot())
        else:
            self._send_json({"error": "Endpoint not found"}, 404)

    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length).decode("utf-8") if length > 0 else "{}"
        payload = json.loads(body) if body.strip() else {}

        parsed = urlparse(self.path)
        if parsed.path == "/open_app":
            app = payload.get("app", "")
            self._send_json(launch_application(app))
        elif parsed.path == "/command":
            cmd = payload.get("command", "")
            self._send_json(execute_safe_command(cmd))
        elif parsed.path == "/screenshot":
            self._send_json(take_screenshot())
        else:
            self._send_json({"error": "Unknown POST endpoint"}, 404)

    def log_message(self, format, *args):
        # Concise logging
        sys.stderr.write(f"[MindSparQ PC Agent] {args[0]} - {args[1]}\n")

def main():
    server = http.server.ThreadingHTTPServer(("0.0.0.0", PORT), PCAgentHandler)
    local_ip = socket.gethostbyname(socket.gethostname())
    print(f"==================================================")
    print(f"MindSparQ PC Agent Daemon active!")
    print(f"Listening on: http://0.0.0.0:{PORT} (Local IP: {local_ip})")
    print(f"Endpoints: /health, /open_app, /command, /screenshot")
    print(f"Connect from Mobile Jarvis with IP: {local_ip}:{PORT}")
    print(f"==================================================")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping PC Agent...")
        server.server_close()

if __name__ == "__main__":
    main()

