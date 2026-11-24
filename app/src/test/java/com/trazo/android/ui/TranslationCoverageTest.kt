package com.trazo.android.ui

import com.trazo.android.data.local.AppLocale
import com.trazo.android.data.local.LanguageChoice
import com.trazo.android.data.net.acceptLanguage
import com.trazo.android.ui.common.Labels
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

/**
 * The app is translated into every language of [LanguageChoice.LANGUAGES]: each one has all the texts of the default (English) file with
 * the same placeholders, every code of the API vocabulary and an entry in the system language list. A missing or broken translation
 * fails here and not on the phone of a user.
 */
class TranslationCoverageTest {
    private val original = Locale.getDefault()
    @After fun restore() { Locale.setDefault(original) }

    private val resources: File = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }

    private class Entry(val name: String, val values: Map<String, String>)

    /** Strings and plural items of a resource file: name -> (quantity or "" -> text). */
    private fun load(dir: String): List<Entry> {
        val text = File(resources, "$dir/strings.xml").readText()
        val strings = Regex("<string name=\"([^\"]+)\"([^>]*)>(.*?)</string>", RegexOption.DOT_MATCHES_ALL).findAll(text)
            .filter { !it.groupValues[2].contains("translatable=\"false\"") }
            .map { Entry(it.groupValues[1], mapOf("" to it.groupValues[3])) }
        val plurals = Regex("<plurals name=\"([^\"]+)\">(.*?)</plurals>", RegexOption.DOT_MATCHES_ALL).findAll(text).map { m ->
            Entry(m.groupValues[1], Regex("<item quantity=\"(\\w+)\">(.*?)</item>", RegexOption.DOT_MATCHES_ALL).findAll(m.groupValues[2]).associate { it.groupValues[1] to it.groupValues[2] })
        }
        return (strings + plurals).toList()
    }

    private val placeholder = Regex("%\\d\\$[sdf]|%\\d\\$\\.\\d[f]|\\{link}")
    private fun placeholders(text: String) = placeholder.findAll(text).map { it.value }.sorted().toList()
    private fun dirOf(language: String) = if (language == "en") "values" else "values-$language"

    @Test fun `every language has every text with the same placeholders`() {
        val english = load("values").associateBy { it.name }
        assertTrue(english.size > 500)
        for (language in LanguageChoice.LANGUAGES) {
            val texts = load(dirOf(language)).associateBy { it.name }
            val extra = texts.keys - english.keys
            assertTrue("$language has texts the default does not: $extra", extra.isEmpty())
            for ((name, source) in english) {
                val translated = texts[name] ?: error("$language lacks $name")
                for ((quantity, text) in source.values) {
                    val other = translated.values[quantity] ?: translated.values["other"] ?: error("$language lacks $name $quantity")
                    assertFalse("$language $name is empty", other.isBlank())
                    if (quantity == "" || quantity == "other") {
                        assertEquals("$language $name placeholders", placeholders(text), placeholders(other))
                    }
                }
                // every plural form the language needs has the placeholder too
                if (source.values.size > 1) {
                    translated.values.forEach { (q, t) -> assertEquals("$language $name $q", placeholders(source.values.getValue("other")), placeholders(t)) }
                }
            }
        }
    }

    @Test fun `quotes are escaped so the resource file compiles`() {
        val apostrophe = Regex("(?<!\\\\)'")
        val quote = Regex("(?<!\\\\)\"")
        for (language in LanguageChoice.LANGUAGES) {
            load(dirOf(language)).flatMap { it.values.values }.forEach { text ->
                assertFalse("$language: unescaped apostrophe in $text", apostrophe.containsMatchIn(text))
                assertFalse("$language: unescaped quote in $text", quote.containsMatchIn(text))
            }
        }
    }

    @Test fun `the system language list has every language of the app`() {
        val config = File(resources, "xml/locales_config.xml").readText()
        val listed = Regex("android:name=\"([^\"]+)\"").findAll(config).map { it.groupValues[1].substringBefore('-') }.toSet()
        assertEquals(LanguageChoice.LANGUAGES.toSet(), listed)
    }

    @Test fun `every language names its own language in the picker`() {
        val text = File(resources, "values/strings.xml").readText()
        for (language in LanguageChoice.LANGUAGES) assertTrue("language_$language", text.contains("name=\"language_$language\""))
    }

    @Test fun `every language has the whole vocabulary of the API and the weekdays`() {
        for (language in LanguageChoice.LANGUAGES) {
            Locale.setDefault(AppLocale.localeFor(language))
            assertEquals(language, Labels.language())
            for (code in Labels.muscleGroups) assertFalse("$language $code", Labels.muscle(code).isBlank())
            for (code in Labels.allEquipment) assertFalse("$language $code", Labels.equipment(code).isBlank())
            for (code in Labels.movementPatterns) assertFalse("$language $code", Labels.pattern(code).isBlank())
            for (day in listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")) {
                assertFalse("$language $day", Labels.day(day).isBlank())
                assertFalse("$language $day short", Labels.dayShort(day).isBlank())
            }
        }
        // a translation exists, not the English fallback, for what is shown most
        Locale.setDefault(AppLocale.localeFor("de"))
        assertEquals("Muskelaufbau", Labels.goal("Hypertrophy"))
        assertEquals("Montag", Labels.day("Monday"))
        Locale.setDefault(AppLocale.localeFor("zh"))
        assertEquals("增肌", Labels.goal("Hypertrophy"))
    }

    @Test fun `the server is asked in the language of the app and English as the fallback`() {
        Locale.setDefault(AppLocale.localeFor("es")); assertEquals("es", acceptLanguage())
        Locale.setDefault(AppLocale.localeFor("en")); assertEquals("en", acceptLanguage())
        Locale.setDefault(AppLocale.localeFor("de")); assertEquals("de, en;q=0.8", acceptLanguage())
        Locale.setDefault(Locale("ru")); assertEquals("en", acceptLanguage()) // not translated: English
    }

    @Test fun `choices map to the locales of the resources`() {
        assertEquals("pt", AppLocale.localeFor("pt").language)
        assertEquals("BR", AppLocale.localeFor("pt").country)
        assertEquals("zh", AppLocale.localeFor("zh").language)
        for (language in LanguageChoice.LANGUAGES) assertEquals(language, AppLocale.localeFor(language).language)
    }
}
