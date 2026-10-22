"""Small ADB helper for inspecting this app on a connected test phone."""
import re
import subprocess
import sys
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.check_output(["adb", *args], text=True, encoding="utf-8")


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/kanso-test.xml")
    return ET.fromstring(adb("shell", "cat", "/sdcard/kanso-test.xml")).iter("node")


if __name__ == "__main__":
    action = sys.argv[1]
    if action in ("tap", "hold"):
        query = sys.argv[2]
        target = next(node for node in nodes() if node.get("text") == query or node.get("content-desc") == query)
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", target.get("bounds")))
        x, y = str((x1 + x2) // 2), str((y1 + y2) // 2)
        if action == "hold":
            adb("shell", "input", "swipe", x, y, x, y, "650")
        else:
            adb("shell", "input", "tap", x, y)
        print("Tapped:", query)
    elif action == "list":
        for node in nodes():
            if node.get("package", "").startswith("com.navadeep.kanso"):
                value = node.get("text") or node.get("content-desc")
                if value:
                    print(value, node.get("bounds"))
