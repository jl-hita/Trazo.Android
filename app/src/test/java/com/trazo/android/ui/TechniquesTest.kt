package com.trazo.android.ui

import com.trazo.android.data.local.JsonFileStore
import com.trazo.android.data.local.WorkoutDraftStore
import com.trazo.android.data.model.*
import com.trazo.android.data.repo.ExerciseRepository
import com.trazo.android.testing.*
import com.trazo.android.ui.manual.ManualPlanViewModel
import com.trazo.android.ui.workout.FinishForm
import com.trazo.android.ui.workout.WorkoutViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response
import java.io.File

/** Set techniques: which sets carry them, how a workout goes through their segments and what a manual plan sends. */
class TechniquesTest : ViewModelTestBase() {
    private val drop = Techniques.defaults(Techniques.DROP_SET).copy(maxSegments = 2)
    private val myo = Techniques.defaults(Techniques.MYO_REPS)
    private val match = Techniques.defaults(Techniques.MATCH)
    private val id = "db-bench-press"

    @Test fun `each technique is carried by its sets`() {
        assertEquals(listOf(false, false, true), (0..2).map { Techniques.carries(drop, it, 3) })
        assertEquals(listOf(true, true, true), (0..2).map { Techniques.carries(drop.copy(appliesTo = "AllSets"), it, 3) })
        assertEquals(listOf(false, true, true), (0..2).map { Techniques.carries(match, it, 3) })
        assertEquals(listOf(false, false, true), (0..2).map { Techniques.carries(match.copy(appliesTo = "LastSet"), it, 3) })
        assertFalse(Techniques.carries(match.copy(appliesTo = "LastSet"), 0, 1))
    }

    @Test fun `a technique is over at its maximum, at a short segment or when the match is reached`() {
        assertTrue(Techniques.wantsAnother(drop, null, 10, listOf(6)))
        assertFalse(Techniques.wantsAnother(drop, null, 10, listOf(6, 4)))
        assertTrue(Techniques.wantsAnother(myo, null, 15, listOf(4, 3)))
        assertFalse(Techniques.wantsAnother(myo, null, 15, listOf(4, 2)))
        assertTrue(Techniques.wantsAnother(match, 12, 8, listOf(3)))
        assertFalse(Techniques.wantsAnother(match, 12, 8, listOf(3, 1)))
        assertFalse(Techniques.wantsAnother(match, 12, 12, emptyList()))
    }

    @Test fun `a drop lowers the load to half a kilo and the others keep it`() {
        assertEquals(18.0, Techniques.segmentLoad(drop, 22.5)!!, 0.0)
        assertEquals(22.5, Techniques.segmentLoad(myo, 22.5)!!, 0.0)
        assertNull(Techniques.segmentLoad(drop, null))
    }

    private suspend fun workout(technique: TechniqueView?, supersets: List<SupersetView>? = null, vararg exercises: ExerciseView): Triple<WorkoutViewModel, WorkoutDraftStore, com.trazo.android.data.repo.PlanRepository> {
        val api = FakePlansApi()
        val list = if (exercises.isEmpty()) arrayOf(sampleExercise().copy(technique = technique)) else exercises
        api.weekResponse = { Response.success(sampleWeek().copy(sessions = listOf(sampleSession("s1", *list).copy(supersets = supersets)))) }
        val repo = plansRepo(api)
        repo.currentWeek()
        val drafts = WorkoutDraftStore(JsonFileStore(File(tmp.root, "drafts")))
        return Triple(WorkoutViewModel(repo, drafts, "s1", dispatcher), drafts, repo)
    }

    private fun WorkoutViewModel.set(index: Int) = state.value.entries.first().sets[index]

    private fun WorkoutViewModel.complete(index: Int, reps: String) {
        updateSet(id, index) { it.copy(reps = reps) }
        toggleDone(id, index)
    }

    private fun WorkoutViewModel.completeSegment(index: Int, segment: Int, reps: String) {
        updateSegment(id, index, segment) { it.copy(reps = reps) }
        toggleSegment(id, index, segment)
    }

    @Test fun `a drop set offers each drop after a short rest and ends with the normal one`() = runTest(dispatcher) {
        val (vm, _, _) = workout(drop)
        vm.complete(0, "10")
        assertTrue(vm.set(0).segments.isEmpty())
        assertEquals(90, vm.state.value.rest!!.totalSeconds)

        vm.complete(2, "10")
        assertEquals("18", vm.set(2).segments.single().weight)
        assertEquals(10, vm.state.value.rest!!.totalSeconds)
        vm.completeSegment(2, 0, "6")
        assertEquals("14.5", vm.set(2).segments[1].weight)
        vm.completeSegment(2, 1, "4")
        assertEquals(2, vm.set(2).segments.size)
        assertEquals(90, vm.state.value.rest!!.totalSeconds)
        vm.skipRest()
    }

    @Test fun `the user can stop early or add a part after a short one`() = runTest(dispatcher) {
        val (vm, _, _) = workout(myo)
        vm.complete(2, "15")
        vm.completeSegment(2, 0, "2") // short: myo-reps are over
        assertEquals(1, vm.set(2).segments.size)
        assertEquals(90, vm.state.value.rest!!.totalSeconds)
        vm.addSegment(id, 2)
        assertEquals(2, vm.set(2).segments.size)
        vm.finishSet(id, 2)
        assertEquals(1, vm.set(2).segments.size)
        vm.skipRest()
    }

    @Test fun `segments are kept in the draft and sent after their set, without gaps`() = runTest(dispatcher) {
        val (vm, drafts, repo) = workout(drop)
        vm.complete(2, "10")
        vm.completeSegment(2, 0, "6")
        vm.skipRest()
        advanceUntilIdle()

        val restored = WorkoutViewModel(repo, drafts, "s1", dispatcher)
        assertEquals(listOf("6", ""), restored.set(2).segments.map { it.reps })
        assertTrue(restored.set(2).segments[0].done)

        val request = WorkoutViewModel.buildLogRequest(restored.state.value.entries, FinishForm())
        assertEquals(listOf(1 to null, 2 to null, 3 to null, 3 to 1), request.sets.map { it.setNumber to it.segment })
        assertEquals(6, request.sets.last().repetitions)
        assertNull(request.sets.last().skippedReason)
        // A queued log edited again comes back with its segments.
        assertEquals(listOf(0, 0, 0, 1), WorkoutViewModel.draftOf(request).map { it.segment })

        // A set unmarked takes its segments out of the log.
        restored.toggleDone(id, 2)
        assertTrue(WorkoutViewModel.buildLogRequest(restored.state.value.entries, FinishForm()).sets.all { it.segment == null })
    }

    @Test fun `a superset with rounds rests between its exercises and after the round`() = runTest(dispatcher) {
        val a = sampleExercise("a").copy(supersetGroup = "A")
        val b = sampleExercise("b").copy(order = 2, supersetGroup = "A")
        val (vm, _, _) = workout(null, listOf(SupersetView("A", 3, 15, 120)), a, b)
        vm.updateSet("a", 0) { it.copy(reps = "10") }; vm.toggleDone("a", 0)
        assertEquals(15, vm.state.value.rest!!.totalSeconds)
        vm.updateSet("b", 0) { it.copy(reps = "10") }; vm.toggleDone("b", 0)
        assertEquals(120, vm.state.value.rest!!.totalSeconds)
        vm.skipRest()
    }

    @Test fun `the editor keeps techniques and rounds, and joining exercises takes the technique away`() {
        val a = sampleExercise("a").copy(supersetGroup = "A")
        val b = sampleExercise("b").copy(order = 2, supersetGroup = "A")
        val c = sampleExercise("c").copy(order = 3, technique = drop)
        val session = sampleSession("s1", a, b, c).copy(supersets = listOf(SupersetView("A", 4, 20, 100)))
        val day = ManualPlanViewModel.routinesOf(listOf(session)).getValue("Monday")
        val state = com.trazo.android.ui.manual.ManualPlanState(name = "Plan", days = mapOf("Monday" to day))
        val sent = ManualPlanViewModel.buildRequest(state, listOf("Monday")).days.single()
        assertEquals(listOf(null, null, drop), sent.exercises.map { it.technique })
        // The rounds follow the sets of its exercises.
        assertEquals(listOf(SupersetView("A", 3, 20, 100)), sent.supersets)
        val plain = ManualPlanViewModel.buildRequest(state.copy(days = mapOf("Monday" to day.copy(exercises = day.exercises.map { it.copy(superset = null) }))), listOf("Monday"))
        assertNull(plain.days.single().supersets)
    }

    @Test fun `the editor sets the rounds and rests of a superset of up to four`() = runTest(dispatcher) {
        val vm = ManualPlanViewModel(plansRepo(FakePlansApi()), ExerciseRepository(FakeExercisesApi()))
        vm.toggleDay("Monday", "A")
        val item = FakeExercisesApi().let { vm.openPicker("Monday"); advanceUntilIdle(); vm.state.value.results.first() }
        listOf("a", "b", "c", "d", "e").forEach { name -> vm.openPicker("Monday"); vm.pick(item.copy(id = name), name) }
        listOf("a", "b", "c", "d").forEach { vm.toggleLink("Monday", it) }
        fun exercises() = vm.state.value.days.getValue("Monday").exercises
        assertEquals(listOf("A", "A", "A", "A", null), ManualPlanViewModel.supersetGroups(exercises())) // a fifth is refused

        fun sent() = ManualPlanViewModel.buildRequest(vm.state.value.copy(name = "Plan"), listOf("Monday")).days.single()
        assertNull(sent().supersets) // untouched: done as supersets always were
        vm.changeRounds("Monday", "a", 1)
        assertEquals(listOf(4, 4, 4, 4, 3), exercises().map { it.sets })
        assertNull(sent().supersets)
        vm.changeSupersetRest("Monday", "a", false, 15)
        vm.changeSupersetRest("Monday", "a", true, 30)
        assertEquals(listOf(SupersetView("A", 4, 15, 120)), sent().supersets)
        vm.changeSupersetRest("Monday", "a", false, -60)
        assertEquals(0, sent().supersets!!.single().restBetweenSeconds)
        advanceUntilIdle()
    }

    @Test fun `a session of a generated plan is edited on its own or for later too`() = runTest(dispatcher) {
        val api = FakePlansApi()
        val a = sampleExercise("a")
        val b = sampleExercise("b").copy(order = 2, technique = drop.copy(reason = "ShortOnTime"))
        api.weekResponse = { Response.success(sampleWeek().copy(sessions = listOf(sampleSession("s1", a, b)))) }
        val repo = plansRepo(api)
        repo.currentWeek()
        val vm = ManualPlanViewModel(repo, ExerciseRepository(FakeExercisesApi()), sessionId = "s1")
        advanceUntilIdle()
        assertTrue(vm.state.value.canSubmit)

        vm.removeExercise(ManualPlanViewModel.SESSION_KEY, "a")
        vm.setKeepForLater(true)
        vm.submit(emptyList())
        advanceUntilIdle()
        val (id, sent) = api.sessionEdits.single()
        assertEquals("s1", id)
        assertEquals("plan", sent.scope)
        assertEquals(listOf("b"), sent.exercises.map { it.exerciseId })
        assertEquals("DropSet", sent.exercises.single().technique!!.type)
        assertTrue(vm.state.value.created)

        vm.forgetChanges()
        advanceUntilIdle()
        assertEquals(1, api.adjustmentsCleared)
        assertTrue(vm.state.value.forgotten)
    }

    @Test fun `the technique dialog offers what the catalog admits`() = runTest(dispatcher) {
        val exercises = FakeExercisesApi()
        exercises.detailResponse = Response.success(exercises.detailResponse.body()!!.copy(allowedTechniques = listOf("DropSet", "MyoRepMatch")))
        val vm = ManualPlanViewModel(plansRepo(FakePlansApi()), ExerciseRepository(exercises))
        vm.toggleDay("Monday", "A"); vm.openPicker("Monday"); advanceUntilIdle()
        vm.pick(vm.state.value.results.first(), "Press")
        vm.openTechnique("Monday", id); advanceUntilIdle()
        assertEquals(listOf("DropSet", "MyoRepMatch"), vm.state.value.technique!!.allowed)

        fun chosen() = vm.state.value.days.getValue("Monday").exercises.single().technique
        vm.setTechnique("DropSet")
        vm.changeDrops(5)
        assertEquals(3, chosen()!!.maxSegments)
        vm.setTechniqueScope("AllSets")
        assertEquals("AllSets", chosen()!!.appliesTo)
        vm.setTechnique("MyoRepMatch")
        assertEquals("MyoRepMatch", chosen()!!.type)
        repeat(3) { vm.changeSets("Monday", id, -1) }
        assertNull(chosen()) // a match needs two sets
        vm.setTechnique("MyoRepMatch")
        assertNull(chosen())
        vm.setTechnique("DropSet"); vm.setTechnique(null)
        assertNull(chosen())
        vm.closeTechnique()
        assertNull(vm.state.value.technique)
    }
}
