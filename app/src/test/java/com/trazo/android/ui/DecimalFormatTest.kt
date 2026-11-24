package com.trazo.android.ui

import com.trazo.android.ui.common.Labels
import com.trazo.android.ui.progress.formatKgText
import com.trazo.android.ui.workout.WorkoutViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** Weights are written the way the language of the app writes them, and what the app writes it can read back. */
class DecimalFormatTest {
    private val original = Locale.getDefault()
    @After fun restore() { Locale.setDefault(original) }

    @Test fun `a decimal uses the separator of the language`() {
        Locale.setDefault(Locale("es", "ES"))
        assertEquals("62,5", WorkoutViewModel.formatKg(62.5))
        assertEquals("62,5", formatKgText(62.5))
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("62.5", WorkoutViewModel.formatKg(62.5))
        assertEquals("62.5", formatKgText(62.5))
        Locale.setDefault(Locale.GERMAN)
        assertEquals("62,5", WorkoutViewModel.formatKg(62.5))
    }

    @Test fun `a whole number has no decimals and thousands are not grouped`() {
        Locale.setDefault(Locale("es", "ES"))
        assertEquals("65", WorkoutViewModel.formatKg(65.0))
        assertEquals("1250", WorkoutViewModel.formatKg(1250.0))
        assertEquals("–", formatKgText(null))
    }

    @Test fun `the decimals kept are limited`() {
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("62.33", Labels.decimal(62.3333))
        assertEquals("62.3", formatKgText(62.3333))
    }

    @Test fun `what is written is read back in every language`() {
        for (locale in listOf(Locale("es", "ES"), Locale.ENGLISH, Locale.GERMAN, Locale.FRENCH, Locale("pt", "BR"))) {
            Locale.setDefault(locale)
            assertEquals(locale.toString(), 62.5, WorkoutViewModel.parseWeight(WorkoutViewModel.formatKg(62.5))!!, 0.0)
        }
    }
}
