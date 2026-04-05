#!/usr/bin/env python3
"""
画幅比例诊断服务器
==================
在开发机上运行此脚本，手机上的 App 会自动将诊断数据发回。

用法:
    python diagnostics_server.py

前置条件:
    - 手机与开发机在同一 WiFi
    - 修改 DiagnosticsBackend.kt 中的 SERVER_URL 为本机 IP
    - App 已集成 DiagnosticsBackend 的调用

日志文件: aspect_ratio_diagnostics.jsonl (每行一条 JSON 记录)
"""

import json
import http.server
import socketserver
import socket
import sys
from datetime import datetime

PORT = 8765
LOG_FILE = "aspect_ratio_diagnostics.jsonl"

# ANSI 颜色
RED    = "\033[91m"
GREEN  = "\033[92m"
YELLOW = "\033[93m"
CYAN   = "\033[96m"
RESET  = "\033[0m"
BOLD   = "\033[1m"


def get_local_ip():
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"


def print_report(data: dict):
    ts = data.get("timestamp", "?")
    trigger = data.get("trigger", "?")
    device = data.get("device", {})
    ratio = data.get("selectedRatio", {})
    pv = data.get("previewView", {})
    bounds = data.get("computedBounds", {})
    cam_out = data.get("cameraOutput", {})
    cam_content = data.get("estimatedCameraContentInView", {})
    anomalies = data.get("anomalies", [])
    ok = data.get("ok", False)

    sep = "─" * 64
    print(f"\n{BOLD}{sep}{RESET}")
    print(f"{CYAN}{ts}{RESET}  [{trigger}]  {device.get('model','?')}  SDK={device.get('sdk','?')}")
    print(f"  屏幕:      {device.get('screenWidthPx')} × {device.get('screenHeightPx')} px  ({device.get('densityDpi')} dpi)")
    print(f"  PreviewView: {pv.get('widthPx')} × {pv.get('heightPx')} px  (ratio={pv.get('ratio')})")
    print(f"  选择比例:  {BOLD}{ratio.get('label')}{RESET}  (value={ratio.get('value')})")

    if cam_out:
        print(f"  相机输出:  {cam_out.get('widthPx')} × {cam_out.get('heightPx')} px  (ratio={cam_out.get('ratio')})")
    if cam_content:
        print(f"  相机内容区: left={cam_content.get('left'):.0f} top={cam_content.get('top'):.0f} "
              f"w={cam_content.get('width'):.0f} h={cam_content.get('height'):.0f}")

    top    = bounds.get('top', 0)
    bottom = bounds.get('bottom', 0)
    width  = bounds.get('width', 0)
    height = bounds.get('height', 0)
    ar     = bounds.get('actualRatio', '?')
    print(f"  计算边界:  left={bounds.get('left',0):.0f}  top={top:.0f}  "
          f"w={width:.0f}  h={height:.0f}  bottom={bottom:.0f}  ratio={ar}")
    print(f"  offsetYPx: {bounds.get('offsetYPx', 0):.1f}")

    # 关键诊断：边界是否超出 PreviewView
    pv_h = pv.get('heightPx', 0) or 0
    pv_w = pv.get('widthPx', 0) or 0
    if isinstance(top, (int, float)) and top < 0:
        print(f"  {YELLOW}⚠ bounds.top={top:.0f} < 0，顶部遮罩不生效（ViewfinderMask 跳过负值）{RESET}")
    if isinstance(bottom, (int, float)) and isinstance(pv_h, (int, float)) and bottom > pv_h:
        print(f"  {YELLOW}⚠ bounds.bottom={bottom:.0f} > PreviewView.height={pv_h:.0f}，底部超出{RESET}")
    if cam_content:
        cam_top    = cam_content.get('top', 0)
        cam_bottom = cam_content.get('bottom', 0)
        if isinstance(top, (int, float)) and isinstance(cam_top, (int, float)) and top < cam_top:
            print(f"  {RED}✗ 取景框顶部({top:.0f}) 高于相机内容顶部({cam_top:.0f})，取景框内有空白区域{RESET}")
        if isinstance(bottom, (int, float)) and isinstance(cam_bottom, (int, float)) and bottom > cam_bottom:
            print(f"  {RED}✗ 取景框底部({bottom:.0f}) 低于相机内容底部({cam_bottom:.0f})，取景框内有空白区域{RESET}")

    if anomalies:
        for a in anomalies:
            print(f"  {RED}✗ {a}{RESET}")
    else:
        print(f"  {GREEN}✓ 未检测到异常{RESET}")

    print(f"  整体状态:  {'✓ OK' if ok else f'{RED}✗ 有问题{RESET}'}")


class DiagnosticsHandler(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/diagnostics":
            self.send_response(404)
            self.end_headers()
            return

        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length).decode("utf-8")

        try:
            data = json.loads(body)
            print_report(data)

            with open(LOG_FILE, "a", encoding="utf-8") as f:
                f.write(body + "\n")

            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"ok")
        except Exception as e:
            print(f"{RED}解析错误: {e}{RESET}")
            self.send_response(400)
            self.end_headers()

    def log_message(self, fmt, *args):
        pass  # 屏蔽默认访问日志


def main():
    local_ip = get_local_ip()
    print(f"{BOLD}画幅比例诊断服务器{RESET}")
    print(f"监听端口: {PORT}")
    print(f"本机 IP:  {CYAN}{local_ip}{RESET}")
    print(f"日志文件: {LOG_FILE}")
    print()
    print(f"请将 DiagnosticsBackend.kt 中的 SERVER_URL 设置为:")
    print(f"  {BOLD}http://{local_ip}:{PORT}/diagnostics{RESET}")
    print()
    print("等待手机上报数据（切换比例时自动触发）...\n")

    try:
        with socketserver.TCPServer(("", PORT), DiagnosticsHandler) as httpd:
            httpd.serve_forever()
    except KeyboardInterrupt:
        print(f"\n服务器已停止。日志已保存到 {LOG_FILE}")
    except OSError as e:
        print(f"{RED}端口 {PORT} 被占用: {e}{RESET}")
        sys.exit(1)


if __name__ == "__main__":
    main()
