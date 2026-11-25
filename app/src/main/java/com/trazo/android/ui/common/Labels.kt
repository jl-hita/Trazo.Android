package com.trazo.android.ui.common

import com.trazo.android.data.model.ExerciseListItem
import com.trazo.android.ui.common.vocabulary.*
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.text.NumberFormat
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Names for the vocabulary the API sends as codes (enum names, movement patterns, muscles, equipment) in the language of the
 * app. A value missing in a language falls back to English and then to a readable form of the raw value, so a new backend
 * value never breaks a screen.
 */
object Labels {
    private val vocabularies: Map<String, Vocabulary> = mapOf(
        "en" to VocabularyEn, "es" to VocabularyEs, "ca" to VocabularyCa, "de" to VocabularyDe, "pt" to VocabularyPt,
        "fr" to VocabularyFr, "it" to VocabularyIt, "pl" to VocabularyPl, "tr" to VocabularyTr, "sv" to VocabularySv, "zh" to VocabularyZh,
        "gl" to VocabularyGl, "eu" to VocabularyEu, "nl" to VocabularyNl, "da" to VocabularyDa, "nb" to VocabularyNb, "fi" to VocabularyFi,
        "cs" to VocabularyCs, "sk" to VocabularySk, "sl" to VocabularySl, "hr" to VocabularyHr, "sr" to VocabularySr, "bs" to VocabularyBs,
        "mk" to VocabularyMk, "ru" to VocabularyRu, "el" to VocabularyEl, "ar" to VocabularyAr, "ja" to VocabularyJa, "ko" to VocabularyKo
    )

    /** The language the app is shown in: the default locale (the app sets it from the language choice), English when unsupported. */
    fun language(): String = com.trazo.android.data.local.AppLocale.appLanguage()

    fun english(): Boolean = language() == "en"

    private class Dict(val select: (Vocabulary) -> Map<String, String>) {
        fun of(value: String): String? = vocabularies[language()]?.let(select)?.get(value) ?: select(VocabularyEn)[value]
    }

    private val goal = Dict { it.goal }
    private val experience = Dict { it.experience }
    private val limitation = Dict { it.limitation }
    private val recovery = Dict { it.recovery }
    private val reason = Dict { it.reason }
    private val phase = Dict { it.phase }
    private val pattern = Dict { it.pattern }
    private val muscle = Dict { it.muscle }
    private val equipment = Dict { it.equipment }
    private val difficulty = Dict { it.difficulty }
    private val split = Dict { it.split }

    /** All equipment codes supported by the backend catalog, used to keep vocabulary coverage testable. */
    val allEquipment = VocabularyEs.equipment.keys.toList()
    val allEquipmentEnglish = VocabularyEn.equipment.keys.toList()

    val movementPatterns = listOf("Squat", "Hinge", "HorizontalPush", "VerticalPush", "HorizontalPull", "VerticalPull", "SingleLeg", "Core")
    val muscleGroups = listOf(
        "Chest", "Lats", "UpperBack", "Deltoids", "Biceps", "Triceps", "Forearms", "Abdominals", "Obliques", "ErectorSpinae", "Glutes",
        "Quadriceps", "Hamstrings", "Calves", "Adductors", "Abductors", "HipFlexors", "Serratus", "TibialisAnterior", "RotatorCuff"
    )
    val experienceLevels = listOf("Beginner", "Intermediate", "Advanced")
    val commonEquipment = listOf("barbell", "dumbbell", "cable", "bench", "kettlebell", "resistance-band", "pull-up-bar", "low-row-bar", "squat-rack", "smith-machine", "leg-press-machine", "dip-station", "ab-wheel", "step")

    fun goal(v: String) = lookup(goal, v)
    fun experience(v: String) = lookup(experience, v)
    /** Name of a weekday in the app language, written by the platform ("Lunes" / "Monday" / "Montag"). */
    fun day(v: String): String {
        val dow = runCatching { DayOfWeek.valueOf(v.uppercase()) }.getOrNull() ?: return humanize(v)
        return dow.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()).replaceFirstChar { it.uppercase() }
    }
    fun split(v: String) = lookup(split, v)

    /** Three-letter name of a weekday in the app language ("lun", "mié" / "Mon", "Wed"); unknown values fall back to [day]. */
    fun dayShort(v: String): String {
        val dow = runCatching { DayOfWeek.valueOf(v.uppercase()) }.getOrNull() ?: return day(v)
        return dow.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault()).trimEnd('.').replaceFirstChar { it.uppercase() }
    }

    /**
     * Orders weekday names from the first day of the week of the device region (Monday in Spain, Sunday in the US), taken
     * from the locale and not from the location. Values that are not weekdays keep their place at the end.
     */
    fun inWeekOrder(days: List<String>, locale: Locale = Locale.getDefault()): List<String> {
        val first = WeekFields.of(locale).firstDayOfWeek
        fun rank(v: String): Int = runCatching { DayOfWeek.valueOf(v.uppercase()) }.getOrNull()?.let { (it.value - first.value + 7) % 7 } ?: 7
        return days.sortedBy { rank(it) }
    }
    fun limitation(v: String) = lookup(limitation, v)
    fun recovery(v: String) = lookup(recovery, v)
    fun reason(v: String) = lookup(reason, v)
    fun phase(v: String) = lookup(phase, v.lowercase())
    fun pattern(v: String) = lookup(pattern, v)
    fun muscle(v: String) = lookup(muscle, v)
    fun equipment(v: String) = lookup(equipment, v.lowercase())
    fun difficulty(v: String) = lookup(difficulty, v)

    fun muscles(values: List<String>) = values.joinToString(", ") { muscle(it) }

    /** Name of a catalog exercise in the user's language: the catalog has Spanish and English names, and English serves every other language. */
    fun localName(es: String, en: String?): String =
        if (language() == "es" || en.isNullOrBlank()) es else en

    /** Name of an item of the catalog list: the server's translation when it sends one, Spanish or English otherwise. */
    fun itemName(item: ExerciseListItem): String =
        item.localizedName?.takeIf { it.isNotBlank() } ?: localName(item.nameEs.ifBlank { item.nameEn }, item.nameEn)

    /** The name in the other of the two catalog languages, shown as a subtitle. */
    fun otherName(es: String, en: String?): String? =
        if (language() == "es" || en.isNullOrBlank()) en else es

    /** The API is not consistent about case ("Intermediate" / "intermediate"), so a capitalised retry is also tried. */
    private fun lookup(dict: Dict, value: String): String =
        dict.of(value) ?: dict.of(value.replaceFirstChar { it.uppercase() }) ?: humanize(value)

    /** `HorizontalPush` / `pull-up-bar` / `lower_back` → `Horizontal push`, `Pull up bar`, `Lower back`. */
    fun humanize(value: String): String {
        val spaced = value.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace('-', ' ').replace('_', ' ').trim().lowercase()
        return spaced.replaceFirstChar { it.uppercase() }
    }

    /**
     * A number the way the language of the app writes it: "62,5" in Spanish, "62.5" in English. A whole number drops the decimals
     * ("65"), thousands are not grouped (it is also written back into text fields), and no more than [maxDecimals] are kept.
     */
    fun decimal(value: Double, maxDecimals: Int = 2): String =
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = maxDecimals
            isGroupingUsed = false
        }.format(value)

    /** 125 → "2:05". */
    fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
}
