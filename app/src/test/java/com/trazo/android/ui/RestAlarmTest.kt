package com.trazo.android.ui

import com.trazo.android.rest.RestAlarmScheduler
import com.trazo.android.testing.FakePlansApi
import com.trazo.android.data.local.JsonFileStore
import com.trazo.android.data.local.WorkoutDraftStore
import com.trazo.android.ui.workout.FinishForm
import com.trazo.android.ui.workout.WorkoutViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/** Records what the rest timer asks of the alarm: when it is planned and when it is withdrawn. */
private class FakeAlarms : RestAlarmScheduler {
    val events = mutableListOf<String>()
    var at: Long? = null
    override fun schedule(atMillis: Long) { at = atMillis; events += "schedule:$atMillis" }
    override fun cancel() { at = null; events += "cancel" }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RestAlarmTest : ViewModelTestBase() {
    private val alarms = FakeAlarms()
    private var now = 1_000_000L

    private suspend fun vm(): WorkoutViewModel {
        val api = FakePlansApi()
        val repo = plansRepo(api)
        repo.currentWeek()
        val drafts = WorkoutDraftStore(JsonFileStore(File(tmp.root, "drafts")))
        return WorkoutViewModel(repo, drafts, "s1", dispatcher, alarms = alarms, clock = { now })
    }

    private fun WorkoutViewModel.doSet(i: Int) {
        updateSet("db-bench-press", i) { it.copy(reps = "10", rir = "2") }
        toggleDone("db-bench-press", i)
    }

    @Test fun `finishing a set plans the alert for when the rest ends`() = runTest(dispatcher) {
        val vm = vm()
        vm.doSet(0)
        assertEquals(now + 90_000, alarms.at)
    }

    @Test fun `changing the time moves the alert`() = runTest(dispatcher) {
        val vm = vm()
        vm.doSet(0)
        now += 10_000
        vm.adjustRest(15)
        // 90 s remain at the start of the test clock, +15 s from then.
        assertEquals(now + 105_000, alarms.at)
        vm.adjustRest(-15)
        assertEquals(now + 90_000, alarms.at)
    }

    @Test fun `coming back plans the same alert again, so a new permission applies to it`() = runTest(dispatcher) {
        val vm = vm()
        vm.doSet(0)
        val end = now + 90_000
        now += 30_000
        alarms.events.clear()
        vm.replanRestAlert()
        assertEquals(listOf("schedule:$end"), alarms.events)
    }

    @Test fun `coming back after the rest, or after skipping it, plans nothing`() = runTest(dispatcher) {
        val vm = vm()
        vm.replanRestAlert()
        vm.doSet(0)
        now += 90_000
        alarms.events.clear()
        vm.replanRestAlert()
        now -= 80_000
        vm.skipRest()
        alarms.events.clear()
        vm.replanRestAlert()
        assertEquals(emptyList<String>(), alarms.events)
    }

    @Test fun `skipping the rest withdraws the alert`() = runTest(dispatcher) {
        val vm = vm()
        vm.doSet(0)
        vm.skipRest()
        assertNull(alarms.at)
        assertNull(vm.state.value.rest)
    }

    @Test fun `taking the time down to zero withdraws the alert`() = runTest(dispatcher) {
        val vm = vm()
        vm.doSet(0)
        vm.adjustRest(-900)
        assertNull(alarms.at)
    }

    @Test fun `finishing the workout withdraws the alert`() = runTest(dispatcher) {
        val vm = vm()
        repeat(3) { vm.doSet(it) }
        vm.finish(FinishForm(recovery = "Good", sessionRpe = 7.0))
        advanceUntilIdle()
        assertNull(alarms.at)
    }

    @Test fun `leaving the workout withdraws the alert`() = runTest(dispatcher) {
        val vm = vm()
        vm.doSet(0)
        val onCleared = WorkoutViewModel::class.java.getDeclaredMethod("onCleared").apply { isAccessible = true }
        onCleared.invoke(vm)
        assertNull(alarms.at)
    }
}
