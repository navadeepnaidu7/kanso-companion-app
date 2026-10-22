"""Exercise phone controls and assert their effects on a connected test TV."""
import argparse
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(serial, *args):
    return subprocess.check_output(["adb", "-s", serial, *args], text=True, encoding="utf-8", timeout=25).strip()


def nodes(phone):
    adb(phone, "shell", "uiautomator", "dump", "/sdcard/kanso-test.xml")
    result = list(ET.fromstring(adb(phone, "shell", "cat", "/sdcard/kanso-test.xml")).iter("node"))
    assert any(n.get("package") == "com.navadeep.kanso.companion" for n in result), "Keep Kanso open on the phone during the check"
    return result


def tap(phone, label):
    n = next((n for n in nodes(phone) if label in (n.get("text"), n.get("content-desc"))), None)
    assert n is not None, f"Kanso control is not visible: {label}"
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
    adb(phone, "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def volume(tv):
    output = adb(tv, "shell", "cmd", "media_session", "volume", "--stream", "3", "--get")
    return int(re.search(r"volume is (\d+)", output).group(1))


def muted(tv):
    output = adb(tv, "shell", "dumpsys", "audio")
    return re.search(r"- STREAM_MUSIC:\s+Muted: (true|false)", output).group(1) == "true"


def foreground(tv, package):
    for _ in range(6):
        output = adb(tv, "shell", "dumpsys", "activity", "activities")
        if any(package + "/" in line for line in output.splitlines() if "topResumedActivity=" in line):
            return
        time.sleep(.5)
    raise AssertionError(f"TV did not show {package}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--phone", required=True)
    parser.add_argument("--tv", required=True)
    args = parser.parse_args()
    tap(args.phone, "Home")
    foreground(args.tv, "com.navadeep.kanso.tv")
    original = volume(args.tv)
    first, second = ("Volume up", "Volume down") if original < 100 else ("Volume down", "Volume up")
    try:
        tap(args.phone, first)
        assert volume(args.tv) != original, "Phone volume command did not change TV volume"
        tap(args.phone, second)
        assert volume(args.tv) == original, "Inverse volume command did not restore TV volume"
        print("Passed: real TV volume change and restore", flush=True)
    finally:
        if volume(args.tv) != original:
            adb(args.tv, "shell", "cmd", "media_session", "volume", "--stream", "3", "--set", str(original))
    original_mute = muted(args.tv)
    try:
        tap(args.phone, "Unmute" if original_mute else "Mute")
        assert muted(args.tv) != original_mute, "Phone mute command did not change TV mute state"
        assert any(n.get("content-desc") == ("Mute" if original_mute else "Unmute") for n in nodes(args.phone)), "Phone did not reflect TV mute state"
    finally:
        if muted(args.tv) != original_mute:
            tap(args.phone, "Mute" if original_mute else "Unmute")
    assert muted(args.tv) == original_mute
    print("Passed: direct mute, confirmed phone state, and restore", flush=True)
    tap(args.phone, "Apps")
    tap(args.phone, "Open YouTube on TV")
    foreground(args.tv, "com.google.android.youtube.tv")
    assert any(n.get("content-desc") == "Rewind" for n in nodes(args.phone)), "Phone did not adapt to YouTube"
    tap(args.phone, "Home")
    foreground(args.tv, "com.navadeep.kanso.tv")
    print("Passed: phone Home, real TV volume change/restore, mute/restore, YouTube launch, and app-aware remote")


if __name__ == "__main__":
    main()
