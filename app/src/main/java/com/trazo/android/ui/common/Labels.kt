package com.trazo.android.ui.common

import java.util.Locale

/**
 * Names for the vocabulary the API sends as codes (enum names, movement patterns, muscles, equipment), in Spanish (default)
 * and English. Unknown values fall back to a readable form of the raw value, so a new backend value never breaks a screen.
 */
object Labels {
    private class Dict(val es: Map<String, String>, val en: Map<String, String>) {
        fun of(value: String): String? = (if (english()) en else es)[value]
    }

    /** The app is Spanish unless the device language is English. */
    fun english(): Boolean = Locale.getDefault().language == "en"

    private val goal = Dict(
        mapOf("GeneralFitness" to "Forma general", "Hypertrophy" to "Hipertrofia", "Strength" to "Fuerza"),
        mapOf("GeneralFitness" to "General fitness", "Hypertrophy" to "Muscle growth", "Strength" to "Strength")
    )
    private val experience = Dict(
        mapOf("Beginner" to "Principiante", "Intermediate" to "Intermedio", "Advanced" to "Avanzado"),
        mapOf("Beginner" to "Beginner", "Intermediate" to "Intermediate", "Advanced" to "Advanced")
    )
    private val day = Dict(
        mapOf("Monday" to "Lunes", "Tuesday" to "Martes", "Wednesday" to "Miércoles", "Thursday" to "Jueves", "Friday" to "Viernes", "Saturday" to "Sábado", "Sunday" to "Domingo"),
        mapOf("Monday" to "Monday", "Tuesday" to "Tuesday", "Wednesday" to "Wednesday", "Thursday" to "Thursday", "Friday" to "Friday", "Saturday" to "Saturday", "Sunday" to "Sunday")
    )
    private val limitation = Dict(
        mapOf("Knee" to "Rodilla", "Shoulder" to "Hombro", "LowerBack" to "Zona lumbar", "Wrist" to "Muñeca", "Elbow" to "Codo"),
        mapOf("Knee" to "Knee", "Shoulder" to "Shoulder", "LowerBack" to "Lower back", "Wrist" to "Wrist", "Elbow" to "Elbow")
    )
    private val recovery = Dict(
        mapOf("Unknown" to "Prefiero no decirlo", "Good" to "Bien recuperado", "Partial" to "A medias", "Insufficient" to "Poco recuperado"),
        mapOf("Unknown" to "Prefer not to say", "Good" to "Well recovered", "Partial" to "Partly recovered", "Insufficient" to "Poorly recovered")
    )
    private val reason = Dict(
        mapOf("Time" to "Falta de tiempo", "Fatigue" to "Fatiga", "Equipment" to "Material", "Motivation" to "Motivación", "Illness" to "Enfermedad", "Other" to "Otro"),
        mapOf("Time" to "Lack of time", "Fatigue" to "Fatigue", "Equipment" to "Equipment", "Motivation" to "Motivation", "Illness" to "Illness", "Other" to "Other")
    )
    private val phase = Dict(
        mapOf("accumulation" to "Acumulación", "intensification" to "Intensificación", "deload" to "Descarga"),
        mapOf("accumulation" to "Accumulation", "intensification" to "Intensification", "deload" to "Deload")
    )
    private val pattern = Dict(
        mapOf(
            "Squat" to "Sentadilla", "Hinge" to "Bisagra de cadera", "HorizontalPush" to "Empuje horizontal", "VerticalPush" to "Empuje vertical",
            "HorizontalPull" to "Tirón horizontal", "VerticalPull" to "Tirón vertical", "SingleLeg" to "Unilateral de pierna", "Core" to "Core", "Accessory" to "Accesorio"
        ),
        mapOf(
            "Squat" to "Squat", "Hinge" to "Hip hinge", "HorizontalPush" to "Horizontal push", "VerticalPush" to "Vertical push",
            "HorizontalPull" to "Horizontal pull", "VerticalPull" to "Vertical pull", "SingleLeg" to "Single leg", "Core" to "Core", "Accessory" to "Accessory"
        )
    )
    private val muscle = Dict(
        mapOf(
            "Chest" to "Pecho", "Lats" to "Dorsales", "UpperBack" to "Espalda alta", "Deltoids" to "Hombros", "Biceps" to "Bíceps", "Triceps" to "Tríceps",
            "Forearms" to "Antebrazos", "Abdominals" to "Abdominales", "Obliques" to "Oblicuos", "ErectorSpinae" to "Zona lumbar", "Glutes" to "Glúteos",
            "Quadriceps" to "Cuádriceps", "Hamstrings" to "Isquiotibiales", "Calves" to "Gemelos", "Adductors" to "Aductores", "Abductors" to "Abductores",
            "HipFlexors" to "Flexores de cadera", "Serratus" to "Serrato", "TibialisAnterior" to "Tibial anterior", "RotatorCuff" to "Manguito rotador"
        ),
        mapOf(
            "Chest" to "Chest", "Lats" to "Lats", "UpperBack" to "Upper back", "Deltoids" to "Shoulders", "Biceps" to "Biceps", "Triceps" to "Triceps",
            "Forearms" to "Forearms", "Abdominals" to "Abs", "Obliques" to "Obliques", "ErectorSpinae" to "Lower back", "Glutes" to "Glutes",
            "Quadriceps" to "Quadriceps", "Hamstrings" to "Hamstrings", "Calves" to "Calves", "Adductors" to "Adductors", "Abductors" to "Abductors",
            "HipFlexors" to "Hip flexors", "Serratus" to "Serratus", "TibialisAnterior" to "Tibialis anterior", "RotatorCuff" to "Rotator cuff"
        )
    )
    private val equipment = Dict(
        mapOf(
            "barbell" to "Barra", "dumbbell" to "Mancuernas", "bench" to "Banco", "cable" to "Polea", "machine" to "Máquina", "kettlebell" to "Pesa rusa",
            "bodyweight" to "Peso corporal", "band" to "Banda elástica", "pull-up-bar" to "Barra de dominadas", "rack" to "Rack", "smith-machine" to "Máquina Smith", "ez-bar" to "Barra Z"
        ),
        mapOf(
            "barbell" to "Barbell", "dumbbell" to "Dumbbells", "bench" to "Bench", "cable" to "Cable", "machine" to "Machine", "kettlebell" to "Kettlebell",
            "bodyweight" to "Bodyweight", "band" to "Resistance band", "pull-up-bar" to "Pull-up bar", "rack" to "Rack", "smith-machine" to "Smith machine", "ez-bar" to "EZ bar"
        )
    )
    private val difficulty = Dict(
        mapOf("Beginner" to "Principiante", "Intermediate" to "Intermedio", "Advanced" to "Avanzado", "Easy" to "Fácil", "Hard" to "Difícil"),
        mapOf("Beginner" to "Beginner", "Intermediate" to "Intermediate", "Advanced" to "Advanced", "Easy" to "Easy", "Hard" to "Hard")
    )

    val movementPatterns = listOf("Squat", "Hinge", "HorizontalPush", "VerticalPush", "HorizontalPull", "VerticalPull", "SingleLeg", "Core")
    val muscleGroups = listOf(
        "Chest", "Lats", "UpperBack", "Deltoids", "Biceps", "Triceps", "Forearms", "Abdominals", "Obliques", "ErectorSpinae", "Glutes",
        "Quadriceps", "Hamstrings", "Calves", "Adductors", "Abductors", "HipFlexors", "Serratus", "TibialisAnterior", "RotatorCuff"
    )
    val experienceLevels = listOf("Beginner", "Intermediate", "Advanced")
    val commonEquipment = listOf("barbell", "dumbbell", "machine", "cable", "bench", "kettlebell", "bodyweight", "band")

    fun goal(v: String) = lookup(goal, v)
    fun experience(v: String) = lookup(experience, v)
    fun day(v: String) = lookup(day, v)
    fun limitation(v: String) = lookup(limitation, v)
    fun recovery(v: String) = lookup(recovery, v)
    fun reason(v: String) = lookup(reason, v)
    fun phase(v: String) = lookup(phase, v.lowercase())
    fun pattern(v: String) = lookup(pattern, v)
    fun muscle(v: String) = lookup(muscle, v)
    fun equipment(v: String) = lookup(equipment, v.lowercase())
    fun difficulty(v: String) = lookup(difficulty, v)

    fun muscles(values: List<String>) = values.joinToString(", ") { muscle(it) }

    /** Name of a catalog exercise in the user's language (the catalog has Spanish and English names). */
    fun localName(es: String, en: String): String = if (english() && en.isNotBlank()) en else es

    /** The name in the other language, shown as a subtitle. */
    fun otherName(es: String, en: String): String = if (english() && en.isNotBlank()) es else en

    /** The API is not consistent about case ("Intermediate" / "intermediate"), so a capitalised retry is also tried. */
    private fun lookup(dict: Dict, value: String): String =
        dict.of(value) ?: dict.of(value.replaceFirstChar { it.uppercase() }) ?: humanize(value)

    /** `HorizontalPush` / `pull-up-bar` / `lower_back` → `Horizontal push`, `Pull up bar`, `Lower back`. */
    fun humanize(value: String): String {
        val spaced = value.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace('-', ' ').replace('_', ' ').trim().lowercase()
        return spaced.replaceFirstChar { it.uppercase() }
    }

    /** 125 → "2:05". */
    fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
}
