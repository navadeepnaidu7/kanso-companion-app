"""Check retained library position on the selected, paired test phone."""
import re
import time
from device_ui import adb, nodes


def tap(label):
    node = next(n for n in nodes() if n.get("content-desc") == label or n.get("text") == label)
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def first_library_app():
    grid = next(n for n in nodes() if n.get("content-desc") == "App library")
    return next(n.get("content-desc") for n in grid.iter("node") if n.get("content-desc", "").startswith("Open "))


tap("Apps")
time.sleep(.6)
top = first_library_app()
adb("shell", "input", "swipe", "540", "2070", "540", "1050", "300")
time.sleep(.8)
scrolled = first_library_app()
assert scrolled != top, "Library didn't scroll"
tap("Close")
tap("Apps")
time.sleep(.6)
assert first_library_app() == scrolled, "Library lost its scroll position"
tap("Close")
print("Passed: library scrolling and retained position across close/reopen")
