package com.trazo.android.ui

import com.trazo.android.data.local.JsonFileStore
import com.trazo.android.data.local.WorkoutDraftStore
import com.trazo.android.data.model.ExerciseFeedbackRequest
import com.trazo.android.data.model.ExerciseView
import com.trazo.android.data.model.MuscleFeedbackRequest
import com.trazo.android.data.model.withoutHealthData
import com.trazo.android.ui.workout.ExerciseEntry
import com.trazo.android.ui.workout.FinishForm
import com.trazo.android.ui.workout.SetEntry
import com.trazo.android.ui.workout.WorkoutViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The two questions about each muscle: where they are asked, what is sent and what is kept on the phone. */
class MuscleQuestionsTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun entry(id: String, vararg muscles: String, done: Boolean = false) = ExerciseEntry(
        ExerciseView(id, id, 1, 2, 8, 12, 2.0, 90, "DoubleProgression", null, null, null, null, muscles.toList(), emptyList(), null),
        List(2) { SetEntry(weight = "20", reps = "10", done = done) }
    )

    @Test fun `recovery is asked on the first exercise of each muscle`() {
        val entries = listOf(entry("bench", "Chest"), entry("row", "Lats", "UpperBack"), entry("fly", "Chest"), entry("pulldown", "Lats"))

        assertEquals(listOf("Chest"), WorkoutViewModel.recoveryQuestions(entries, 0))
        assertEquals(listOf("Lats", "UpperBack"), WorkoutViewModel.recoveryQuestions(entries, 1))
        assertEquals(emptyList<String>(), WorkoutViewModel.recoveryQuestions(entries, 2))
        assertEquals(emptyList<String>(), WorkoutViewModel.recoveryQuestions(entries, 3))
    }

    @Test fun `work is asked on the last exercise of a muscle once all its sets are done`() {
        val pending = listOf(entry("bench", "Chest"), entry("fly", "Chest", done = true))
        assertEquals(emptyList<String>(), WorkoutViewModel.workloadQuestions(pending, 1))

        val finished = listOf(entry("bench", "Chest", done = true), entry("fly", "Chest", done = true))
        assertEquals(emptyList<String>(), WorkoutViewModel.workloadQuestions(finished, 0))
        assertEquals(listOf("Chest"), WorkoutViewModel.workloadQuestions(finished, 1))
    }

    @Test fun `recovery is sent for every muscle of the session and work only for what was trained`() {
        val entries = listOf(entry("bench", "Chest", done = true), entry("curl", "Biceps"))

        val request = WorkoutViewModel.buildLogRequest(
            entries, FinishForm(),
            muscles = mapOf(
                "Chest" to MuscleFeedbackRequest("recovered", "low"), "Biceps" to MuscleFeedbackRequest("notRecovered", "high"),
                "Calves" to MuscleFeedbackRequest("recovered")
            ),
            discomfort = mapOf("bench" to "mild", "curl" to "strong")
        )

        assertEquals(
            mapOf("Chest" to MuscleFeedbackRequest("recovered", "low"), "Biceps" to MuscleFeedbackRequest("notRecovered")),
            request.muscleFeedback
        )
        // The app no longer asks about the recovery of the whole session.
        assertEquals("Unknown", request.recovery)
        assertEquals(mapOf("bench" to ExerciseFeedbackRequest(discomfort = "mild")), request.exerciseFeedback)
        assertEquals(mapOf("bench" to "mild"), WorkoutViewModel.discomfortOf(request))
        // The answers about the muscles say nothing about health and stay; the discomfort does not.
        val withheld = request.withoutHealthData()
        assertEquals(request.muscleFeedback, withheld.muscleFeedback)
        assertEquals(emptyMap<String, ExerciseFeedbackRequest>(), withheld.exerciseFeedback.orEmpty())
    }

    @Test fun `nothing answered sends nothing`() {
        val request = WorkoutViewModel.buildLogRequest(listOf(entry("bench", "Chest", done = true)), FinishForm())

        assertNull(request.muscleFeedback)
        assertNull(request.exerciseFeedback)
    }

    @Test fun `the draft keeps the answers and forgets the discomfort when the consent is withdrawn`() {
        val drafts = WorkoutDraftStore(JsonFileStore(File(tmp.root, "drafts")))
        val muscles = mapOf("Chest" to MuscleFeedbackRequest(workload = "high"))
        drafts.save("s1", emptyList(), muscles, mapOf("bench" to "strong"))
        assertEquals(muscles, drafts.load("s1")?.muscles)
        assertEquals(mapOf("bench" to "strong"), drafts.load("s1")?.discomfort)

        drafts.forgetHealthData()

        assertEquals(muscles, drafts.load("s1")?.muscles)
        assertNull(drafts.load("s1")?.discomfort)
    }
}
