package com.trazo.android.ui

import com.trazo.android.auth.AuthError
import com.trazo.android.auth.AuthException
import com.trazo.android.auth.AuthRepository
import com.trazo.android.testing.FakeAuthApi
import com.trazo.android.testing.MemoryTokenStore
import com.trazo.android.ui.common.Labels
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class LabelsAndAuthTest {
    private val original = Locale.getDefault()
    @After fun restore() { Locale.setDefault(original) }

    @Test fun `vocabulary is Spanish by default and English on English devices`() {
        Locale.setDefault(Locale("es", "ES"))
        assertEquals("Hipertrofia", Labels.goal("Hypertrophy"))
        assertEquals("Pecho", Labels.muscle("Chest"))
        Locale.setDefault(Locale("fr", "FR")) // any other language falls back to Spanish
        assertEquals("Hipertrofia", Labels.goal("Hypertrophy"))
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("Muscle growth", Labels.goal("Hypertrophy"))
        assertEquals("Lower back", Labels.muscle("ErectorSpinae"))
        assertEquals("Monday", Labels.day("Monday"))
    }

    @Test fun `exercise names follow the language and unknown codes stay readable`() {
        Locale.setDefault(Locale("es", "ES"))
        assertEquals("Sentadilla", Labels.localName("Sentadilla", "Squat"))
        assertEquals("Squat", Labels.otherName("Sentadilla", "Squat"))
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("Squat", Labels.localName("Sentadilla", "Squat"))
        assertEquals("Sentadilla", Labels.localName("Sentadilla", "")) // no English name: keep Spanish
        assertEquals("New thing", Labels.humanize("NewThing"))
        assertEquals("Pull-up bar", Labels.equipment("pull-up-bar"))
        assertEquals("Mystery item", Labels.equipment("mystery-item"))
    }

    @Test fun `every vocabulary list used by the filters has a label in both languages`() {
        for (l in listOf(Locale("es", "ES"), Locale.ENGLISH)) {
            Locale.setDefault(l)
            (Labels.movementPatterns.map(Labels::pattern) + Labels.muscleGroups.map(Labels::muscle) + Labels.commonEquipment.map(Labels::equipment))
                .forEach { assertFalse(it.contains("  ")) }
        }
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("Hip hinge", Labels.pattern("Hinge"))
    }

    @Test fun `logout everywhere revokes on the server and clears the tokens`() = runBlocking {
        val store = MemoryTokenStore("a", "r")
        AuthRepository(FakeAuthApi(), store).logoutAll()
        assertNull(store.refresh)
    }

    @Test fun `account operations without a session fail with a typed error`() = runBlocking {
        val repo = AuthRepository(FakeAuthApi(), MemoryTokenStore())
        val e = runCatching { repo.logoutAll() }.exceptionOrNull() as AuthException
        assertEquals(AuthError.NoSession, e.code)
    }
}
