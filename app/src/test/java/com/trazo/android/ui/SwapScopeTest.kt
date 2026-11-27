package com.trazo.android.ui

import com.google.gson.Gson
import com.trazo.android.data.api.PlansApi
import com.trazo.android.data.local.JsonFileStore
import com.trazo.android.data.local.WorkoutDraftStore
import com.trazo.android.data.model.SwapRequest
import com.trazo.android.data.net.NetworkModule
import com.trazo.android.data.net.SessionEvents
import com.trazo.android.data.net.apiCall
import com.trazo.android.data.repo.ExerciseRepository
import com.trazo.android.testing.*
import com.trazo.android.ui.plan.SessionViewModel
import com.trazo.android.ui.workout.WorkoutViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** The scope of a swap, from the screens' state to the request: what is chosen is what is sent, once. */
@OptIn(ExperimentalCoroutinesApi::class)
class SwapScopeTest : ViewModelTestBase() {
    private val api = FakePlansApi()

    private suspend fun workout(): WorkoutViewModel {
        val repo = plansRepo(api)
        repo.currentWeek()
        return WorkoutViewModel(repo, WorkoutDraftStore(JsonFileStore(File(tmp.root, "drafts"))), "s1", dispatcher)
    }

    private suspend fun session(): SessionViewModel {
        val repo = plansRepo(api)
        repo.currentWeek()
        return SessionViewModel(repo, ExerciseRepository(FakeExercisesApi()), "s1")
    }

    private fun request(remainingBlock: Boolean) = Triple("s1", "db-bench-press", SwapRequest("other", remainingBlock))

    @Test fun `opening and closing the alternatives sends nothing`() = runTest(dispatcher) {
        val vm = workout()
        vm.showAlternatives("db-bench-press"); advanceUntilIdle()
        vm.dismissAlternatives(); advanceUntilIdle()
        assertNull(vm.state.value.alternatives)
        assertTrue(api.swaps.isEmpty())
    }

    @Test fun `a swap from the workout for this session sends that scope and does not read the week again`() = runTest(dispatcher) {
        val vm = workout()
        val weekReads = api.weekCalls
        vm.showAlternatives("db-bench-press"); advanceUntilIdle()
        vm.swap("db-bench-press", "other", false); advanceUntilIdle()
        assertEquals(listOf(request(false)), api.swaps)
        assertEquals(weekReads, api.weekCalls)
        assertNull(vm.state.value.alternatives)
    }

    @Test fun `a swap from the workout for the block sends that scope and reads the week again`() = runTest(dispatcher) {
        val vm = workout()
        val weekReads = api.weekCalls
        vm.showAlternatives("db-bench-press"); advanceUntilIdle()
        vm.swap("db-bench-press", "other", true); advanceUntilIdle()
        assertEquals(listOf(request(true)), api.swaps)
        assertEquals(weekReads + 1, api.weekCalls)
    }

    @Test fun `a swap from the session screen sends the chosen scope`() = runTest(dispatcher) {
        for (block in listOf(false, true)) {
            api.swaps.clear()
            val vm = session()
            vm.showAlternatives("db-bench-press"); advanceUntilIdle()
            vm.swap("db-bench-press", "other", block); advanceUntilIdle()
            assertEquals(listOf(request(block)), api.swaps)
            assertNull(vm.state.value.alternatives)
        }
    }

    @Test fun `a second choice while the first is being sent is ignored`() = runTest(dispatcher) {
        val workout = workout()
        val session = session()
        for (swap in listOf<(Boolean) -> Unit>(
            { workout.swap("db-bench-press", "other", it) }, { session.swap("db-bench-press", "other", it) }
        )) {
            api.swaps.clear()
            workout.showAlternatives("db-bench-press"); session.showAlternatives("db-bench-press"); advanceUntilIdle()
            swap(false); swap(true); advanceUntilIdle()
            assertEquals(listOf(request(false)), api.swaps)
        }
    }

    @Test fun `a rejected swap keeps the choice open and can be sent again`() = runTest(dispatcher) {
        api.swapResponse = { httpError(409, problem(409, "plan_modified_concurrently")) }
        val vm = session()
        vm.showAlternatives("db-bench-press"); advanceUntilIdle()
        vm.swap("db-bench-press", "other", true); advanceUntilIdle()
        assertNotNull(vm.state.value.swapError)
        assertFalse(vm.state.value.alternatives!!.swapping)
        api.swapResponse = { retrofit2.Response.success(sampleSession()) }
        vm.swap("db-bench-press", "other", true); advanceUntilIdle()
        assertEquals(2, api.swaps.size)
        assertNull(vm.state.value.alternatives)
    }

    @Test fun `the request carries the scope in the parameter of the endpoint`() = runBlocking {
        MockWebServer().use { server ->
            val plans = NetworkModule(server.url("/").toString(), MemoryTokenStore("a", "r"), SessionEvents(), Gson()).create(PlansApi::class.java)
            for (block in listOf(false, true)) {
                server.enqueue(MockResponse().setBody(Gson().toJson(sampleSession())))
                apiCall { plans.swap("s1", "db-bench-press", SwapRequest("other", block)) }
                val sent = server.takeRequest()
                assertEquals("POST", sent.method)
                assertEquals("/api/v1/plans/current/sessions/s1/exercises/db-bench-press/swap", sent.path)
                assertEquals("""{"replacementExerciseId":"other","applyToRemainingBlock":$block}""", sent.body.readUtf8())
            }
        }
    }
}
