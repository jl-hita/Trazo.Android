package com.trazo.android.ui

import android.app.Activity
import com.trazo.android.auth.AuthRepository
import com.trazo.android.data.local.*
import com.trazo.android.data.model.SubscriptionStatus
import com.trazo.android.data.net.SessionEvent
import com.trazo.android.data.net.SessionEvents
import com.trazo.android.data.repo.*
import com.trazo.android.testing.*
import com.trazo.android.ui.account.PaywallViewModel
import com.trazo.android.ui.nav.AppState
import com.trazo.android.ui.nav.AppViewModel
import com.trazo.android.ui.onboarding.OnboardingViewModel
import com.trazo.android.ui.plan.PlanViewModel
import com.trazo.android.ui.workout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTestBase {
    @get:Rule val tmp = TemporaryFolder()
    protected val dispatcher = StandardTestDispatcher()

    @Before fun setMain() { Dispatchers.setMain(dispatcher) }
    @After fun resetMain() { Dispatchers.resetMain() }

    protected fun plansRepo(api: FakePlansApi): PlanRepository = PlanRepository(
        api,
        PlanCache(JsonFileStore(File(tmp.root, "cache")), today = { LocalDate.of(2026, 11, 17) }),
        PendingLogQueue(JsonFileStore(File(tmp.root, "queue")))
    )
}

class WorkoutViewModelTest : ViewModelTestBase() {
    private lateinit var api: FakePlansApi
    private lateinit var repo: PlanRepository
    private lateinit var drafts: WorkoutDraftStore

    private suspend fun vm(): WorkoutViewModel {
        api = FakePlansApi()
        repo = plansRepo(api)
        repo.currentWeek()
        drafts = WorkoutDraftStore(JsonFileStore(File(tmp.root, "drafts")))
        return WorkoutViewModel(repo, drafts, "s1")
    }

    @Test fun `sets start with the target load and a set needs repetitions to be marked`() = runTest(dispatcher) {
        val vm = vm()
        val entry = vm.state.value.entries.single()
        assertEquals(3, entry.sets.size)
        assertEquals("22.5", entry.sets[0].weight)
        vm.toggleDone("db-bench-press", 0)
        assertFalse(vm.state.value.entries.single().sets[0].done) // no reps yet
        vm.updateSet("db-bench-press", 0) { it.copy(reps = "10") }
        vm.toggleDone("db-bench-press", 0)
        assertTrue(vm.state.value.entries.single().sets[0].done)
        assertEquals(1, vm.state.value.doneSets)
    }

    @Test fun `finishing a set starts the rest timer of that exercise and it counts down`() = runTest(dispatcher) {
        val vm = vm()
        vm.updateSet("db-bench-press", 0) { it.copy(reps = "10") }
        vm.toggleDone("db-bench-press", 0)
        assertEquals(90, vm.state.value.rest!!.remainingSeconds)
        advanceTimeBy(3_100)
        assertEquals(87, vm.state.value.rest!!.remainingSeconds)
        vm.adjustRest(15)
        assertEquals(102, vm.state.value.rest!!.remainingSeconds)
        vm.skipRest()
        assertNull(vm.state.value.rest)
    }

    @Test fun `typed sets are kept as a draft and restored`() = runTest(dispatcher) {
        val vm = vm()
        vm.updateSet("db-bench-press", 1) { it.copy(reps = "9", weight = "24") }
        val restored = WorkoutViewModel(repo, drafts, "s1")
        assertEquals("9", restored.state.value.entries.single().sets[1].reps)
        assertEquals("24", restored.state.value.entries.single().sets[1].weight)
    }

    @Test fun `a completed workout builds a Completed log and clears the draft`() = runTest(dispatcher) {
        val vm = vm()
        repeat(3) { i ->
            vm.updateSet("db-bench-press", i) { it.copy(reps = "10", rir = "2") }
            vm.toggleDone("db-bench-press", i)
        }
        vm.finish(FinishForm(recovery = "Good", sessionRpe = 7.0))
        advanceUntilIdle()
        val request = api.logCalls.single().second
        assertEquals("Completed", request.outcome)
        assertEquals("None", request.nonCompletionReason)
        assertEquals("Good", request.recovery)
        assertEquals(3, request.sets.count { it.completed })
        assertEquals(22.5, request.sets[0].weightKg!!, 0.0)
        assertEquals(2.0, request.sets[0].rir!!, 0.0)
        assertTrue((vm.state.value.finish as FinishState.Done).queued.not())
        assertNull(drafts.load("s1"))
    }

    @Test fun `unfinished sets make a partial log with the skip reason`() {
        val entries = listOf(WorkoutViewModel.freshEntry(sampleExercise()).let { e ->
            e.copy(sets = e.sets.mapIndexed { i, s -> if (i == 0) s.copy(reps = "10", done = true) else s })
        })
        val request = WorkoutViewModel.buildLogRequest(entries, FinishForm(reason = "Fatigue"))
        assertEquals("Partial", request.outcome)
        assertEquals("Fatigue", request.nonCompletionReason)
        assertEquals("fatigue", request.sets[1].skippedReason)
        assertFalse(request.sets[1].completed)
        assertTrue(request.sets.all { it.setNumber in 1..12 })
    }

    @Test fun `nothing done is NotCompleted, notes are trimmed and capped`() {
        val entries = listOf(WorkoutViewModel.freshEntry(sampleExercise()))
        val request = WorkoutViewModel.buildLogRequest(entries, FinishForm(notes = "  " + "x".repeat(1200) + " ", reason = "Time"))
        assertEquals("NotCompleted", request.outcome)
        assertEquals(1000, request.notes!!.length)
        assertEquals("time", request.sets[0].skippedReason)
    }

    @Test fun `numbers accept commas and reject out of range values`() {
        assertEquals(22.5, WorkoutViewModel.parseWeight("22,5")!!, 0.0)
        assertNull(WorkoutViewModel.parseWeight("1001"))
        assertNull(WorkoutViewModel.parseReps("101"))
        assertNull(WorkoutViewModel.parseReps(""))
        assertEquals(2.0, WorkoutViewModel.parseRir("2")!!, 0.0)
        assertNull(WorkoutViewModel.parseRir("11"))
    }

    @Test fun `without connection the finished workout is queued, not lost`() = runTest(dispatcher) {
        val vm = vm()
        api.logResponse = { _, _ -> networkFailure() }
        vm.updateSet("db-bench-press", 0) { it.copy(reps = "10") }
        vm.toggleDone("db-bench-press", 0)
        vm.finish(FinishForm())
        advanceUntilIdle()
        assertTrue((vm.state.value.finish as FinishState.Done).queued)
        assertEquals(1, repo.pendingCount.value)
    }

    @Test fun `a rejected log shows the error and can be retried`() = runTest(dispatcher) {
        val vm = vm()
        api.logResponse = { _, _ -> httpError(409, problem(409, "session_out_of_window")) }
        vm.finish(FinishForm())
        advanceUntilIdle()
        assertTrue(vm.state.value.finish is FinishState.Failed)
        vm.dismissFinishError()
        assertEquals(FinishState.Idle, vm.state.value.finish)
    }
}

class OnboardingViewModelTest : ViewModelTestBase() {
    @Test fun `days must be inside the limits the server allows and the request is built from the options`() = runTest(dispatcher) {
        val api = FakePlansApi()
        val vm = OnboardingViewModel(plansRepo(api))
        advanceUntilIdle()
        assertFalse(vm.state.value.canSubmit)
        vm.toggleDay("Friday"); vm.toggleDay("Monday")
        vm.setGoal("Strength"); vm.toggleLimitation("Knee"); vm.toggleEquipment("dumbbell")
        assertTrue(vm.state.value.canSubmit)
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.state.value.created)
        val sent = api.createdWith!!
        assertEquals(listOf("Monday", "Friday"), sent.trainingDays) // week order, not tap order
        assertEquals("Strength", sent.goal)
        assertEquals(listOf("Knee"), sent.limitations)
        assertEquals(listOf("dumbbell"), sent.availableEquipment)
        assertEquals("Automatic", sent.split)
    }

    @Test fun `five or more days use push pull legs because automatic only covers 2 to 4`() = runTest(dispatcher) {
        val vm = OnboardingViewModel(plansRepo(FakePlansApi()))
        advanceUntilIdle()
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday").forEach(vm::toggleDay)
        assertEquals("PushPullLegs", OnboardingViewModel.buildRequest(vm.state.value, vm.state.value.options!!).split)
    }

    @Test fun `server rejection is shown and does not mark the plan as created`() = runTest(dispatcher) {
        val api = FakePlansApi().apply { createResponse = { httpError(409, problem(409, "active_plan_exists")) } }
        val vm = OnboardingViewModel(plansRepo(api))
        advanceUntilIdle()
        vm.toggleDay("Monday"); vm.toggleDay("Thursday")
        vm.submit()
        advanceUntilIdle()
        assertFalse(vm.state.value.created)
        assertEquals("active_plan_exists", vm.state.value.error!!.code)
    }
}

class PlanViewModelTest : ViewModelTestBase() {
    @Test fun `loads the current week and the plan summary`() = runTest(dispatcher) {
        val vm = PlanViewModel(plansRepo(FakePlansApi()))
        advanceUntilIdle()
        assertEquals(1, vm.state.value.week!!.sessions.size)
        assertEquals(12, vm.state.value.plan!!.totalWeeks)
        assertFalse(vm.state.value.needsOnboarding)
    }

    @Test fun `no plan sends the user to onboarding`() = runTest(dispatcher) {
        val api = FakePlansApi().apply { weekResponse = { httpError(404, problem(404, "no_active_plan")) } }
        val vm = PlanViewModel(plansRepo(api))
        advanceUntilIdle()
        assertTrue(vm.state.value.needsOnboarding)
        assertNull(vm.state.value.error)
    }

    @Test fun `offline without a copy shows an error and offers retry`() = runTest(dispatcher) {
        val api = FakePlansApi().apply { weekResponse = { networkFailure() } }
        val vm = PlanViewModel(plansRepo(api))
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        api.weekResponse = { Response.success(sampleWeek()) }
        vm.load(); advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertNotNull(vm.state.value.week)
    }
}

class AppViewModelTest : ViewModelTestBase() {
    private fun app(
        store: MemoryTokenStore, subs: FakeSubscriptionApi, events: SessionEvents = SessionEvents(), authApi: FakeAuthApi = FakeAuthApi(),
        cleared: () -> Unit = {}
    ) = AppViewModel(AuthRepository(authApi, store), SubscriptionRepository(subs), events, { store.clear() }, cleared)

    @Test fun `no stored session shows sign in`() = runTest(dispatcher) {
        val vm = app(MemoryTokenStore(), FakeSubscriptionApi())
        advanceUntilIdle()
        assertEquals(AppState.SignedOut, vm.state.value)
    }

    @Test fun `while the backend says the app is free the plans screen never shows`() = runTest(dispatcher) {
        val vm = app(MemoryTokenStore("a", "r"), FakeSubscriptionApi())
        advanceUntilIdle()
        assertTrue(vm.state.value is AppState.Ready)
    }

    @Test fun `enforced without access shows the plans screen, with access it does not`() = runTest(dispatcher) {
        val subs = FakeSubscriptionApi({ Response.success(SubscriptionStatus(hasAccess = false, enforced = true, status = "expired")) })
        val vm = app(MemoryTokenStore("a", "r"), subs)
        advanceUntilIdle()
        assertTrue(vm.state.value is AppState.Paywall)
        subs.status = { Response.success(SubscriptionStatus(hasAccess = true, enforced = true, status = "active")) }
        vm.refreshAccess(); advanceUntilIdle()
        assertTrue(vm.state.value is AppState.Ready)
    }

    @Test fun `a 402 event brings up the plans screen only if the backend confirms`() = runTest(dispatcher) {
        val events = SessionEvents()
        val subs = FakeSubscriptionApi()
        val vm = app(MemoryTokenStore("a", "r"), subs, events)
        advanceUntilIdle()
        subs.status = { Response.success(SubscriptionStatus(hasAccess = false, enforced = true)) }
        events.emit(SessionEvent.SubscriptionRequired); advanceUntilIdle()
        assertTrue(vm.state.value is AppState.Paywall)
    }

    @Test fun `an expired session clears local data and goes back to sign in`() = runTest(dispatcher) {
        val events = SessionEvents()
        var cleared = false
        val vm = app(MemoryTokenStore("a", "r"), FakeSubscriptionApi(), events, cleared = { cleared = true })
        advanceUntilIdle()
        events.emit(SessionEvent.SessionExpired); advanceUntilIdle()
        assertEquals(AppState.SignedOut, vm.state.value)
        assertTrue(cleared)
    }

    @Test fun `refresh refused at start goes to sign in, offline keeps the session`() = runTest(dispatcher) {
        val refused = MemoryTokenStore("a", "r")
        val vm1 = app(refused, FakeSubscriptionApi(), authApi = FakeAuthApi { httpError(401) })
        advanceUntilIdle()
        assertEquals(AppState.SignedOut, vm1.state.value)

        val kept = MemoryTokenStore("a", "r")
        val vm2 = app(kept, FakeSubscriptionApi(), authApi = FakeAuthApi { networkFailure() })
        advanceUntilIdle()
        assertTrue(vm2.state.value is AppState.Ready)
        assertEquals("r", kept.refresh)
    }

    @Test fun `signing out ends in sign in`() = runTest(dispatcher) {
        val store = MemoryTokenStore("a", "r")
        val vm = app(store, FakeSubscriptionApi())
        advanceUntilIdle()
        vm.signOut(); advanceUntilIdle()
        assertEquals(AppState.SignedOut, vm.state.value)
        assertNull(store.refresh)
    }
}

class PaywallViewModelTest : ViewModelTestBase() {
    private class FakeBilling(var offer: BillingOffer? = BillingOffer("trazo_premium", "TRAZO", "4,99 €", "P1M", true)) : BillingRepository {
        val flow = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 4)
        override val results: SharedFlow<PurchaseResult> = flow
        var launched: String? = null
        var restoreResult: PurchaseResult? = null
        override suspend fun loadOffer() = offer
        override suspend fun launch(activity: Activity, userId: String): Boolean { launched = userId; return true }
        override suspend fun restore() = restoreResult
        override fun close() {}
    }

    @Test fun `shows the offer and reports access once the backend accepts the purchase`() = runTest(dispatcher) {
        val billing = FakeBilling()
        var granted: SubscriptionStatus? = null
        val vm = PaywallViewModel(billing, "user-1", { granted = it })
        advanceUntilIdle()
        assertEquals("4,99 €", vm.state.value.offer!!.formattedPrice)
        billing.flow.emit(PurchaseResult.Verified(SubscriptionStatus(true, true, "active")))
        advanceUntilIdle()
        assertEquals("active", granted!!.status)
    }

    @Test fun `a product Google does not offer is reported, a pending purchase waits`() = runTest(dispatcher) {
        val billing = FakeBilling(offer = null)
        val vm = PaywallViewModel(billing, "user-1", {})
        advanceUntilIdle()
        assertTrue(vm.state.value.unavailable)
        billing.flow.emit(PurchaseResult.Pending); advanceUntilIdle()
        assertTrue(vm.state.value.pending)
    }

    @Test fun `an earlier purchase found on start is verified without asking again`() = runTest(dispatcher) {
        val billing = FakeBilling().apply { restoreResult = PurchaseResult.Verified(SubscriptionStatus(true, true, "active")) }
        var granted = false
        PaywallViewModel(billing, "user-1", { granted = true })
        advanceUntilIdle()
        assertTrue(granted)
    }
}

class AccountReviewViewModelTest : ViewModelTestBase() {
    @Test fun `sending a message refreshes the conversation when the reply carries none`() = runTest(dispatcher) {
        val api = FakePlansApi()
        val vm = com.trazo.android.ui.account.AccountViewModel(SubscriptionRepository(FakeSubscriptionApi()), plansRepo(api))
        advanceUntilIdle()
        api.sendWithoutMessages = true
        vm.setDraft("hola")
        api.messagesResponse = { Response.success(com.trazo.android.data.model.ReviewRequestView("r1", "in_progress", "2026-11-14T10:00:00Z", messages = listOf(com.trazo.android.data.model.ReviewMessage("m1", "user", "hola", "2026-11-14T10:01:00Z")))) }
        vm.sendMessage()
        advanceUntilIdle()
        assertEquals("", vm.state.value.draft)
        assertEquals(listOf("hola"), vm.state.value.thread.map { it.body })
    }
}
