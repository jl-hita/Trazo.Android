package com.trazo.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.trazo.android.data.local.JsonFileStore
import com.trazo.android.data.local.PendingLogQueue
import com.trazo.android.data.local.PlanCache
import com.trazo.android.data.local.WorkoutDraftStore
import com.trazo.android.data.repo.PlanRepository
import com.trazo.android.rest.ExactAlarmOffer
import com.trazo.android.testing.FakePlansApi
import com.trazo.android.ui.theme.TrazoTheme
import com.trazo.android.ui.workout.WorkoutScreen
import com.trazo.android.ui.workout.WorkoutViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** The bottom of the workout screen: its actions, and the offer to allow exact alarms (only while resting and when due). */
@RunWith(AndroidJUnit4::class)
class ExactAlarmOfferTest {
    @get:Rule val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val files = File(context.cacheDir, "exact-alarm-${UUID.randomUUID()}")
    private val plans = PlanRepository(
        FakePlansApi(),
        PlanCache(JsonFileStore(File(files, "cache")), today = { LocalDate.of(2026, 11, 17) }),
        PendingLogQueue(JsonFileStore(File(files, "queue")))
    )
    private var allowed = 0
    private var declined = 0

    @After fun delete() { files.deleteRecursively() }

    private val hint get() = context.getString(R.string.rest_exact_hint)

    private fun show(due: Boolean): WorkoutViewModel {
        runBlocking { plans.currentWeek() }
        val viewModel = WorkoutViewModel(plans, WorkoutDraftStore(JsonFileStore(File(files, "drafts"))), "s1")
        val offer = ExactAlarmOffer(isDue = { due }, allow = { allowed++ }, decline = { declined++ })
        compose.setContent { TrazoTheme { WorkoutScreen(viewModel, onExit = {}, onExercise = {}, exactAlarm = offer) } }
        return viewModel
    }

    private fun rest(viewModel: WorkoutViewModel) {
        compose.runOnUiThread { viewModel.startRest(90) }
        waitFor(context.getString(R.string.workout_rest_skip))
        compose.waitForIdle()
    }

    private fun waitFor(text: String) =
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    /** Without a keyboard the actions under the list are there: the session can always be finished. */
    @Test fun theSessionCanBeFinishedWithoutAKeyboard() {
        show(due = false)
        waitFor(context.getString(R.string.workout_finish))
        compose.onNodeWithText(context.getString(R.string.workout_finish)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.workout_add_exercise)).assertIsDisplayed()
    }

    @Test fun offeredWhileRestingAndAllowOpensTheSettings() {
        val viewModel = show(due = true)
        compose.onNodeWithText(hint).assertDoesNotExist()
        rest(viewModel)
        waitFor(hint)
        compose.onNodeWithText(hint).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.rest_exact_allow)).performClick()
        assertEquals(1 to 0, allowed to declined)
    }

    @Test fun turningItDownRemovesIt() {
        rest(show(due = true))
        waitFor(hint)
        compose.onNodeWithText(context.getString(R.string.rest_exact_decline)).performClick()
        compose.onNodeWithText(hint).assertDoesNotExist()
        assertEquals(0 to 1, allowed to declined)
    }

    @Test fun notOfferedWhenNotDue() {
        rest(show(due = false))
        compose.onNodeWithText(hint).assertDoesNotExist()
    }
}
