package com.trazo.android.ui

import com.trazo.android.data.model.PlanView
import com.trazo.android.testing.*
import com.trazo.android.ui.plan.PlanViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** An injury outside a session: the areas go to the plan, the week is reloaded and a refusal is shown without losing the plan. */
class ReportInjuryTest : ViewModelTestBase() {
    @Test fun `reporting an area sends it, keeps it in the plan and reloads the week`() = runTest(dispatcher) {
        val api = FakePlansApi()
        val vm = PlanViewModel(plansRepo(api))
        advanceUntilIdle()
        vm.updateLimitations(listOf("Knee", "Elbow")); advanceUntilIdle()
        assertEquals(listOf(listOf("Knee", "Elbow")), api.limitationsSent)
        assertEquals(listOf("Knee", "Elbow"), vm.state.value.plan!!.limitations)
        assertFalse(vm.state.value.savingLimitations)
        assertNull(vm.state.value.error)
    }

    @Test fun `recovering sends an empty list`() = runTest(dispatcher) {
        val api = FakePlansApi()
        val vm = PlanViewModel(plansRepo(api))
        advanceUntilIdle()
        vm.updateLimitations(listOf("Knee")); advanceUntilIdle()
        vm.updateLimitations(emptyList()); advanceUntilIdle()
        assertEquals(listOf(listOf("Knee"), emptyList<String>()), api.limitationsSent)
        assertTrue(vm.state.value.plan!!.limitations.isEmpty())
    }

    @Test fun `without Premium the refusal is shown and the plan is untouched`() = runTest(dispatcher) {
        val api = FakePlansApi()
        val vm = PlanViewModel(plansRepo(api))
        advanceUntilIdle()
        api.limitationsResponse = { httpError(402, problem(402, "premium_required")) }
        vm.updateLimitations(listOf("Shoulder")); advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.savingLimitations)
        assertTrue(vm.state.value.plan!!.limitations.isEmpty())
    }
}
