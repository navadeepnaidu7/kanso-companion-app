"""Verify foreground phone recovery after restarting only Kanso Receiver accessibility."""
import argparse
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(serial, *args):
    return subprocess.check_output(["adb", "-s", serial, *args], text=True, encoding="utf-8", timeout=25).strip()


def connected(phone):
    adb(phone, "shell", "uiautomator", "dump", "/sdcard/kanso-test.xml")
    root = ET.fromstring(adb(phone, "shell", "cat", "/sdcard/kanso-test.xml"))
    return any(n.get("content-desc", "").startswith("Connected to ") for n in root.iter("node"))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--phone", required=True)
    parser.add_argument("--tv", required=True)
    args = parser.parse_args()
    assert connected(args.phone), "Leave the connected phone remote open first"
    original = adb(args.tv, "shell", "settings", "get", "secure", "enabled_accessibility_services")
    services = original.split(":")
    receiver = [s for s in services if s.startswith("com.navadeep.kanso.receiver/")]
    assert receiver, "Kanso Receiver accessibility is not enabled"
    remaining = ":".join(s for s in services if s not in receiver)
    try:
        adb(args.tv, "shell", "settings", "put", "secure", "enabled_accessibility_services", remaining)
        time.sleep(5)
        assert not connected(args.phone), "Phone did not notice the closed receiver connection"
    finally:
        adb(args.tv, "shell", "settings", "put", "secure", "enabled_accessibility_services", original)
    for _ in range(6):
        time.sleep(2)
        if connected(args.phone):
            print("Passed: receiver restart detected and phone reconnected without reopening or pairing")
            return
    raise AssertionError("Phone did not reconnect after receiver restart")


if __name__ == "__main__":
    main()
