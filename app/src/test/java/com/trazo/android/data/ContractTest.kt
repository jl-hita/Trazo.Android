package com.trazo.android.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.trazo.android.data.model.*
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.data.net.parseError
import org.junit.Assert.*
import org.junit.Test

/** The API contracts of docs/planning-engine.md and docs/subscriptions.md, as the server writes them. */
class ContractTest {
    private val gson = Gson()

    @Test fun `current week from the server is read completely`() {
        val json = """
        {"planId":"p1","weekNumber":1,"weekStart":"2026-11-16","weekEnd":"2026-11-22","timeline":"InProgress",
         "mesocycle":{"sequence":1,"phaseType":"accumulation","isDeload":false,"startWeek":1,"weeks":4,"weekInBlock":1},
         "sessions":[{"id":"s1","weekNumber":1,"date":"2026-11-16","day":"Monday","title":"Tren superior A",
           "estimatedDurationMinutes":51,"status":"Planned","log":null,
           "exercises":[{"exerciseId":"db-bench-press","name":"Press","order":1,"sets":3,"minRepetitions":8,"maxRepetitions":12,
             "targetRir":2.5,"restSeconds":120,"progressionRule":"DoubleProgression","targetLoadKg":22.5,"loadBasis":"x",
             "warmupSets":1,"warmupNote":"ligero","primaryMuscles":["Chest"],"secondaryMuscles":["Triceps"],"swappedFromExerciseId":null}]}]}
        """
        val week = gson.fromJson(json, WeekView::class.java)
        val ex = week.sessions.single().exercises.single()
        assertEquals("InProgress", week.timeline)
        assertEquals(4, week.mesocycle!!.weeks)
        assertEquals(2.5, ex.targetRir!!, 0.0)
        assertEquals(22.5, ex.targetLoadKg!!, 0.0)
        assertEquals(120, ex.restSeconds)
        assertNull(ex.swappedFromExerciseId)
        assertNull(week.sessions.single().log)
    }

    @Test fun `log request uses the exact field names of the API`() {
        val request = LogSessionRequest(
            "Partial", "Fatigue", "Good", 7.0, false, "ok",
            listOf(LogSetRequest("db-bench-press", 1, 22.5, 10, 2.0, true), LogSetRequest("db-bench-press", 2, null, null, null, false, "fatigue"))
        )
        val json = JsonParser.parseString(gson.toJson(request)).asJsonObject
        assertEquals(setOf("outcome", "nonCompletionReason", "recovery", "sessionRpe", "painReported", "notes", "sets"), json.keySet())
        val set = json.getAsJsonArray("sets")[0].asJsonObject
        assertEquals(setOf("exerciseId", "setNumber", "weightKg", "repetitions", "rir", "completed", "painReported"), set.keySet())
        assertEquals("fatigue", json.getAsJsonArray("sets")[1].asJsonObject["skippedReason"].asString)
    }

    @Test fun `plan profile request matches PlanProfileInput`() {
        val json = JsonParser.parseString(gson.toJson(PlanProfileRequest("Strength", "Beginner", trainingDays = listOf("Monday", "Friday")))).asJsonObject
        assertEquals("Strength", json["goal"].asString)
        assertEquals("Automatic", json["split"].asString)
        assertTrue(json["includeDeloadPhases"].asBoolean)
        assertEquals(2, json.getAsJsonArray("trainingDays").size())
        assertFalse(json.has("startDate")) // optional: the server defaults to today
    }

    @Test fun `log result carries the adaptation report`() {
        val json = """{"session":{"id":"s1","weekNumber":1,"date":"2026-11-16","day":"Monday","title":"t","estimatedDurationMinutes":50,"status":"Completed","exercises":[],"log":null},
          "adaptation":{"exercises":[{"exerciseId":"a","action":"IncreaseLoad","explanation":"sube","nextLoadKg":25.0,"suggestedRepetitions":[8,8,8]}],
          "sessionAction":"Hold","sessionExplanation":"bien","applied":[{"kind":"deload","exerciseIds":["a"],"sessionsChanged":2,"explanation":"x"}],"notes":["dolor"]}}"""
        val result = gson.fromJson(json, LogResult::class.java)
        assertEquals(25.0, result.adaptation.exercises.single().nextLoadKg!!, 0.0)
        assertEquals(listOf("dolor"), result.adaptation.notes)
        assertEquals(2, result.adaptation.applied.single().sessionsChanged)
    }

    @Test fun `subscription status decides when the plans screen shows`() {
        fun status(json: String) = gson.fromJson(json, SubscriptionStatus::class.java)
        val free = status("""{"hasAccess":true,"enforced":false,"status":"none","productId":null,"startedAt":null,"expiresAt":null,"autoRenewing":false,"source":"google_play"}""")
        val lapsed = status("""{"hasAccess":false,"enforced":true,"status":"expired","productId":"p","startedAt":"2026-01-01T00:00:00+00:00","expiresAt":"2026-02-01T00:00:00+00:00","autoRenewing":false,"source":"google_play"}""")
        val paying = status("""{"hasAccess":true,"enforced":true,"status":"active","productId":"p","startedAt":null,"expiresAt":null,"autoRenewing":true,"source":"google_play"}""")
        assertFalse(free.paywallRequired)
        assertTrue(lapsed.paywallRequired)
        assertFalse(paying.paywallRequired)
    }

    @Test fun `exercise page and detail`() {
        val page = gson.fromJson(
            """{"items":[{"id":"a","nameEs":"Sentadilla","nameEn":"Squat","descriptionEs":"d","difficulty":"Beginner","equipment":null,"requiredEquipment":[],
            "movementPattern":"Squat","primaryMuscles":["Quadriceps"],"secondaryMuscles":[],"minimumExperience":"Beginner","isUnilateral":false,"isBodyweight":true,"selectionPriority":3}],
            "page":1,"pageSize":30,"totalCount":1,"totalPages":1}""", ExercisePage::class.java
        )
        assertEquals("Sentadilla", page.items.single().nameEs)
        assertTrue(page.items.single().isBodyweight)
        val detail = gson.fromJson("""{"id":"a","nameEs":"S","nameEn":"S","descriptionEs":"d","instructionsEs":["1","2"],"tipsEs":[],"difficulty":"Beginner","equipment":null,"requiredEquipment":[],"bodyPart":"legs","met":5.0,"movementPattern":"Squat","primaryMuscles":["Glutes"],"secondaryMuscles":[],"suitableGoals":["Strength"]}""", ExerciseDetail::class.java)
        assertEquals(2, detail.instructionsEs.size)
    }

    @Test fun `plan error codes come from the problem title`() {
        val noPlan = parseError(404, """{"title":"no_active_plan","status":404,"detail":"No tienes un plan activo."}""")
        assertEquals(ErrorKind.NotFound, noPlan.kind)
        assertEquals("no_active_plan", noPlan.code)
        val validation = parseError(400, """{"title":"One or more validation errors occurred.","status":400,"errors":{"trainingDays":["Elige entre 2 y 6 días."]}}""")
        assertEquals(ErrorKind.Validation, validation.kind)
        assertNull(validation.code)
        assertEquals("Elige entre 2 y 6 días.", validation.fields["trainingDays"]!!.single())
        val gen = parseError(422, """{"title":"plan_generation_failed","detail":"x","errors":{"a":["b"]}}""")
        assertEquals("plan_generation_failed", gen.code)
    }

    @Test fun `payment required and billing errors`() {
        assertEquals(ErrorKind.SubscriptionRequired, parseError(402, """{"error":"subscription_required"}""").kind)
        assertEquals("subscription_required", parseError(402, """{"error":"subscription_required"}""").code)
        val linked = parseError(409, """{"error":"purchase_already_linked","message":"m"}""")
        assertEquals(ErrorKind.Conflict, linked.kind)
        assertEquals("purchase_already_linked", linked.code)
        assertEquals(ErrorKind.Server, parseError(503, "<html>").kind)
        assertTrue(parseError(503, null).retryable)
        assertFalse(parseError(400, "{}").retryable)
    }
}
