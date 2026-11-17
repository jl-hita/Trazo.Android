#!/usr/bin/env python3
"""
Local stand-in for the TRAZO API, only to take screenshots of the app (landing page, store listing) without touching
a real account. It serves a fixed demo user and plan for /auth, /subscription and /plans, and forwards the public
exercise catalog (/api/v1/exercises*) to the real server so names and images are the real ones.

    python tools/mock-api/server.py --lang es --port 8099      # or --lang en

The language follows the Accept-Language header of each request (the app sends it), and --lang is only the default. Other switches show
the new screens: --mode sequence (a plan with no dates: cycles and sessions, no days), --recovery wait (recovery advice that says to
wait and offers another session), --behind 2 (offer to move a calendar plan that fell two weeks behind; POST reschedule clears it) and
--reentry 30 (30 days without training: the re-entry message).

Build the app against it:   ./gradlew assembleDebug -PapiBaseUrl=http://10.0.2.2:8099/ -PsecureScreens=false
Sign in with any e-mail and password. Nothing is stored; every request answers from the fixtures below.
"""
import argparse
import json
import threading
import urllib.request
from datetime import date, datetime, timedelta, timezone
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

LANG = "es"          # default language (--lang); each request may ask for another with Accept-Language
MODE = "calendar"    # --mode
RECOVERY = "ready"   # --recovery
BEHIND = 0           # --behind: weeks a calendar plan is behind the calendar (the offer to reschedule)
REENTRY = 0          # --reentry: days without training
_ctx = threading.local()


def lang():
    return getattr(_ctx, "lang", LANG)


def sequence():
    return MODE == "sequence"
UPPER = [("db-bench-press", 3, 8, 12, 2, 120, 22.5, ["Chest"], ["Triceps"]),
         ("seated-cable-row", 3, 8, 12, 2, 90, 45.0, ["Lats", "UpperBack"], ["Biceps"]),
         ("dumbbell-shoulder-press", 3, 8, 12, 2, 90, 14.0, ["Deltoids"], ["Triceps"]),
         ("lat-pulldown", 3, 10, 12, 2, 90, 50.0, ["Lats"], ["Biceps"]),
         ("hammer-curl", 2, 10, 15, 1, 60, 10.0, ["Biceps"], ["Forearms"])]
LOWER = [("squat", 4, 6, 10, 2, 150, 60.0, ["Glutes", "Quadriceps"], ["Hamstrings"]),
         ("romanian-deadlift", 3, 8, 12, 2, 120, 50.0, ["Glutes", "Hamstrings"], ["ErectorSpinae"]),
         ("hack-squat", 3, 10, 12, 2, 120, 70.0, ["Glutes", "Quadriceps"], []),
         ("hip-thrust", 3, 8, 12, 2, 90, 60.0, ["Glutes"], ["Hamstrings"])]


# Exercises done as a superset (consecutive ones share a letter) and what the demo user did last time.
SUPERSETS = {"dumbbell-shoulder-press": "A", "lat-pulldown": "A"}
LAST = {"db-bench-press": (22.5, [12, 12, 11]), "seated-cable-row": (45.0, [10, 10, 9]), "dumbbell-shoulder-press": (14.0, [9, 9, 8]),
        "lat-pulldown": (50.0, [12, 11, 11]), "hammer-curl": (10.0, [13, 12]), "squat": (60.0, [8, 8, 7, 7]),
        "romanian-deadlift": (50.0, [10, 10, 9]), "hack-squat": (70.0, [12, 12, 11]), "hip-thrust": (60.0, [10, 9, 9])}


def last_performance(eid, kg, lo, hi):
    weight, reps = LAST[eid]
    day = (monday() - timedelta(days=4)).isoformat()
    top = all(r >= hi for r in reps)
    return {"date": day, "sets": [{"setNumber": i + 1, "weightKg": weight, "repetitions": r, "rir": 2.0} for i, r in enumerate(reps)],
            "suggestedLoadKg": weight + (2.5 if top else 0), "suggestionBasis": "increase" if top else "repeat"}


def monday():
    d = date.today()
    return d - timedelta(days=d.weekday())


def exercise(row, order):
    eid, sets, lo, hi, rir, rest, kg, prim, sec = row
    return {"exerciseId": eid, "name": T[lang()]["names"][eid], "order": order, "sets": sets, "minRepetitions": lo, "maxRepetitions": hi,
            "targetRir": rir, "restSeconds": rest, "progressionRule": "DoubleProgression", "targetLoadKg": kg, "loadBasis": None,
            "warmupSets": 1 if order == 1 else 0, "warmupNote": None, "primaryMuscles": prim, "secondaryMuscles": sec,
            "swappedFromExerciseId": None, "lastPerformance": last_performance(eid, kg, lo, hi), "supersetGroup": SUPERSETS.get(eid)}


def session(i, day_offset, rows, minutes, logged=False, index=None):
    d = monday() + timedelta(days=day_offset)
    return {"id": f"00000000-0000-0000-0000-00000000000{i}", "weekNumber": 3,
            "date": None if sequence() else d.isoformat(), "day": None if sequence() else d.strftime("%A"), "sequenceIndex": index or (6 + i),
            "title": T[lang()]["sessions"][i - 1], "estimatedDurationMinutes": minutes, "status": "Completed" if logged else "Planned",
            "exercises": [exercise(r, n + 1) for n, r in enumerate(rows)],
            "log": ({"id": "l1", "loggedAt": d.isoformat() + "T18:00:00Z", "outcome": "Completed", "nonCompletionReason": "None",
                     "recovery": "Good", "sessionRpe": 7, "painReported": False, "notes": None, "sets": [],
                     "performedAt": d.isoformat() + "T18:00:00Z", "performedOn": d.isoformat()} if logged else None)}


def week():
    m = monday()
    return {"planId": "p1", "weekNumber": 3, "weekStart": None if sequence() else m.isoformat(),
            "weekEnd": None if sequence() else (m + timedelta(days=6)).isoformat(), "timeline": "InProgress", "mesocycle": {"sequence": 1, "phaseType": "accumulation", "isDeload": False, "startWeek": 1, "weeks": 4, "weekInBlock": 3},
            "sessions": [session(1, 0, UPPER, 55, logged=True), session(2, 2, LOWER, 60), session(3, 4, UPPER, 55)]}


def plan():
    m = monday() - timedelta(days=14)
    ph = T[lang()]["phases"]
    return {"id": "p1", "status": "Active", "timeline": "InProgress", "currentWeek": 3, "totalWeeks": 12, "startDate": m.isoformat(),
            "endDate": None if sequence() else (m + timedelta(weeks=12) - timedelta(days=1)).isoformat(), "goal": "Hypertrophy", "experience": "Intermediate",
            "split": "UpperLower", "trainingDays": [] if sequence() else ["Monday", "Wednesday", "Friday"], "maxSessionMinutes": 60,
            "availableEquipment": ["barbell", "dumbbell", "cable", "machine", "bench"], "excludedExerciseIds": [], "limitations": [],
            "currentPhase": 1,
            "macrocycle": {"name": T[lang()]["macro"], "notes": T[lang()]["notes"], "phases": [
                {"sequence": i + 1, "name": n, "type": t, "startDate": None if sequence() else (m + timedelta(weeks=4 * i)).isoformat(),
                 "endDate": None if sequence() else (m + timedelta(weeks=4 * i + 4) - timedelta(days=1)).isoformat(), "objective": o,
                 "startPeriod": 4 * i + 1, "endPeriod": 4 * i + 4} for i, (n, t, o) in enumerate(ph)]},
            "disclaimer": T[lang()]["disclaimer"], "origin": "Generated",
            "schedulingMode": "Sequence" if sequence() else "Calendar", "sessionsPerPeriod": 3}


def progress():
    m = monday()
    names = T[lang()]["names"]

    def points(start, step, n, reps=8):
        return [{"date": (m - timedelta(weeks=n - 1 - i)).isoformat(), "topWeightKg": start + step * i, "repsAtTop": reps,
                 "estimatedOneRepMaxKg": round((start + step * i) * (1 + reps / 30), 1), "volumeKg": (start + step * i) * reps * 3, "sets": 3}
                for i in range(n)]

    def ex(eid, sessions, start, step, record=False):
        pts = points(start, step, sessions)
        best = max(p["estimatedOneRepMaxKg"] for p in pts)
        return {"exerciseId": eid, "name": names[eid], "sessions": sessions, "lastDate": pts[-1]["date"], "bestWeightKg": pts[-1]["topWeightKg"],
                "bestEstimatedOneRepMaxKg": best, "recordDate": pts[-1]["date"], "newRecord": record, "points": pts}

    exercises = [ex("db-bench-press", 8, 16.0, 1.0, True), ex("squat", 8, 50.0, 2.5, True), ex("seated-cable-row", 7, 35.0, 1.5),
                 ex("romanian-deadlift", 6, 40.0, 2.5), ex("lat-pulldown", 7, 40.0, 1.5), ex("hip-thrust", 6, 50.0, 2.5)]
    weeks = [(m - timedelta(weeks=7 - i)).isoformat() for i in range(8)]
    muscles = [("Chest", [8, 9, 10, 10, 11, 12, 12, 13]), ("Quadriceps", [9, 10, 10, 11, 12, 12, 13, 13]), ("Lats", [8, 8, 9, 10, 10, 11, 11, 12]),
               ("Glutes", [11, 12, 12, 13, 14, 14, 15, 15.5]), ("Deltoids", [7, 7, 8, 8, 9, 9, 10, 10]), ("Hamstrings", [6, 6, 7, 7, 7.5, 8, 8, 8.5]),
               ("Biceps", [5, 5, 5.5, 6, 6, 6.5, 7, 7]), ("Triceps", [6, 6.5, 7, 7, 7.5, 7.5, 8, 8.5])]
    return {"summary": {"totalSessions": 22, "sessionsLast30Days": 12, "sessionsThisWeek": 1, "weeklyGoal": 3, "streakWeeks": 7, "totalSets": 301,
                        "totalVolumeKg": 48250},
            "weekStarts": weeks, "muscles": [{"muscle": n, "setsByWeek": v} for n, v in muscles], "exercises": exercises}


def block():
    w = week()
    m = monday()
    weeks = []
    for i in range(4):
        start = m - timedelta(weeks=2 - i)
        sessions = []
        for j, (offset, rows, title) in enumerate([(0, UPPER, 0), (2, LOWER, 1), (4, UPPER, 2)]):
            d = start + timedelta(days=offset)
            sessions.append({"id": f"b{i}{j}", "weekNumber": i + 1, "date": None if sequence() else d.isoformat(), "day": None if sequence() else d.strftime("%A"),
                             "sequenceIndex": i * 3 + j + 1, "title": T[lang()]["sessions"][title],
                             "estimatedDurationMinutes": 55, "status": "Planned",
                             "exercises": [dict(exercise(r, n + 1), sets=r[1] + (1 if i >= 2 and n < 2 else 0), targetRir=max(0, 3 - i)) for n, r in enumerate(rows)],
                             "log": None})
        weeks.append({"weekNumber": i + 1, "weekStart": None if sequence() else start.isoformat(), "weekEnd": None if sequence() else (start + timedelta(days=6)).isoformat(), "isCurrent": i == 2,
                      "sessions": sessions})
    return {"planId": "p1", "mesocycle": w["mesocycle"], "weeks": weeks, "locked": False}


def load():
    m = monday() - timedelta(days=14)
    weeks = []
    for i in range(12):
        planned = [30, 33, 36, 39, 30, 36, 39, 42, 45, 30, 36, 24][i]
        weeks.append({"week": i + 1, "weekStart": None if sequence() else (m + timedelta(weeks=i)).isoformat(), "phaseType": ["accumulation", "intensification", "deload"][i // 4],
                      "isDeload": i // 4 == 2, "plannedSets": planned if i < 4 else None, "completedSets": planned - 1 if i < 3 else None,
                      "plannedSessions": 3, "loggedSessions": 3 if i < 2 else (1 if i == 2 else 0)})
    return {"planId": "p1", "totalWeeks": 12, "currentWeek": 3, "unit": "sets", "weeks": weeks}


def next_session():
    """What GET plans/current/next answers: the next session in program order, where it sits, recovery advice and the offers."""
    names = {"es": {"wait": "Pecho: trabajado hace 9 h (10 series efectivas); necesita unas 31 h más.", "alt": T["es"]["sessions"][1]},
             "en": {"wait": "Chest: worked 9 h ago (10 effective sets); needs about 31 h more.", "alt": T["en"]["sessions"][1]}}[lang()]
    waiting = RECOVERY == "wait"
    nxt = session(3, 4, UPPER, 55, index=9) if waiting else session(2, 2, LOWER, 60, index=8)
    availability = ({"state": "Wait", "availableAt": (datetime.now(timezone.utc) + timedelta(hours=31)).isoformat(), "reasons": [names["wait"]],
                     "alternatives": [{"sessionId": session(2, 2, LOWER, 60)["id"], "sequenceIndex": 8, "title": names["alt"]}]}
                    if waiting else {"state": "Ready", "availableAt": None, "reasons": [], "alternatives": []})
    reentry = None
    if REENTRY:
        msg = ({"es": f"Llevas {REENTRY} días sin entrenar. Hemos aligerado esta sesión (una serie menos y una repetición más en reserva) para que vuelvas con buen pie; tu plan sigue donde lo dejaste.",
                "en": f"You have gone {REENTRY} days without training. We have lightened this session (one set less and one more rep in reserve) so you come back gently; your plan stays where you left it."})[lang()]
        reentry = {"daysSinceLastWorkout": REENTRY, "message": msg, "applied": True}
    return {"planId": "p1", "schedulingMode": "Sequence" if sequence() else "Calendar", "timeline": "InProgress", "session": nxt,
            "position": {"period": 3, "sessionInPeriod": 3 if waiting else 2, "sessionsInPeriod": 3,
                         "mesocycle": {"sequence": 1, "phaseType": "accumulation", "isDeload": False, "startWeek": 1, "weeks": 4, "weekInBlock": 3}},
            "availability": availability, "availabilityLocked": False, "overdue": [], "locked": False, "reentry": reentry,
            "reschedule": {"weeksBehind": BEHIND} if BEHIND and not sequence() else None}


OPTIONS = {"goals": ["GeneralFitness", "Hypertrophy", "Strength"], "experience": ["Beginner", "Intermediate", "Advanced"],
           "splits": ["Automatic", "FullBody", "UpperLower", "PushPullLegs"],
           "days": ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"],
           "limitations": ["Knee", "Shoulder", "LowerBack", "Wrist", "Elbow"],
           "equipment": ["barbell", "dumbbell", "bench", "cable", "machine", "kettlebell", "band"],
           "minimumTrainingDays": 2, "maximumTrainingDays": 6, "minimumSessionMinutes": 35, "maximumSessionMinutes": 180,
           "minimumTotalWeeks": 4, "maximumTotalWeeks": 52, "defaultTotalWeeks": 12,
           "schedulingModes": ["Calendar", "Sequence"], "minimumSessionsPerPeriod": 2, "maximumSessionsPerPeriod": 6}


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
        # The language of the app (it sends Accept-Language); --lang only when the request says nothing usable.
        asked = (self.headers.get("Accept-Language") or "").lower().replace(" ", "")
        _ctx.lang = "en" if asked.startswith("en") else "es" if asked.startswith("es") else LANG
        path = urlparse(self.path).path
        if path.startswith("/api/v1/exercises"):
            return self._proxy()
        if self.command == "POST" and path.startswith("/api/v1/auth/"):
            n = int(self.headers.get("Content-Length") or 0)
            self.rfile.read(n)
            return self._send(200, {"accessToken": "demo-access", "refreshToken": "demo-refresh", "tokenType": "Bearer", "expiresIn": 3600})
        if path == "/api/v1/auth/me":
            return self._send(200, {"id": "u1", "email": "demo@trazo.app", "displayName": T[lang()]["user"], "role": "user", "twoFactorEnabled": False,
                                    "googleLinked": False, "language": None})
        if self.command == "PUT" and path == "/api/v1/account/language":
            self.rfile.read(int(self.headers.get("Content-Length") or 0))
            return self._send(204, b"")
        if path == "/api/v1/subscription":
            return self._send(200, {"hasAccess": True, "enforced": False, "status": "none", "autoRenewing": False})
        if path == "/api/v1/plans/options":
            return self._send(200, OPTIONS)
        if self.command == "POST" and path == "/api/v1/plans/manual":
            self.rfile.read(int(self.headers.get("Content-Length") or 0))
            return self._send(201, plan())
        if path == "/api/v1/plans/current":
            return self._send(200, plan())
        if self.command == "PUT" and path == "/api/v1/plans/current/manual":
            self.rfile.read(int(self.headers.get("Content-Length") or 0))
            return self._send(200, plan())
        if path == "/api/v1/plans/current/next":
            return self._send(200, next_session())
        if self.command == "POST" and path == "/api/v1/plans/current/reschedule":
            global BEHIND
            moved = BEHIND
            BEHIND = 0
            start = monday()
            return self._send(200, {"weeksShifted": moved, "sessionsMoved": 3 * 4 * 3 if moved else 0, "startDate": start.isoformat(),
                                    "endDate": (start + timedelta(weeks=12) - timedelta(days=1)).isoformat()})
        if path == "/api/v1/plans/current/review-request":
            return self._send(404, {"title": "no_review_request", "status": 404, "detail": "No has pedido ninguna revisión."})
        if path == "/api/v1/plans/current/week":
            return self._send(200, week())
        if path == "/api/v1/plans/progress":
            return self._send(200, progress())
        if path == "/api/v1/plans/current/block":
            return self._send(200, block())
        if path == "/api/v1/plans/current/load":
            return self._send(200, load())
        if path == "/api/v1/plans/current/history":
            return self._send(200, [{"sessionId": "s0", "weekNumber": 3, "date": monday().isoformat(), "title": T[lang()]["sessions"][0],
                                     "outcome": "Completed", "loggedAt": monday().isoformat() + "T18:00:00Z", "completedSets": 14, "plannedSets": 14,
                                     "performedAt": monday().isoformat() + "T18:00:00Z"}])
        if path.endswith("/alternatives"):
            return self._send(200, {"exerciseId": "x", "alternatives": [], "message": None})
        if path.endswith("/log"):
            n = int(self.headers.get("Content-Length") or 0)
            self.rfile.read(n)
            return self._send(200, {"session": session(2, 2, LOWER, 60, logged=True),
                                    "adaptation": {"exercises": [], "sessionAction": "Hold", "sessionExplanation": T[lang()]["adapt"], "applied": [], "notes": []},
                                    "replayed": False})
        self._send(404, {"title": "not_found"})

    do_GET = do_POST = do_PUT = _route

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--lang", choices=["es", "en"], default="es")
    ap.add_argument("--port", type=int, default=8099)
    ap.add_argument("--mode", choices=["calendar", "sequence"], default="calendar")
    ap.add_argument("--recovery", choices=["ready", "wait"], default="ready")
    ap.add_argument("--behind", type=int, default=0)
    ap.add_argument("--reentry", type=int, default=0)
    a = ap.parse_args()
    LANG, MODE, RECOVERY, BEHIND, REENTRY = a.lang, a.mode, a.recovery, a.behind, a.reentry
    print(f"Mock TRAZO API ({LANG}) on http://0.0.0.0:{a.port}/")
    ThreadingHTTPServer(("0.0.0.0", a.port), Handler).serve_forever()
