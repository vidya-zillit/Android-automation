#!/usr/bin/env python3
"""Tiny adb UI helper for exploratory walks (never during an Appium run).

ui.py <serial> dump [filter]          compact node list: [i] id | text | desc | (cx,cy) flags
ui.py <serial> tap <needle>           tap first node whose id/text/desc contains needle (exact id match preferred)
ui.py <serial> tapxy <x> <y>
ui.py <serial> shot <file.png>
ui.py <serial> act                    top activity
"""
import re, subprocess, sys, xml.etree.ElementTree as ET, os

ADB = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
ENV = dict(os.environ, ADB_MDNS="0")
serial, cmd, *args = sys.argv[1:]


def adb(*a, text=True):
    return subprocess.run([ADB, "-s", serial, *a], capture_output=True, text=text, env=ENV).stdout


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/zl_ui.xml")
    xml = adb("exec-out", "cat", "/sdcard/zl_ui.xml")
    xml = xml[xml.find("<?xml"):]
    root = ET.fromstring(xml)
    out = []
    for n in root.iter("node"):
        b = re.findall(r"\d+", n.get("bounds", "[0,0][0,0]"))
        x1, y1, x2, y2 = map(int, b)
        rid = n.get("resource-id", "").split(":id/")[-1]
        out.append(dict(id=rid, text=n.get("text", ""), desc=n.get("content-desc", ""),
                        cls=n.get("class", "").split(".")[-1], cx=(x1 + x2) // 2, cy=(y1 + y2) // 2,
                        click=n.get("clickable") == "true", enabled=n.get("enabled") == "true",
                        checked=n.get("checked") == "true", sel=n.get("selected") == "true",
                        w=x2 - x1, h=y2 - y1))
    return out


if cmd == "dump":
    f = args[0].lower() if args else ""
    for i, n in enumerate(nodes()):
        if not (n["id"] or n["text"] or n["desc"]):
            continue
        line = f"[{i}] {n['id']} | {n['text'][:70]!r} | {n['desc'][:40]!r} | ({n['cx']},{n['cy']}) {n['cls']}" \
               + (" C" if n["click"] else "") + ("" if n["enabled"] else " DISABLED") \
               + (" CHECKED" if n["checked"] else "") + (" SEL" if n["sel"] else "")
        if f in line.lower():
            print(line.replace("\n", " "))
elif cmd == "tap":
    needle = args[0]
    ns = [n for n in nodes() if n["w"] > 0 and n["h"] > 0]
    hit = next((n for n in ns if n["id"] == needle), None) \
        or next((n for n in ns if n["text"] == needle or n["desc"] == needle), None) \
        or next((n for n in ns if needle.lower() in (n["text"] + " " + n["desc"]).lower()), None)
    if not hit:
        sys.exit(f"no node for {needle!r}")
    adb("shell", "input", "tap", str(hit["cx"]), str(hit["cy"]))
    print(f"tapped {hit['id'] or hit['text'] or hit['desc']!r} at ({hit['cx']},{hit['cy']})")
elif cmd == "tapxy":
    adb("shell", "input", "tap", args[0], args[1])
elif cmd == "shot":
    open(args[0], "wb").write(subprocess.run([ADB, "-s", serial, "exec-out", "screencap", "-p"],
                                             capture_output=True, env=ENV).stdout)
    print(args[0])
elif cmd == "act":
    out = adb("shell", "dumpsys", "activity", "activities")
    m = re.search(r"topResumedActivity=.*? (\S+/\S+)", out) or re.search(r"mResumedActivity: .*? (\S+/\S+)", out)
    print(m.group(1) if m else "?")
elif cmd == "type":
    adb("shell", "input", "text", args[0].replace(" ", "%s"))
elif cmd == "key":
    adb("shell", "input", "keyevent", args[0])
elif cmd == "swipe":
    adb("shell", "input", "swipe", *args)
