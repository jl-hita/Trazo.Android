package com.trazo.android.ui

import com.google.gson.Gson
import com.trazo.android.data.model.PlanView
import com.trazo.android.data.model.UpdateMusclePrioritiesRequest
import com.trazo.android.testing.*
import com.trazo.android.ui.onboarding.OnboardingViewModel
import com.trazo.android.ui.plan.MusclePriorities
import com.trazo.android.ui.plan.PlanViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/** Muscle priorities: the rules the screens share, the request the app sends and the plan it shows back. */
class MusclePrioritiesTest : ViewModelTestBase() {
    @Test fun `grow is the default and is never stored`() {
        val chosen = MusclePriorities.with(emptyMap(), "Chest", MusclePriorities.EMPHASIZE)
        assertEquals(mapOf("Chest" to "Emphasize"), chosen)
        assertEquals(emptyMap<String, String>(), MusclePriorities.with(chosen, "Chest", MusclePriorities.GROW))
        assertEquals("Grow", MusclePriorities.of(emptyMap(), "Lats"))
    }

    @Test fun `at most three muscles can be emphasized and a refused emphasis changes nothing`() {
        var map = emptyMap<String, String>()
        listOf("Chest", "Lats", "Biceps").forEach { map = MusclePriorities.with(map, it, MusclePriorities.EMPHASIZE) }
        assertFalse(MusclePriorities.canEmphasize(map, "Triceps"))
        assertEquals(map, MusclePriorities.with(map, "Triceps", MusclePriorities.EMPHASIZE))
        // An emphasized muscle can still be changed, and a freed slot is available again.
        assertTrue(MusclePriorities.canEmphasize(map, "Chest"))
        map = MusclePriorities.with(map, "Chest", MusclePriorities.MAINTAIN)
        assertTrue(MusclePriorities.canEmphasize(map, "Triceps"))
    }

    @Test fun `maintaining does not count against the emphasis limit`() {
        var map = emptyMap<String, String>()
        listOf("Chest", "Lats").forEach { map = MusclePriorities.with(map, it, MusclePriorities.EMPHASIZE) }
        listOf("Calves", "Triceps", "Biceps").forEach { map = MusclePriorities.with(map, it, MusclePriorities.MAINTAIN) }
        assertTrue(MusclePriorities.canEmphasize(map, "Deltoids"))
    }

    @Test fun `a muscle the server does not accept is ignored`() {
        assertEquals(emptyMap<String, String>(), MusclePriorities.with(emptyMap(), "Abdominals", MusclePriorities.EMPHASIZE))
        assertEquals(emptyMap<String, String>(), MusclePriorities.with(emptyMap(), "Chest", "Maximum"))
    }

    @Test fun `priorities are sent only for hypertrophy and only when something is chosen`() {
        val chosen = mapOf("Chest" to "Emphasize", "Triceps" to "Maintain", "Lats" to "Grow", "Abdominals" to "Emphasize")
        assertEquals(mapOf("Chest" to "Emphasize", "Triceps" to "Maintain"), MusclePriorities.forRequest("Hypertrophy", chosen))
        assertNull(MusclePriorities.forRequest("Strength", chosen))
        assertNull(MusclePriorities.forRequest("Hypertrophy", emptyMap()))
    }

    @Test fun `the onboarding sends the chosen priorities and drops them when the goal is not hypertrophy`() = runTest(dispatcher) {
        val api = FakePlansApi()
        val vm = OnboardingViewModel(plansRepo(api))
        advanceUntilIdle()
        vm.toggleDay("Monday"); vm.toggleDay("Thursday")
        vm.setGoal("Hypertrophy")
        vm.setMusclePriority("Deltoids", "Emphasize"); vm.setMusclePriority("Chest", "Maintain")
        vm.submit(); advanceUntilIdle()
        assertEquals(mapOf("Deltoids" to "Emphasize", "Chest" to "Maintain"), api.createdWith!!.musclePriorities)

        vm.setGoal("Strength")
        assertNull(OnboardingViewModel.buildRequest(vm.state.value, vm.state.value.options!!).musclePriorities)
    }

    @Test fun `without choices the request carries no priorities at all`() {
        assertNull(Gson().toJsonTree(com.trazo.android.data.model.PlanProfileRequest("Hypertrophy", "Beginner")).asJsonObject.get("musclePriorities"))
    }

    @Test fun `the plan reads the priorities and an old server without them gives an empty map`() {
        val gson = Gson()
        val with = gson.fromJson(planJson("""{"Triceps":"Maintain"}"""), PlanView::class.java)
        assertEquals(mapOf("Triceps" to "Maintain"), with.priorities)
        val without = gson.fromJson(planJson(null), PlanView::class.java)
        assertTrue(without.priorities.isEmpty())
    }

    @Test fun `the update request has the shape the server expects`() {
        val json = Gson().toJson(UpdateMusclePrioritiesRequest(mapOf("Calves" to "Emphasize")))
        assertEquals("""{"musclePriorities":{"Calves":"Emphasize"}}""", json)
    }

    @Test fun `saving replaces the plan on screen and clearing sends an empty map`() = runTest(dispatcher) {
        val api = FakePlansApi()
        val vm = PlanViewModel(plansRepo(api))
        advanceUntilIdle()
        vm.updateMusclePriorities(mapOf("Triceps" to "Maintain")); advanceUntilIdle()
        assertEquals(mapOf("Triceps" to "Maintain"), vm.state.value.plan!!.priorities)
        assertFalse(vm.state.value.savingPriorities)
        vm.updateMusclePriorities(emptyMap()); advanceUntilIdle()
        assertEquals(listOf(mapOf("Triceps" to "Maintain"), emptyMap<String, String>()), api.prioritiesSent)
        assertTrue(vm.state.value.plan!!.priorities.isEmpty())
    }

    @Test fun `a rejected change keeps the plan and shows the error`() = runTest(dispatcher) {
        val api = FakePlansApi()
        val vm = PlanViewModel(plansRepo(api))
        advanceUntilIdle()
        api.prioritiesResponse = { httpError(400, problem(400, "invalid_request")) }
        vm.updateMusclePriorities(mapOf("Chest" to "Emphasize")); advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.savingPriorities)
        assertTrue(vm.state.value.plan!!.priorities.isEmpty())
    }

    private fun planJson(priorities: String?) = """
        {"id":"p1","status":"Active","timeline":"InProgress","currentWeek":1,"totalWeeks":12,"startDate":"2026-11-16","endDate":"2027-02-07",
         "goal":"Hypertrophy","experience":"Beginner","split":"UpperLower","trainingDays":["Monday"],"maxSessionMinutes":60,"availableEquipment":[],
         "excludedExerciseIds":[],"limitations":[],"currentPhase":1,
         "macrocycle":{"name":"m","notes":null,"phases":[]},"disclaimer":"d"${priorities?.let { ""","musclePriorities":$it""" } ?: ""}}"""
}
