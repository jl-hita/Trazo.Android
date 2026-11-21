package com.trazo.android.data

import com.trazo.android.data.model.LocalizedExerciseText
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.repo.ExerciseRepository
import com.trazo.android.testing.FakeExercisesApi
import com.trazo.android.testing.httpError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseLocalizationTest {
    @Test fun `exercise detail uses the backend resolved text`() = runBlocking {
        val result = ExerciseRepository(FakeExercisesApi()).detail("db-bench-press")

        assertTrue(result is ApiResult.Success)
        val detail = (result as ApiResult.Success).value
        assertEquals("Bench press", detail.localizedName)
        assertEquals("en", detail.localizedNameLanguage)
        assertEquals("A chest exercise.", detail.localizedDescription)
        assertEquals(listOf("Set your hands."), detail.localizedInstructions)
        assertEquals(listOf("Control the descent."), detail.localizedTips)
        // Non-text metadata still comes from the regular detail endpoint.
        assertEquals(listOf("Chest"), detail.primaryMuscles)
    }

    @Test fun `localized endpoint failure keeps the original exercise detail`() = runBlocking {
        val api = FakeExercisesApi().apply {
            localizedResponse = httpError<LocalizedExerciseText>(404)
        }
        val result = ExerciseRepository(api).detail("db-bench-press")

        assertTrue(result is ApiResult.Success)
        val detail = (result as ApiResult.Success).value
        assertEquals("Descripción en español.", detail.descriptionEs)
        assertNull(detail.localizedName)
        assertNull(detail.localizedDescription)
    }
}
