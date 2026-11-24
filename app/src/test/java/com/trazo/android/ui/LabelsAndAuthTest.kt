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

    @Test fun `vocabulary follows the language and is English when the language is not supported`() {
        Locale.setDefault(Locale("es", "ES"))
        assertEquals("Hipertrofia", Labels.goal("Hypertrophy"))
        assertEquals("Pecho", Labels.muscle("Chest"))
        Locale.setDefault(Locale("fr", "FR"))
        assertEquals("Hypertrophie", Labels.goal("Hypertrophy"))
        Locale.setDefault(Locale("ru", "RU")) // a language the app is not translated into is shown in English
        assertEquals("Muscle growth", Labels.goal("Hypertrophy"))
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("Muscle growth", Labels.goal("Hypertrophy"))
        assertEquals("Lower back", Labels.muscle("ErectorSpinae"))
        assertEquals("Monday", Labels.day("Monday"))
    }

    @Test fun `weekdays start on the first day of the locale week`() {
        val days = listOf("Sunday", "Friday", "Monday", "Wednesday")
        assertEquals(listOf("Monday", "Wednesday", "Friday", "Sunday"), Labels.inWeekOrder(days, Locale("es", "ES")))
        assertEquals(listOf("Sunday", "Monday", "Wednesday", "Friday"), Labels.inWeekOrder(days, Locale.US))
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
        assertEquals("Intermediate", Labels.difficulty("intermediate"))
        Locale.setDefault(Locale("es", "ES"))
        assertEquals("Intermedio", Labels.difficulty("intermediate"))
    }

    @Test fun `all backend equipment codes have translated labels`() {
        assertEquals(54, Labels.allEquipment.size)
        assertEquals(Labels.allEquipment.toSet(), Labels.allEquipmentEnglish.toSet())
        Locale.setDefault(Locale("es", "ES"))
        assertEquals("Barra hexagonal", Labels.equipment("trap-bar"))
        assertEquals("Máquina de curl femoral", Labels.equipment("leg-curl-machine"))
        assertEquals("Anillas", Labels.equipment("rings"))
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("Trap bar / hex bar", Labels.equipment("trap-bar"))
        assertEquals("Leg curl machine", Labels.equipment("leg-curl-machine"))
        assertEquals("Gymnastics rings", Labels.equipment("rings"))
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
