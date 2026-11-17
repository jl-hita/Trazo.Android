package com.trazo.android.data

import com.google.gson.Gson
import com.trazo.android.data.local.JsonFileStore
import com.trazo.android.data.local.PendingLog
import com.trazo.android.data.local.PendingLogQueue
import com.trazo.android.data.local.PlanCache
import com.trazo.android.data.model.*
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.LogOutcome
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.testing.*
import com.trazo.android.ui.onboarding.OnboardingState
import com.trazo.android.ui.onboarding.OnboardingViewModel
import com.trazo.android.ui.workout.ExerciseEntry
import com.trazo.android.ui.workout.FinishForm
import com.trazo.android.ui.workout.SetEntry
import com.trazo.android.ui.workout.WorkoutViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Registering a workout is not the same as doing it: the moment it happened and the identity of the request are fixed once, when
 * the workout ends, and travel unchanged through retries, the offline queue and a late send.
 */
class FlexibleRegistrationTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var api: FakePlansApi
    private lateinit var queue: PendingLogQueue
    private lateinit var cache: PlanCache
    private lateinit var repo: PlanRepository
    private val ended = Instant.parse("2026-11-17T18:30:00Z").toEpochMilli()

    @Before fun setUp() {
        api = FakePlansApi()
        cache = PlanCache(JsonFileStore(File(tmp.root, "cache")), { ended }, { LocalDate.of(2026, 11, 17) })
        queue = PendingLogQueue(JsonFileStore(File(tmp.root, "queue")))
        repo = PlanRepository(api, cache, queue)
    }

    private fun ok(): Response<LogResult> = Response.success(LogResult(sampleSession(), sampleReport()))

    @Test fun `the first attempt already carries the time of the workout and a request id`() = runTest {
        api.logResponse = { _, _ -> ok() }
        repo.submitLog("s1", logRequest(), now = ended)
        val sent = api.logCalls.single().second
        assertEquals("2026-11-17T18:30:00Z", sent.performedAt)
        assertNotNull(sent.clientRequestId)
    }

    @Test fun `a date chosen by the user is never replaced`() = runTest {
        api.logResponse = { _, _ -> ok() }
        repo.submitLog("s1", logRequest().copy(performedAt = "2026-11-14T10:00:00Z", clientRequestId = "fixed"), now = ended)
        val sent = api.logCalls.single().second
        assertEquals("2026-11-14T10:00:00Z", sent.performedAt)
        assertEquals("fixed", sent.clientRequestId)
    }

    @Test fun `a log queued offline keeps its date and id and is sent unchanged later`() = runTest {
        api.logResponse = { _, _ -> networkFailure() }
        assertEquals(LogOutcome.Queued, repo.submitLog("s1", logRequest(), now = ended))
        val first = api.logCalls.single().second
        assertEquals(first, queue.all().single().request) // what is stored is exactly what was sent

        api.logResponse = { _, _ -> ok() }
        repo.flushPending() // hours later: the clock is not read again
        val delivered = api.logCalls.last().second
        assertEquals("2026-11-17T18:30:00Z", delivered.performedAt)
        assertEquals(first.clientRequestId, delivered.clientRequestId)
        assertTrue(queue.all().isEmpty())
    }

    @Test fun `a log queued by an older version is dated when it was queued, with the same id on every attempt`() = runTest {
        queue.add(PendingLog("s9", logRequest(), ended)) // no performedAt, no clientRequestId
        api.logResponse = { _, _ -> networkFailure() }
        repo.flushPending()
        repo.flushPending()
        val (a, b) = api.logCalls.takeLast(2).map { it.second }
        assertEquals("2026-11-17T18:30:00Z", a.performedAt)
        assertEquals(a.clientRequestId, b.clientRequestId) // a retry is the same request
        assertEquals(1, queue.all().size) // still waiting
    }

    @Test fun `the next session is asked from the backend and can be opened even if it is not in the current week`() = runTest {
        val late = sampleSession(id = "late-one")
        api.nextResponse = { Response.success(sampleNext(session = late)) }
        repo.currentWeek() // the week holds only s1
        val next = (repo.next() as ApiResult.Success).value
        assertEquals("late-one", next.session!!.id)
        assertEquals("late-one", repo.session("late-one")!!.id)
        assertNull(repo.session("unknown"))
    }

    // ------------------------------------------------------------------ what the screen sends

    private fun entries() = listOf(ExerciseEntry(sampleExercise(), listOf(SetEntry("20", "10", "2", true))))

    @Test fun `another day sends that day at the current time of day, today sends nothing`() {
        val now = ZonedDateTime.of(2026, 11, 17, 21, 15, 0, 0, ZoneId.of("Europe/Madrid"))
        assertNull(WorkoutViewModel.performedAtFor(null, now))
        assertNull(WorkoutViewModel.performedAtFor(LocalDate.of(2026, 11, 17), now))
        assertEquals("2026-11-14T20:15:00Z", WorkoutViewModel.performedAtFor(LocalDate.of(2026, 11, 14), now))
        assertNull(WorkoutViewModel.performedAtFor(LocalDate.of(2026, 11, 18), now)) // never in the future
    }

    @Test fun `the log request carries the chosen day`() {
        val now = ZonedDateTime.of(2026, 11, 17, 21, 15, 0, 0, ZoneId.of("Europe/Madrid"))
        val late = WorkoutViewModel.buildLogRequest(entries(), FinishForm(performedOn = LocalDate.of(2026, 11, 15)), now)
        assertEquals("2026-11-15T20:15:00Z", late.performedAt)
        assertNull(WorkoutViewModel.buildLogRequest(entries(), FinishForm(), now).performedAt)
    }

    // ------------------------------------------------------------------ contracts

    private val gson = Gson()

    @Test fun `a sequence plan has no dates and the app reads it`() {
        val week = gson.fromJson(
            """{"planId":"p1","weekNumber":2,"weekStart":null,"weekEnd":null,"timeline":"InProgress",
              "mesocycle":{"sequence":1,"phaseType":"accumulation","isDeload":false,"startWeek":1,"weeks":4,"weekInBlock":2},
              "sessions":[{"id":"s3","weekNumber":2,"date":null,"day":null,"title":"Pierna","estimatedDurationMinutes":50,"status":"Planned",
                "exercises":[],"log":null,"sequenceIndex":3}]}""",
            WeekView::class.java
        )
        assertNull(week.weekStart)
        val session = week.sessions.single()
        assertNull(session.date)
        assertNull(session.day)
        assertEquals(3, session.sequenceIndex)

        val plan = gson.fromJson(
            """{"id":"p","status":"Active","timeline":"InProgress","currentWeek":2,"totalWeeks":12,"startDate":"2026-11-09","endDate":null,
              "goal":"Hypertrophy","experience":"Beginner","split":"Automatic","trainingDays":[],"maxSessionMinutes":60,
              "availableEquipment":[],"excludedExerciseIds":[],"limitations":[],"currentPhase":1,
              "macrocycle":{"name":"M","notes":"","phases":[{"sequence":1,"name":"Base","type":"accumulation","startDate":null,"endDate":null,
                "objective":"x","startPeriod":1,"endPeriod":4}]},"disclaimer":"d","schedulingMode":"Sequence","sessionsPerPeriod":3}""",
            PlanView::class.java
        )
        assertNull(plan.endDate)
        assertEquals("Sequence", plan.schedulingMode)
        assertNull(plan.macrocycle.phases.single().startDate)
        assertEquals(4, plan.macrocycle.phases.single().endPeriod)
    }

    @Test fun `a calendar plan from an older server still reads, with the defaults of the new fields`() {
        val plan = gson.fromJson(
            """{"id":"p","status":"Active","timeline":"InProgress","currentWeek":1,"totalWeeks":4,"startDate":"2026-11-09","endDate":"2026-12-06",
              "goal":"Hypertrophy","experience":"Beginner","split":"Automatic","trainingDays":["Monday"],"maxSessionMinutes":60,
              "availableEquipment":[],"excludedExerciseIds":[],"limitations":[],"currentPhase":1,
              "macrocycle":{"name":"M","notes":"","phases":[]},"disclaimer":"d"}""",
            PlanView::class.java
        )
        assertEquals("2026-12-06", plan.endDate)
    }

    @Test fun `the next session is read with its recovery advice`() {
        val next = gson.fromJson(
            """{"planId":"p","schedulingMode":"Sequence","timeline":"InProgress","availabilityLocked":false,"locked":false,
              "session":{"id":"s2","weekNumber":1,"date":null,"day":null,"title":"Empuje B","estimatedDurationMinutes":50,"status":"Planned","exercises":[],"log":null,"sequenceIndex":2},
              "position":{"period":1,"sessionInPeriod":2,"sessionsInPeriod":3,"mesocycle":null},
              "availability":{"state":"Wait","availableAt":"2026-11-18T08:00:00+00:00","reasons":["Pecho: trabajado hace 6 h"],
                "alternatives":[{"sessionId":"s3","sequenceIndex":3,"title":"Pierna"}]},
              "overdue":[],"reentry":{"daysSinceLastWorkout":30,"message":"Llevas 30 días sin entrenar."}}""",
            NextSessionView::class.java
        )
        assertEquals("Wait", next.availability!!.state)
        assertEquals("Pierna", next.availability!!.alternatives.single().title)
        assertEquals(3, next.position!!.sessionsInPeriod)
        assertEquals(30, next.reentry!!.daysSinceLastWorkout)
    }

    @Test fun `the log request names the new fields like the API`() {
        val json = com.google.gson.JsonParser.parseString(gson.toJson(logRequest().copy(performedAt = "2026-11-17T18:30:00Z", clientRequestId = "abc"))).asJsonObject
        assertEquals("2026-11-17T18:30:00Z", json["performedAt"].asString)
        assertEquals("abc", json["clientRequestId"].asString)
        assertFalse(com.google.gson.JsonParser.parseString(gson.toJson(logRequest())).asJsonObject.has("performedAt")) // absent, not null
    }

    @Test fun `a sequence plan is requested without days and a calendar plan without the mode`() {
        val options = sampleOptions()
        val sequence = OnboardingViewModel.buildRequest(OnboardingState(options = options, schedulingMode = "Sequence", sessionsPerPeriod = 5), options)
        val json = com.google.gson.JsonParser.parseString(gson.toJson(sequence)).asJsonObject
        assertFalse(json.has("trainingDays"))
        assertEquals("Sequence", json["schedulingMode"].asString)
        assertEquals(5, json["sessionsPerPeriod"].asInt)
        assertEquals("PushPullLegs", sequence.split) // five sessions per cycle ask for the same split as five days

        val calendar = com.google.gson.JsonParser.parseString(
            gson.toJson(OnboardingViewModel.buildRequest(OnboardingState(options = options, days = setOf("Monday", "Thursday")), options))
        ).asJsonObject
        assertEquals(2, calendar["trainingDays"].asJsonArray.size())
        assertFalse(calendar.has("schedulingMode"))
    }

    @Test fun `the onboarding accepts the sessions per cycle instead of days`() {
        val options = sampleOptions()
        assertFalse(OnboardingState(options = options, schedulingMode = "Sequence", sessionsPerPeriod = 7).daysOk)
        assertTrue(OnboardingState(options = options, schedulingMode = "Sequence", sessionsPerPeriod = 3).daysOk)
        assertFalse(OnboardingState(options = options, schedulingMode = "Calendar").daysOk) // no days chosen
    }

    @Test fun `the copy of a sequence week has no calendar end and only expires with the time limit`() {
        val week = WeekView("p", 1, null, null, "InProgress", null, listOf(sampleSession()))
        cache.saveWeek(week)
        assertNotNull(cache.week())
    }
}
