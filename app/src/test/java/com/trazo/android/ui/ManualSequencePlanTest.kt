package com.trazo.android.ui

import com.trazo.android.data.model.*
import com.trazo.android.data.repo.ExerciseRepository
import com.trazo.android.testing.*
import com.trazo.android.ui.manual.ManualPlanViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

/** A manual plan in sequence mode: routines that rotate in order, with no weekdays. */
class ManualSequencePlanTest : ViewModelTestBase() {
    private val week = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    private fun announcing(vararg modes: String) = FakePlansApi().apply {
        optionsResponse = { Response.success(sampleOptions().copy(schedulingModes = modes.toList())) }
    }

    private fun vm(api: FakePlansApi, editing: Boolean = false) = ManualPlanViewModel(plansRepo(api), ExerciseRepository(FakeExercisesApi()), editing)

    private suspend fun kotlinx.coroutines.test.TestScope.fill(vm: ManualPlanViewModel, key: String) {
        vm.openPicker(key); advanceUntilIdle()
        vm.pick(vm.state.value.results.first(), "Ejercicio")
    }

    @Test fun `the sequence mode is offered only when the server announces it`() = runTest(dispatcher) {
        val old = vm(announcing("Calendar")); advanceUntilIdle()
        assertFalse(old.state.value.sequenceAvailable)
        old.setSchedulingMode("Sequence")
        assertEquals("Calendar", old.state.value.schedulingMode) // refused: the server would not understand it

        val current = vm(announcing("Calendar", "Sequence")); advanceUntilIdle()
        assertTrue(current.state.value.sequenceAvailable)
        current.setSchedulingMode("Sequence")
        assertTrue(current.state.value.isSequence)
    }

    @Test fun `routines are added in order, moved and removed, up to seven`() = runTest(dispatcher) {
        val vm = vm(announcing("Calendar", "Sequence")); advanceUntilIdle()
        vm.setSchedulingMode("Sequence")
        repeat(9) { vm.addRoutine("R${it + 1}") }
        assertEquals(7, vm.state.value.days.size)
        assertEquals(listOf("R1", "R2", "R3", "R4", "R5", "R6", "R7"), vm.state.value.let { st -> st.order.map { k -> st.days.getValue(k).title } })

        vm.moveRoutine("r1", 1)
        assertEquals(listOf("R2", "R1", "R3"), vm.state.value.let { st -> st.order.map { k -> st.days.getValue(k).title } }.take(3))
        vm.moveRoutine("r2", -1) // already first
        vm.moveRoutine("r2", -1)
        assertEquals("r2", vm.state.value.order.first())
        vm.removeRoutine("r3")
        vm.addRoutine("nuevo")
        assertFalse(vm.state.value.order.contains("r3")) // a removed key is not reused
        assertTrue(vm.state.value.order.contains("r8"))
    }

    @Test fun `the request has the routines in order, no weekdays and the mode`() = runTest(dispatcher) {
        val api = announcing("Calendar", "Sequence")
        val vm = vm(api); advanceUntilIdle()
        vm.setName("Rotación"); vm.setSchedulingMode("Sequence")
        vm.addRoutine("Empuje"); vm.addRoutine("")
        fill(vm, "r1"); fill(vm, "r2")
        vm.moveRoutine("r2", -1)
        vm.submit(week); advanceUntilIdle()

        assertTrue(vm.state.value.created)
        val sent = api.manualCreatedWith!!
        assertEquals("Sequence", sent.schedulingMode)
        assertEquals(listOf(null, null), sent.days.map { it.day })
        assertEquals(listOf("R1", "Empuje"), sent.days.map { it.title }) // the blank one falls back to its place
        val json = com.google.gson.Gson().toJson(sent)
        assertFalse(json.contains("\"day\"")) // absent, not null
    }

    @Test fun `a calendar plan is sent as before, without the mode`() = runTest(dispatcher) {
        val api = announcing("Calendar", "Sequence")
        val vm = vm(api); advanceUntilIdle()
        vm.setName("Plan"); vm.toggleDay("Monday", "A"); fill(vm, "Monday")
        vm.submit(week); advanceUntilIdle()
        val sent = api.manualCreatedWith!!
        assertNull(sent.schedulingMode)
        assertEquals(listOf("Monday"), sent.days.map { it.day })
    }

    @Test fun `switching the mode starts the routines again, and an existing plan keeps its mode`() = runTest(dispatcher) {
        val vm = vm(announcing("Calendar", "Sequence")); advanceUntilIdle()
        vm.toggleDay("Monday", "A")
        vm.setSchedulingMode("Sequence")
        assertTrue(vm.state.value.days.isEmpty())

        val api = announcing("Calendar", "Sequence")
        api.planResponse = { Response.success(samplePlan().copy(schedulingMode = "Sequence", sessionsPerPeriod = 2)) }
        api.weekResponse = {
            Response.success(sampleWeek(
                sampleSession("b").copy(date = null, day = null, title = "Pierna", sequenceIndex = 2),
                sampleSession("a").copy(date = null, day = null, title = "Empuje", sequenceIndex = 1)
            ))
        }
        val editing = vm(api, editing = true); advanceUntilIdle()
        assertTrue(editing.state.value.isSequence)
        assertEquals(listOf("Empuje", "Pierna"), editing.state.value.let { st -> st.order.map { k -> st.days.getValue(k).title } }) // program order
        editing.setSchedulingMode("Calendar")
        assertTrue(editing.state.value.isSequence) // not changeable once the plan exists
        assertEquals(3, editing.state.value.nextRoutine)
    }

    @Test fun `routines of a sequence plan are read in program order and of a calendar plan by weekday`() {
        val a = sampleSession("a").copy(date = null, day = null, title = "A", sequenceIndex = 5)
        val b = sampleSession("b").copy(date = null, day = null, title = "B", sequenceIndex = 4)
        assertEquals(listOf("B", "A"), ManualPlanViewModel.routinesOf(listOf(a, b), sequence = true).values.map { it.title })
        assertEquals(setOf("Monday"), ManualPlanViewModel.routinesOf(listOf(sampleSession("m"), a, b)).keys)
    }
}
