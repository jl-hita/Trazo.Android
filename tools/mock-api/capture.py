#!/usr/bin/env python3
"""
Takes the nine landing screenshots of the app in one language, on a running emulator that has the debug build installed:

    ./gradlew assembleDebug -PapiBaseUrl=http://10.0.2.2:8099/   &&   adb install -r app/build/outputs/apk/debug/app-debug.apk
    python tools/mock-api/capture.py de                           # needs lang/de.json (es and en are built into server.py)
    python tools/mock-api/capture.py de --webp ../Trazo/admin/Trazo.Admin.Web/public/landing/screenshots

It starts the mock API itself. The originals go to store/google-play/screenshots/{language}/; --webp also writes the landing copies.
Buttons are found by their position or by the app's own strings, so the same steps work in every language.
"""
import argparse
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ADB = os.environ.get("ADB") or os.environ["LOCALAPPDATA"] + "/Android/Sdk/platform-tools/adb.exe"
PACKAGE = "com.trazo.android"
# Language of the app -> (folder of its strings, tag for `cmd locale`).
FOLDERS = {"en": "values", "pt": "values-pt", "zh": "values-zh", "sr": "values-b+sr+Latn"}
LOCALES = {"pt": "pt-BR", "zh": "zh-Hans-CN", "sr": "sr-Latn-RS"}
# The day chips of the wizard follow the locale: how far Monday is from the first one
# when the week starts on Sunday or Saturday.
DAYS_BEFORE_MONDAY = {"pt": 1, "ja": 1, "ko": 1, "ar": 2}
# Right-to-left languages mirror every row, so the order of the buttons in the tree is what counts, not their position.


def adb(*args):
    return subprocess.run([ADB, *args], capture_output=True, timeout=180).stdout.decode("utf-8", "replace")


def shell(*args):
    return adb("shell", *args)


class Node:
    def __init__(self, element):
        self.text = element.get("text") or ""
        self.desc = element.get("content-desc") or ""
        self.cls = element.get("class") or ""
        self.clickable = element.get("clickable") == "true"
        self.left, self.top, self.right, self.bottom = map(int, re.findall(r"\d+", element.get("bounds")))
        self.texts = [e.get("text") for e in element.iter("node") if e.get("text")]

    @property
    def center(self):
        return (self.left + self.right) // 2, (self.top + self.bottom) // 2


def nodes():
    for _ in range(8):
        shell("uiautomator", "dump", "/sdcard/ui.xml")
        try:
            return [Node(e) for e in ET.fromstring(adb("exec-out", "cat", "/sdcard/ui.xml")).iter("node")]
        except ET.ParseError:
            time.sleep(1)
    raise SystemExit("could not read the screen")


def tap(node, wait=1.2):
    x, y = node.center
    shell("input", "tap", str(x), str(y))
    time.sleep(wait)


def clickables():
    return [n for n in nodes() if n.clickable]


def wait_for(predicate, what, seconds=40):
    end = time.time() + seconds
    while time.time() < end:
        screen = nodes()
        found = predicate(screen)
        if found:
            return found
        time.sleep(1)
    raise SystemExit("not on screen: " + what)


class Strings:
    def __init__(self, language):
        path = ROOT / "app/src/main/res" / FOLDERS.get(language, "values-" + language) / "strings.xml"
        self.values = {e.get("name"): self.unescape("".join(e.itertext())) for e in ET.parse(path).getroot() if e.tag == "string"}

    @staticmethod
    def unescape(value):
        value = value.strip()
        if len(value) > 1 and value[0] == value[-1] == '"':
            value = value[1:-1]
        return value.replace("\\'", "'").replace('\\"', '"').replace("%%", "%")

    def __getitem__(self, key):
        return self.values[key]


def fold(value):
    # Section titles are shown in capitals, and Turkish capitals do not round-trip: i -> İ and ı -> I.
    return value.casefold().replace("̇", "").replace("ı", "i")


def text(value):
    wanted = fold(value)
    return lambda screen: next((n for n in screen if wanted in (fold(n.text), fold(n.desc))), None)


def press(value, wait=1.5):
    """Taps the text, scrolling down until it is there."""
    for _ in range(6):
        found = text(value)(nodes())
        if found:
            tap(found, wait)
            return
        swipe_up(700)
    raise SystemExit("not on screen: " + value)


def swipe_up(distance=1500, duration=400):
    shell("input", "swipe", "640", "2200", "640", str(2200 - distance), str(duration))
    time.sleep(1)


def keyboard_shown():
    return "mInputShown=true" in shell("dumpsys", "input_method")


def hide_keyboard():
    if keyboard_shown():
        shell("input", "keyevent", "4")
        time.sleep(1)


class Mock:
    def __init__(self, language):
        self.language, self.process = language, None

    def start(self, *options):
        self.stop()
        self.process = subprocess.Popen([sys.executable, str(ROOT / "tools/mock-api/server.py"), "--lang", self.language, *options],
                                        stdout=subprocess.DEVNULL, env={**os.environ, "PYTHONUTF8": "1"})
        time.sleep(1.5)

    def stop(self):
        if self.process:
            self.process.kill()
            self.process.wait()
            self.process = None


def launch():
    shell("am", "force-stop", PACKAGE)
    shell("monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1")
    time.sleep(6)


def sign_in(language):
    shell("pm", "clear", PACKAGE)
    shell("cmd", "locale", "set-app-locales", PACKAGE, "--locales", LOCALES.get(language, language))
    shell("pm", "grant", PACKAGE, "android.permission.POST_NOTIFICATIONS")
    launch()
    fields = wait_for(lambda s: [n for n in s if n.cls.endswith("EditText")], "sign-in form")
    tap(fields[0])
    shell("input", "text", "demo@trazo.app")
    time.sleep(0.5)
    hide_keyboard()
    fields = [n for n in nodes() if n.cls.endswith("EditText")]
    tap(fields[1])
    shell("input", "text", "Demopassword123")
    time.sleep(0.5)
    hide_keyboard()
    screen = nodes()
    password = [n for n in screen if n.cls.endswith("EditText")][1]
    tap(next(n for n in screen if n.clickable and n.top >= password.bottom), 6)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("language")
    parser.add_argument("--wizard-only", action="store_true", help="only the three screenshots of the creation wizard")
    parser.add_argument("--progress-only", action="store_true", help="only the progress screenshot")
    parser.add_argument("--webp",help="landing screenshots folder: also writes {folder}/{language}/*.webp")
    args = parser.parse_args()
    language = args.language
    strings = Strings(language)
    out = ROOT / "store/google-play/screenshots" / language
    out.mkdir(parents=True, exist_ok=True)
    taken = []

    def shot(name):
        time.sleep(1.5)
        path = out / (name + ".png")
        path.write_bytes(subprocess.run([ADB, "exec-out", "screencap", "-p"], capture_output=True, timeout=120).stdout)
        taken.append(path)
        print("  " + name, flush=True)

    def finish():
        if args.webp:
            from PIL import Image
            target = Path(args.webp) / language
            target.mkdir(parents=True, exist_ok=True)
            for path in taken:
                image = Image.open(path).convert("RGB")
                image = image.resize((640, round(image.height * 640 / image.width)), Image.LANCZOS)
                image.save(target / (path.stem + ".webp"), quality=86, method=6)
        print(f"{len(taken)} screenshots in {out}")

    mock = Mock(language)
    try:
        if args.progress_only:
            mock.start()
            sign_in(language)
            wait_for(text(strings["next_view"]), "plan")
            press(strings["nav_progress"], 4)
            shot("19-progress")
            return finish()

        # ---- the creation wizard (an account without a plan)
        mock.start("--noplan")
        sign_in(language)
        # The first button of the "how do you want to start" screen is the automatic plan.
        choose = wait_for(lambda s: [n for n in s if n.clickable and n.bottom < 2500 and n.right - n.left > 900], "plan choice")
        tap(choose[0], 4)

        def chips():
            screen = nodes()
            # Clickable nodes below the top bar, in reading order of the tree.
            return [n for n in screen if n.clickable and n.top > 600]

        wizard = wait_for(lambda s: len([n for n in s if n.clickable and n.top > 600]) > 12 and chips(), "wizard")
        tap(wizard[1], 0.5)   # goal: muscle growth
        tap(wizard[4], 0.5)   # level: intermediate
        offset = DAYS_BEFORE_MONDAY.get(language, 0)
        for day in (8 + offset, 10 + offset, 12 + offset):  # Monday, Wednesday, Friday
            tap(chips()[day], 0.5)
        shot("10-wizard-goal")
        tap(chips()[7], 1)    # no fixed days
        sessions = chips()
        tap(sessions[9], 1)   # three sessions per cycle
        shot("11-wizard-flexible")
        tap(chips()[6], 1)    # back to fixed days for the details
        # Down to the aching areas, leaving their title where the Spanish screenshot has it.
        for _ in range(14):
            title = text(strings["onb_limitations"])(nodes())
            if title and title.top <= 2100:
                break
            swipe_up(min(900, title.top - 1950) if title else 900, 900)
        screen = nodes()
        title = text(strings["onb_limitations"])(screen)
        areas = [n for n in screen if n.clickable and title.bottom < n.top < 2560]
        tap(areas[1], 1)      # knee, shoulder, ...: shoulder
        shot("12-wizard-details")

        if args.wizard_only:
            return finish()

        # ---- the plan without dates
        mock.start("--mode", "sequence")
        sign_in(language)   # a clean start: the app keeps the plan it last saw
        wait_for(text(strings["next_view"]), "plan")
        shot("14-plan-sequence")

        # ---- the calendar plan: block, session, workout, adaptation, progress
        mock.start()
        sign_in(language)
        wait_for(text(strings["next_view"]), "plan")
        press(strings["plan_block_button"], 3)
        shot("15-block")
        shell("input", "keyevent", "4")
        time.sleep(2)
        for _ in range(3):
            shell("input", "swipe", "640", "900", "640", "2400", "250")
        time.sleep(1)
        press(strings["next_view"], 3)
        wait_for(text(strings["session_start"]), "session")
        shot("16-session")
        press(strings["session_start"], 3)
        for index, value in ((1, "8"), (2, "2")):
            fields = wait_for(lambda s: [n for n in s if n.cls.endswith("EditText")], "set fields")
            tap(fields[index])
            shell("input", "text", value)
            time.sleep(0.8)
            hide_keyboard()
        done = strings["workout_mark_set"].replace("%1$d", "1")
        press(done, 2)
        hide_keyboard()
        shot("17-workout")
        press(strings["workout_finish"], 3)
        press(strings["finish_send"], 4)
        wait_for(text(strings["finish_adaptation"]), "adaptation")   # the result screen has no top bar
        shot("18-adaptation")
        launch()
        wait_for(text(strings["next_view"]), "plan")
        # By its label: counting items of the bottom bar breaks when a button of the plan reaches that low.
        press(strings["nav_progress"], 4)
        shot("19-progress")
        finish()
    finally:
        mock.stop()


if __name__ == "__main__":
    main()
