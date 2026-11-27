package com.trazo.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.android.data.local.JsonFileStore
import com.trazo.android.data.local.PendingLogQueue
import com.trazo.android.data.local.PlanCache
import com.trazo.android.data.local.WorkoutDraftStore
import com.trazo.android.data.model.AlternativeView
import com.trazo.android.data.model.AlternativesResult
import com.trazo.android.data.model.SwapRequest
import com.trazo.android.data.repo.ExerciseRepository
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.testing.FakeExercisesApi
import com.trazo.android.testing.FakePlansApi
import com.trazo.android.ui.plan.SessionScreen
import com.trazo.android.ui.plan.SessionViewModel
import com.trazo.android.ui.theme.TrazoTheme
import com.trazo.android.ui.workout.WorkoutScreen
import com.trazo.android.ui.workout.WorkoutViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.io.File
import java.time.LocalDate
import java.util.UUID

/**
 * Swapping an exercise from the two screens that offer it: choosing a replacement asks for the scope, nothing is sent before the
 * answer, cancelling sends nothing, and the answer is what reaches the server.
 */
@RunWith(AndroidJUnit4::class)
class SwapScopeScreensTest {
    @get:Rule val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val files = File(context.cacheDir, "swap-scope-${UUID.randomUUID()}")
    private val api = FakePlansApi().apply {
        alternativesResponse = { exerciseId ->
            Response.success(AlternativesResult(exerciseId, listOf(AlternativeView(REPLACEMENT_ID, REPLACEMENT, "HorizontalPush", listOf("Chest"), emptyList(), "Pattern")), null))
        }
    }
    private val plans = PlanRepository(
        api,
        PlanCache(JsonFileStore(File(files, "cache")), today = { LocalDate.of(2026, 11, 17) }),
        PendingLogQueue(JsonFileStore(File(files, "queue")))
    )

    @After fun delete() { files.deleteRecursively() }

    private fun text(id: Int) = context.getString(id)

    private fun showWorkout() {
        runBlocking { plans.currentWeek() }
        val viewModel = WorkoutViewModel(plans, WorkoutDraftStore(JsonFileStore(File(files, "drafts"))), "s1")
        show { WorkoutScreen(viewModel, onExit = {}, onExercise = {}) }
    }

    private fun showSession() {
        runBlocking { plans.currentWeek() }
        val viewModel = SessionViewModel(plans, ExerciseRepository(FakeExercisesApi()), "s1")
        show {
            SessionScreen(viewModel, plans.week, "s1", lookup = plans::session, onBack = {}, onPremium = {}, onStart = {}, onExercise = {})
        }
    }

    private fun show(screen: @Composable () -> Unit) = compose.setContent { TrazoTheme { screen() } }

    private fun waitFor(text: String) =
        compose.waitUntil(10_000) { compose.onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    private fun waitUntilGone(text: String) =
        compose.waitUntil(10_000) { compose.onAllNodes(androidx.compose.ui.test.hasText(text)).fetchSemanticsNodes().isEmpty() }

    private fun chooseReplacement() {
        waitFor(text(R.string.workout_swap))
        compose.onNodeWithText(text(R.string.workout_swap)).performScrollTo().performClick()
        waitFor(REPLACEMENT)
        compose.onNodeWithText(REPLACEMENT).performScrollTo().performClick()
        waitFor(text(R.string.workout_swap_scope_title))
        compose.onNodeWithText(text(R.string.workout_swap_scope_session)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.workout_swap_scope_block)).assertIsDisplayed()
        assertTrue("nothing is sent before the scope is chosen", api.swaps.isEmpty())
    }

    private fun answer(button: Int) {
        compose.onNodeWithText(text(button)).performClick()
        waitUntilGone(text(R.string.workout_swap_scope_title))
    }

    private fun sent(remainingBlock: Boolean) =
        assertEquals(listOf(Triple("s1", "db-bench-press", SwapRequest(REPLACEMENT_ID, remainingBlock))), api.swaps)

    private fun cancelled() {
        answer(R.string.cancel)
        compose.waitForIdle()
        compose.onNodeWithText(text(R.string.workout_alternatives_title)).assertDoesNotExist()
        assertTrue(api.swaps.isEmpty())
    }

    @Test fun workoutSwapsThisSessionOnly() { showWorkout(); chooseReplacement(); answer(R.string.workout_swap_scope_session); sent(false) }

    @Test fun workoutSwapsTheRestOfTheBlock() { showWorkout(); chooseReplacement(); answer(R.string.workout_swap_scope_block); sent(true) }

    @Test fun workoutCancelChangesNothing() { showWorkout(); chooseReplacement(); cancelled() }

    @Test fun sessionSwapsThisSessionOnly() { showSession(); chooseReplacement(); answer(R.string.workout_swap_scope_session); sent(false) }

    @Test fun sessionSwapsTheRestOfTheBlock() { showSession(); chooseReplacement(); answer(R.string.workout_swap_scope_block); sent(true) }

    @Test fun sessionCancelChangesNothing() { showSession(); chooseReplacement(); cancelled() }

    private companion object {
        const val REPLACEMENT_ID = "machine-chest-press"
        const val REPLACEMENT = "Press en máquina"
    }
}
