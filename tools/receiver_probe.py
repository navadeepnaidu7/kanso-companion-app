"""Probe a receiver through `adb forward tcp:9876 tcp:9876` without printing credentials."""
import argparse
import json
import socket
import ssl
import hashlib
import time
import subprocess
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=9876)
    credentials = parser.add_mutually_exclusive_group(required=True)
    credentials.add_argument("--code")
    credentials.add_argument("--paired-phone", help="ADB serial of the paired debug-build phone")
    parser.add_argument("--launch-settings", action="store_true")
    parser.add_argument("--launch-app", help="Installed TV package to use for the foreground-app check")
    args = parser.parse_args()
    saved = None
    if args.paired_phone:
        raw = subprocess.check_output(["adb", "-s", args.paired_phone, "shell", "run-as",
            "com.navadeep.kanso.companion", "cat", "shared_prefs/paired-tv.xml"])
        saved = {node.attrib["name"]: node.text for node in ET.fromstring(raw)}
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    context.check_hostname = False
    context.verify_mode = ssl.CERT_NONE  # Only for this local test probe; the app pins paired TVs.
    with context.wrap_socket(socket.create_connection((args.host, args.port), timeout=20), server_hostname="kanso") as connection:
        if saved:
            assert hashlib.sha256(connection.getpeercert(binary_form=True)).hexdigest() == saved["certificate"], "TV certificate changed"
        stream = connection.makefile("rwb")

        def request(kind, **body):
            stream.write((json.dumps({"type": kind, **body}) + "\n").encode())
            stream.flush()
            return json.loads(stream.readline())

        assert request("hello")["ok"] is False, "Unauthenticated request accepted"
        if saved:
            token = saved["token"]
        else:
            wrong = "000000" if args.code != "000000" else "999999"
            assert request("pair", code=wrong)["ok"] is False, "Incorrect code accepted"
            paired = request("pair", code=args.code)
            assert paired["ok"], paired
            token = paired["token"]
        hello = request("hello", token=token)
        assert hello["ok"] and hello["version"] >= 1
        apps = request("apps", token=token)
        assert apps["ok"] and apps["apps"]
        assert all("id" in app and "name" in app and "icon" in app for app in apps["apps"])
        if hello["version"] >= 2:
            assert "activeApp" in hello
            assert all("banner" in app and "category" in app for app in apps["apps"])
            assert any(app["banner"] for app in apps["apps"]), "No TV banners received"
        assert request("command", token=token, key="invalid")["ok"] is False
        assert request("launch", token=token, id="does.not.exist")["ok"] is False
        assert request("hello", token=token)["ok"], "Error corrupted the connection"
        if args.launch_settings or args.launch_app:
            target = next(app for app in apps["apps"] if app["id"] == args.launch_app) if args.launch_app else next(
                app for app in apps["apps"] if app["id"] in ("com.android.settings", "com.android.tv.settings"))
            assert request("launch", token=token, id=target["id"])["ok"]
            if hello["version"] >= 2:
                for _ in range(12):
                    if request("hello", token=token).get("activeApp") == target["id"]:
                        break
                    time.sleep(.5)
                else:
                    raise AssertionError(f"Foreground app didn't follow launch: {request('hello', token=token).get('activeApp')}, expected {target['id']}")
                assert request("command", token=token, key="home")["ok"]
                for _ in range(12):
                    if request("hello", token=token).get("activeApp") != target["id"]:
                        break
                    time.sleep(.5)
                else:
                    raise AssertionError("Foreground app didn't follow Home")
                print("Passed: TV banners and foreground app tracking after launch and Home")
        print(f"Passed: TLS, authentication, {'certificate pinning' if saved else 'pairing'}, {len(apps['apps'])} apps, error recovery"
              + (", app launch" if args.launch_settings or args.launch_app else ""))


if __name__ == "__main__":
    main()
