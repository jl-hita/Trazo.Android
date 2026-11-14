#!/usr/bin/env python3
"""
Local stand-in for the TRAZO API, only to take screenshots of the app (landing page, store listing) without touching
a real account. It serves a fixed demo user and plan for /auth, /subscription and /plans, and forwards the public
exercise catalog (/api/v1/exercises*) to the real server so names and images are the real ones.

    python tools/mock-api/server.py --lang es --port 8099      # or --lang en

Build the app against it:   ./gradlew assembleDebug -PapiBaseUrl=http://10.0.2.2:8099/ -PsecureScreens=false
Sign in with any e-mail and password. Nothing is stored; every request answers from the fixtures below.
"""
import argparse
import json
import urllib.request
from datetime import date, timedelta
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

UPSTREAM = "https://trazostrength.duckdns.org"

T = {
    "es": {
        "user": "Lucía Martín", "macro": "Hipertrofia en 12 semanas", "notes": "Progresión doble con descargas programadas.",
        "phases": [("Base de volumen", "accumulation", "Construir volumen de trabajo con técnica sólida."),
                   ("Intensificación", "intensification", "Subir la carga manteniendo el volumen."),
                   ("Descarga", "deload", "Recuperar antes del siguiente bloque.")],
        "sessions": ["Tren superior A", "Tren inferior A", "Tren superior B"],
        "disclaimer": "Plan orientativo generado automáticamente; no sustituye el consejo de un profesional sanitario.",
        "adapt": "Vas bien: mantén la carga y busca una repetición más por serie.",
        "names": {"db-bench-press": "Press de Banca con Mancuernas", "seated-cable-row": "Remo Sentado en Polea",
                  "dumbbell-shoulder-press": "Press de Hombros con Mancuernas", "lat-pulldown": "Jalón al Pecho",
                  "hammer-curl": "Curl de Martillo con Mancuernas", "squat": "Sentadilla Trasera con Barra",
                  "romanian-deadlift": "Peso Muerto Rumano", "hack-squat": "Hack Squat", "hip-thrust": "Hip Thrust con Barra"},
    },
    "en": {
        "user": "Lucy Martin", "macro": "12-week muscle growth", "notes": "Double progression with scheduled deloads.",
        "phases": [("Volume base", "accumulation", "Build work capacity with solid technique."),
                   ("Intensification", "intensification", "Raise the load while keeping volume."),
                   ("Deload", "deload", "Recover before the next block.")],
        "sessions": ["Upper body A", "Lower body A", "Upper body B"],
        "disclaimer": "Auto-generated guidance; it does not replace the advice of a health professional.",
        "adapt": "You're on track: keep the load and aim for one more rep per set.",
        "names": {"db-bench-press": "Dumbbell Bench Press", "seated-cable-row": "Seated Cable Row",
                  "dumbbell-shoulder-press": "Dumbbell Shoulder Press", "lat-pulldown": "Lat Pulldown",
                  "hammer-curl": "Dumbbell Hammer Curl", "squat": "Barbell Back Squat",
                  "romanian-deadlift": "Romanian Deadlift", "hack-squat": "Hack Squat", "hip-thrust": "Barbell Hip Thrust"},
    },
}

LANG = "es"
UPPER = [("db-bench-press", 3, 8, 12, 2, 120, 22.5, ["Chest"], ["Triceps"]),
         ("seated-cable-row", 3, 8, 12, 2, 90, 45.0, ["Lats", "UpperBack"], ["Biceps"]),
         ("dumbbell-shoulder-press", 3, 8, 12, 2, 90, 14.0, ["Deltoids"], ["Triceps"]),
         ("lat-pulldown", 3, 10, 12, 2, 90, 50.0, ["Lats"], ["Biceps"]),
         ("hammer-curl", 2, 10, 15, 1, 60, 10.0, ["Biceps"], ["Forearms"])]
LOWER = [("squat", 4, 6, 10, 2, 150, 60.0, ["Glutes", "Quadriceps"], ["Hamstrings"]),
         ("romanian-deadlift", 3, 8, 12, 2, 120, 50.0, ["Glutes", "Hamstrings"], ["ErectorSpinae"]),
         ("hack-squat", 3, 10, 12, 2, 120, 70.0, ["Glutes", "Quadriceps"], []),
         ("hip-thrust", 3, 8, 12, 2, 90, 60.0, ["Glutes"], ["Hamstrings"])]


def monday():
    d = date.today()
    return d - timedelta(days=d.weekday())


def exercise(row, order):
    eid, sets, lo, hi, rir, rest, kg, prim, sec = row
    return {"exerciseId": eid, "name": T[LANG]["names"][eid], "order": order, "sets": sets, "minRepetitions": lo, "maxRepetitions": hi,
            "targetRir": rir, "restSeconds": rest, "progressionRule": "DoubleProgression", "targetLoadKg": kg, "loadBasis": None,
            "warmupSets": 1 if order == 1 else 0, "warmupNote": None, "primaryMuscles": prim, "secondaryMuscles": sec,
            "swappedFromExerciseId": None}


def session(i, day_offset, rows, minutes, logged=False):
    d = monday() + timedelta(days=day_offset)
    return {"id": f"00000000-0000-0000-0000-00000000000{i}", "weekNumber": 3, "date": d.isoformat(), "day": d.strftime("%A"),
            "title": T[LANG]["sessions"][i - 1], "estimatedDurationMinutes": minutes, "status": "Completed" if logged else "Planned",
            "exercises": [exercise(r, n + 1) for n, r in enumerate(rows)],
            "log": ({"id": "l1", "loggedAt": d.isoformat() + "T18:00:00Z", "outcome": "Completed", "nonCompletionReason": "None",
                     "recovery": "Good", "sessionRpe": 7, "painReported": False, "notes": None, "sets": []} if logged else None)}


def week():
    m = monday()
    return {"planId": "p1", "weekNumber": 3, "weekStart": m.isoformat(), "weekEnd": (m + timedelta(days=6)).isoformat(),
            "timeline": "InProgress", "mesocycle": {"sequence": 1, "phaseType": "accumulation", "isDeload": False, "startWeek": 1, "weeks": 4, "weekInBlock": 3},
            "sessions": [session(1, 0, UPPER, 55, logged=True), session(2, 2, LOWER, 60), session(3, 4, UPPER, 55)]}


def plan():
    m = monday() - timedelta(days=14)
    ph = T[LANG]["phases"]
    return {"id": "p1", "status": "Active", "timeline": "InProgress", "currentWeek": 3, "totalWeeks": 12, "startDate": m.isoformat(),
            "endDate": (m + timedelta(weeks=12) - timedelta(days=1)).isoformat(), "goal": "Hypertrophy", "experience": "Intermediate",
            "split": "UpperLower", "trainingDays": ["Monday", "Wednesday", "Friday"], "maxSessionMinutes": 60,
            "availableEquipment": ["barbell", "dumbbell", "cable", "machine", "bench"], "excludedExerciseIds": [], "limitations": [],
            "currentPhase": 1,
            "macrocycle": {"name": T[LANG]["macro"], "notes": T[LANG]["notes"], "phases": [
                {"sequence": i + 1, "name": n, "type": t, "startDate": (m + timedelta(weeks=4 * i)).isoformat(),
                 "endDate": (m + timedelta(weeks=4 * i + 4) - timedelta(days=1)).isoformat(), "objective": o} for i, (n, t, o) in enumerate(ph)]},
            "disclaimer": T[LANG]["disclaimer"]}


OPTIONS = {"goals": ["GeneralFitness", "Hypertrophy", "Strength"], "experience": ["Beginner", "Intermediate", "Advanced"],
           "splits": ["Automatic", "FullBody", "UpperLower", "PushPullLegs"],
           "days": ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"],
           "limitations": ["Knee", "Shoulder", "LowerBack", "Wrist", "Elbow"],
           "equipment": ["barbell", "dumbbell", "bench", "cable", "machine", "kettlebell", "band"],
           "minimumTrainingDays": 2, "maximumTrainingDays": 6, "minimumSessionMinutes": 35, "maximumSessionMinutes": 180,
           "minimumTotalWeeks": 4, "maximumTotalWeeks": 52, "defaultTotalWeeks": 12}


class Handler(BaseHTTPRequestHandler):
    def _send(self, status, body, ctype="application/json"):
        data = body if isinstance(body, bytes) else json.dumps(body).encode()
        self.send_response(status)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _proxy(self):
        try:
            with urllib.request.urlopen(UPSTREAM + self.path, timeout=20) as r:
                self._send(r.status, r.read(), r.headers.get("Content-Type", "application/json"))
        except urllib.error.HTTPError as e:
            self._send(e.code, e.read())

    def _route(self):
        path = urlparse(self.path).path
        if path.startswith("/api/v1/exercises"):
            return self._proxy()
        if self.command == "POST" and path.startswith("/api/v1/auth/"):
            n = int(self.headers.get("Content-Length") or 0)
            self.rfile.read(n)
            return self._send(200, {"accessToken": "demo-access", "refreshToken": "demo-refresh", "tokenType": "Bearer", "expiresIn": 3600})
        if path == "/api/v1/auth/me":
            return self._send(200, {"id": "u1", "email": "demo@trazo.app", "displayName": T[LANG]["user"], "role": "user", "twoFactorEnabled": False})
        if path == "/api/v1/subscription":
            return self._send(200, {"hasAccess": True, "enforced": False, "status": "none", "autoRenewing": False})
        if path == "/api/v1/plans/options":
            return self._send(200, OPTIONS)
        if path == "/api/v1/plans/current":
            return self._send(200, plan())
        if path == "/api/v1/plans/current/week":
            return self._send(200, week())
        if path == "/api/v1/plans/current/history":
            return self._send(200, [{"sessionId": "s0", "weekNumber": 3, "date": monday().isoformat(), "title": T[LANG]["sessions"][0],
                                     "outcome": "Completed", "loggedAt": monday().isoformat() + "T18:00:00Z", "completedSets": 14, "plannedSets": 14}])
        if path.endswith("/alternatives"):
            return self._send(200, {"exerciseId": "x", "alternatives": [], "message": None})
        if path.endswith("/log"):
            n = int(self.headers.get("Content-Length") or 0)
            self.rfile.read(n)
            return self._send(200, {"session": session(2, 2, LOWER, 60, logged=True),
                                    "adaptation": {"exercises": [], "sessionAction": "Hold", "sessionExplanation": T[LANG]["adapt"], "applied": [], "notes": []}})
        self._send(404, {"title": "not_found"})

    do_GET = do_POST = _route

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--lang", choices=["es", "en"], default="es")
    ap.add_argument("--port", type=int, default=8099)
    a = ap.parse_args()
    LANG = a.lang
    print(f"Mock TRAZO API ({LANG}) on http://0.0.0.0:{a.port}/")
    ThreadingHTTPServer(("0.0.0.0", a.port), Handler).serve_forever()
