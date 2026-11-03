"""
Macht Screenshots der App im Android-Emulator (läuft auf GitHub, siehe .github/workflows/screenshots.yml).
Legt Beispieldaten an (keine echten Arbeitszeiten), öffnet die Bildschirme und speichert PNGs in ./screens.
"""
import json
import os
import re
import subprocess
import time
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
from xml.sax.saxutils import escape

PKG = "de.marcel.zeitwaechter"
TZ = ZoneInfo("Europe/Berlin")
OUT = "screens"
os.makedirs(OUT, exist_ok=True)


def sh(*cmd, check=True):
    print("$", " ".join(cmd), flush=True)
    return subprocess.run(cmd, check=check, capture_output=True, text=True).stdout


def adb(*a, check=True):
    return sh("adb", *a, check=check)


def ms(dt):
    return int(dt.timestamp() * 1000)


def now():
    return datetime.fromtimestamp(int(adb("shell", "date", "+%s").strip()), TZ).replace(second=0, microsecond=0)


def sample_history(today):
    """Beispiel-Einträge: Rest des Vormonats und dieser Monat bis gestern (Mo–Fr + ein Samstag)."""
    rnd = [7, 31, 12, 45, 3, 22, 50, 14, 38, 9, 27, 41, 18, 55, 33]
    out = []
    start = (today.replace(day=1) - timedelta(days=20)).date()
    d = start
    i = 0
    while d < today.date():
        wd = d.weekday()
        if wd < 5 or (wd == 5 and d.day % 14 < 7):
            k = rnd[i % len(rnd)]
            i += 1
            begin = datetime(d.year, d.month, d.day, 7, 0, tzinfo=TZ) + timedelta(minutes=30 + k)
            length = timedelta(hours=8, minutes=45 + (k * 2) % 70) if wd < 5 else timedelta(hours=7, minutes=10)
            out.append({"s": ms(begin), "e": ms(begin + length), "q": "automatisch", "p": -1})
        d += timedelta(days=1)
    return out


def prefs(history, active=False, start=None, source="automatisch", latest_out=20 * 60 + 45, ot_start=None):
    vals = {
        "string": {"history": json.dumps(history), "session_source": source, "work_lat": "48.491234",
                   "work_lon": "9.204321", "hourly_rate": "25.5", "warn_list": "30, 15, 5",
                   "geofence_status": "Aktiv (Radius 150 m)", "holiday_state": "BW", "extra_holidays": ""},
        "boolean": {"session_active": active, "tracking_enabled": True},
        "long": {"ot_start_epochday": ot_start, "session_start": start or 0},
        "int": {"soll_min_day": 480, "start_balance_min": 35 * 60, "latest_out_min": latest_out, "radius_m": 150},
    }
    lines = ["<?xml version='1.0' encoding='utf-8' standalone='yes' ?>", "<map>"]
    for k, v in vals["string"].items():
        lines.append(f'    <string name="{k}">{escape(v, {chr(34): "&quot;"})}</string>')
    for k, v in vals["boolean"].items():
        lines.append(f'    <boolean name="{k}" value="{"true" if v else "false"}" />')
    for k, v in vals["long"].items():
        lines.append(f'    <long name="{k}" value="{v}" />')
    for k, v in vals["int"].items():
        lines.append(f'    <int name="{k}" value="{v}" />')
    lines.append("</map>")
    return "\n".join(lines)


def write_prefs(xml):
    adb("shell", "am", "force-stop", PKG)
    with open("z.xml", "w", encoding="utf-8") as f:
        f.write(xml)
    adb("push", "z.xml", "/data/local/tmp/z.xml")
    adb("shell", "chmod", "644", "/data/local/tmp/z.xml")
    adb("shell", f"run-as {PKG} sh -c 'mkdir -p shared_prefs && cat /data/local/tmp/z.xml > shared_prefs/zeitwaechter.xml'")


def launch(tab):
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity", "--ei", "tab", str(tab))
    time.sleep(4)


def shot(name):
    data = subprocess.run(["adb", "exec-out", "screencap", "-p"], check=True, capture_output=True).stdout
    with open(f"{OUT}/{name}.png", "wb") as f:
        f.write(data)
    print("📸", name, len(data), flush=True)


def tap_text(text):
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml")
    m = re.search(r'text="' + re.escape(text) + r'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    if not m:
        print("nicht gefunden:", text)
        return False
    x1, y1, x2, y2 = map(int, m.groups())
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    time.sleep(2)
    return True


def swipe_up(times=1):
    for _ in range(times):
        adb("shell", "input", "swipe", "540", "1900", "540", "500", "500")
        time.sleep(1)


n = now()
print("Emulator-Zeit:", n, flush=True)
hist = sample_history(n)
ot_start = (n.replace(day=1) - timedelta(days=20)).replace(day=1).date().toordinal() - datetime(1970, 1, 1).toordinal()

adb("shell", "cmd", "uimode", "night", "no")
# 1) Heute – noch im Minus
write_prefs(prefs(hist, True, ms(n - timedelta(hours=3, minutes=11)), ot_start=ot_start))
launch(0)
shot("1_heute_minus")

# 2) Heute – im Plus
write_prefs(prefs(hist, True, ms(n - timedelta(hours=9, minutes=34)), ot_start=ot_start))
launch(0)
shot("2_heute_plus")

# 3) Arbeitszeit oben und unten
launch(1)
shot("3_arbeitszeit")
swipe_up(3)
shot("4_arbeitszeit_unten")

# 4) Einstellungen und eine Detailseite
launch(2)
shot("5_einstellungen")
if tap_text("Zuschläge"):
    shot("6_einstellungen_zuschlaege")

# 5) Dunkel: 2. Einsatz, Grenze in 14 Minuten
adb("shell", "cmd", "uimode", "night", "yes")
time.sleep(2)
earlier = {"s": ms(n - timedelta(hours=11, minutes=10)), "e": ms(n - timedelta(hours=2, minutes=40)),
           "q": "automatisch", "p": -1}
cap = n + timedelta(minutes=14)
write_prefs(prefs(hist + [earlier], True, ms(n - timedelta(hours=1, minutes=19)),
                  latest_out=cap.hour * 60 + cap.minute, ot_start=ot_start))
launch(0)
shot("7_heute_dunkel_grenze")
launch(1)
shot("8_arbeitszeit_dunkel")

# 6) Nicht eingestempelt (hell)
adb("shell", "cmd", "uimode", "night", "no")
time.sleep(2)
write_prefs(prefs(hist, False, ot_start=ot_start))
launch(0)
shot("9_heute_nicht_eingestempelt")
