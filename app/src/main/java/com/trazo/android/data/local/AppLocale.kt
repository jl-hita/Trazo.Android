package com.trazo.android.data.local

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/** What the user chose for the language of the app and of the texts TRAZO sends: follow the device, or one of the languages of the app. */
object LanguageChoice {
    const val AUTO = "auto"
    const val ES = "es"
    const val EN = "en"
    /** Languages of the app, in the order the picker shows them. */
    val LANGUAGES = listOf(EN, ES, "ca", "de", "pt", "fr", "it", "pl", "tr", "sv", "zh")
    val ALL = listOf(AUTO) + LANGUAGES

    /** Anything unknown (an older or damaged value) is automatic. */
    fun normalize(value: String?): String = value?.lowercase()?.takeIf { it in ALL } ?: AUTO
}

interface LanguageStore {
    fun choice(): String
    fun save(choice: String)
}

class SharedPreferencesLanguageStore(context: Context) : LanguageStore {
    private val prefs = context.applicationContext.getSharedPreferences("language", Context.MODE_PRIVATE)
    override fun choice(): String = LanguageChoice.normalize(prefs.getString(KEY, null))
    override fun save(choice: String) { prefs.edit().putString(KEY, LanguageChoice.normalize(choice)).apply() }
    private companion object { const val KEY = "choice" }
}

/**
 * Puts the chosen language into effect: the default locale (what the labels and the API calls read) and the resources of an
 * activity. "auto" is the language of the device, which is captured once so it can be restored after a language was forced.
 */
object AppLocale {
    /** The language of the device (not affected by the default locale set here). Replaceable so it can be tested off a device. */
    internal var systemProvider: () -> Locale = { Resources.getSystem().configuration.locales.get(0) }

    /** The locale a choice means. */
    fun localeFor(choice: String): Locale = when (LanguageChoice.normalize(choice)) {
        LanguageChoice.AUTO -> systemProvider()
        "pt" -> Locale("pt", "BR") // Brazilian Portuguese
        "zh" -> Locale("zh", "CN") // Simplified Chinese
        else -> Locale(LanguageChoice.normalize(choice))
    }

    /** The language the app is shown in: the default locale's when the app is translated into it, English otherwise. */
    fun appLanguage(): String = Locale.getDefault().language.takeIf { it in LanguageChoice.LANGUAGES } ?: "en"

    fun apply(choice: String) = Locale.setDefault(localeFor(choice))

    /** The context an activity should use so its strings follow the choice (automatic leaves it as the system made it). */
    fun wrap(base: Context, choice: String): Context {
        if (LanguageChoice.normalize(choice) == LanguageChoice.AUTO) return base
        val configuration = Configuration(base.resources.configuration).apply { setLocale(localeFor(choice)) }
        return base.createConfigurationContext(configuration)
    }
}
