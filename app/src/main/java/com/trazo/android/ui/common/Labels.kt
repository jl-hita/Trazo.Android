package com.trazo.android.ui.common

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields
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
        mapOf("accumulation" to "Acumulación", "intensification" to "Intensificación", "deload" to "Descarga", "preparation" to "Preparación"),
        mapOf("accumulation" to "Accumulation", "intensification" to "Intensification", "deload" to "Deload", "preparation" to "Preparation")
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
            "barbell" to "Barra", "dumbbell" to "Mancuernas", "kettlebell" to "Pesa rusa", "cable" to "Polea / estación de cables",
            "bench" to "Banco de musculación", "squat-rack" to "Jaula o rack de sentadillas", "pull-up-bar" to "Barra de dominadas",
            "low-row-bar" to "Barra baja para remo", "bodyweight" to "Peso corporal", "body-only" to "Peso corporal",
            "band" to "Banda elástica", "bands" to "Bandas elásticas", "resistance-band" to "Banda elástica",
            "ez-bar" to "Barra Z", "trap-bar" to "Barra hexagonal", "smith-machine" to "Máquina Smith",
            "leg-press-machine" to "Prensa de piernas", "hack-squat-machine" to "Máquina de sentadilla hack",
            "leg-curl-machine" to "Máquina de curl femoral", "leg-extension-machine" to "Máquina de extensión de piernas",
            "dip-station" to "Estación de fondos / paralelas", "captains-chair" to "Silla romana",
            "ab-wheel" to "Rueda abdominal", "step" to "Cajón o plataforma", "stability-ball" to "Fitball / balón de estabilidad",
            "medicine-ball" to "Balón medicinal", "exercise-ball" to "Fitball", "rings" to "Anillas",
            "dip-bar" to "Barras paralelas", "trx" to "TRX", "suspension-trainer" to "Entrenador de suspensión",
            "rower" to "Máquina de remo", "glute-ham-developer" to "Banco GHD (glute-ham developer)",
            "landmine" to "Anclaje landmine para barra", "t-bar-row-handle" to "Asa para remo en T",
            "preacher-curl-bench" to "Banco Scott", "other" to "Otro",
            "assisted-pull-up-machine" to "Máquina de dominadas asistidas", "chest-press-machine" to "Máquina de press de pecho",
            "seated-row-machine" to "Máquina de remo sentado", "hip-abduction-machine" to "Máquina de abductores",
            "hip-adduction-machine" to "Máquina de aductores", "back-extension-machine" to "Máquina de extensión lumbar",
            "bicep-curl-machine" to "Máquina de curl de bíceps", "standing-calf-raise-machine" to "Máquina de gemelos de pie",
            "seated-calf-raise-machine" to "Máquina de gemelos sentado", "chest-fly-machine" to "Máquina de aperturas de pecho",
            "reverse-fly-machine" to "Máquina de aperturas inversas", "ab-crunch-machine" to "Máquina de abdominales",
            "shoulder-press-machine" to "Máquina de press de hombros", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Máquina de hip thrust",
            "plate-loaded-lateral-raise-machine" to "Máquina de elevaciones laterales",
            "lat-pulldown-machine" to "Máquina de jalón al pecho"
        ),
        mapOf(
            "barbell" to "Barbell", "dumbbell" to "Dumbbells", "kettlebell" to "Kettlebell", "cable" to "Cable station",
            "bench" to "Weight bench", "squat-rack" to "Squat rack", "pull-up-bar" to "Pull-up bar",
            "low-row-bar" to "Low horizontal row bar", "bodyweight" to "Bodyweight", "body-only" to "Bodyweight",
            "band" to "Resistance band", "bands" to "Resistance bands", "resistance-band" to "Resistance band",
            "ez-bar" to "EZ bar", "trap-bar" to "Trap bar / hex bar", "smith-machine" to "Smith machine",
            "leg-press-machine" to "Leg press machine", "hack-squat-machine" to "Hack squat machine",
            "leg-curl-machine" to "Leg curl machine", "leg-extension-machine" to "Leg extension machine",
            "dip-station" to "Dip station / parallel bars", "captains-chair" to "Captain's chair",
            "ab-wheel" to "Ab wheel", "step" to "Plyometric box / step", "stability-ball" to "Stability ball",
            "medicine-ball" to "Medicine ball", "exercise-ball" to "Exercise ball", "rings" to "Gymnastics rings",
            "dip-bar" to "Dip bars", "trx" to "TRX", "suspension-trainer" to "Suspension trainer",
            "rower" to "Rowing machine", "glute-ham-developer" to "Glute-ham developer (GHD)",
            "landmine" to "Barbell landmine attachment", "t-bar-row-handle" to "T-bar row handle",
            "preacher-curl-bench" to "Preacher curl bench", "other" to "Other",
            "assisted-pull-up-machine" to "Assisted pull-up machine", "chest-press-machine" to "Chest press machine",
            "seated-row-machine" to "Seated row machine", "hip-abduction-machine" to "Hip abduction machine",
            "hip-adduction-machine" to "Hip adduction machine", "back-extension-machine" to "Back extension machine",
            "bicep-curl-machine" to "Biceps curl machine", "standing-calf-raise-machine" to "Standing calf raise machine",
            "seated-calf-raise-machine" to "Seated calf raise machine", "chest-fly-machine" to "Chest fly machine",
            "reverse-fly-machine" to "Reverse fly machine", "ab-crunch-machine" to "Ab crunch machine",
            "shoulder-press-machine" to "Shoulder press machine", "pec-deck" to "Pec deck",
            "hip-thrust-machine" to "Hip thrust machine",
            "plate-loaded-lateral-raise-machine" to "Plate-loaded lateral raise machine",
            "lat-pulldown-machine" to "Lat pulldown machine"
        )
    )
    /** All equipment codes supported by the backend catalog, used to keep vocabulary coverage testable. */
    val allEquipment = equipment.es.keys.toList()
    private val difficulty = Dict(
        mapOf("Beginner" to "Principiante", "Intermediate" to "Intermedio", "Advanced" to "Avanzado", "Easy" to "Fácil", "Hard" to "Difícil"),
        mapOf("Beginner" to "Beginner", "Intermediate" to "Intermediate", "Advanced" to "Advanced", "Easy" to "Easy", "Hard" to "Hard")
    )

    private val split = Dict(
        mapOf("Automatic" to "Automático", "FullBody" to "Cuerpo completo", "UpperLower" to "Torso / pierna", "PushPullLegs" to "Empuje / tirón / pierna", "BroSplit" to "Por grupos musculares"),
        mapOf("Automatic" to "Automatic", "FullBody" to "Full body", "UpperLower" to "Upper / lower", "PushPullLegs" to "Push / pull / legs", "BroSplit" to "Body-part split")
    )

    val movementPatterns = listOf("Squat", "Hinge", "HorizontalPush", "VerticalPush", "HorizontalPull", "VerticalPull", "SingleLeg", "Core")
    val muscleGroups = listOf(
        "Chest", "Lats", "UpperBack", "Deltoids", "Biceps", "Triceps", "Forearms", "Abdominals", "Obliques", "ErectorSpinae", "Glutes",
        "Quadriceps", "Hamstrings", "Calves", "Adductors", "Abductors", "HipFlexors", "Serratus", "TibialisAnterior", "RotatorCuff"
    )
    val experienceLevels = listOf("Beginner", "Intermediate", "Advanced")
    val commonEquipment = listOf("barbell", "dumbbell", "cable", "bench", "kettlebell", "resistance-band", "pull-up-bar", "low-row-bar", "squat-rack", "smith-machine", "leg-press-machine", "dip-station", "ab-wheel", "step")

    fun goal(v: String) = lookup(goal, v)
    fun experience(v: String) = lookup(experience, v)
    fun day(v: String) = lookup(day, v)
    fun split(v: String) = lookup(split, v)

    /** Three-letter name of a weekday in the app language ("lun", "mié" / "Mon", "Wed"); unknown values fall back to [day]. */
    fun dayShort(v: String): String {
        val dow = runCatching { DayOfWeek.valueOf(v.uppercase()) }.getOrNull() ?: return day(v)
        val locale = if (english()) Locale.ENGLISH else Locale("es")
        return dow.getDisplayName(TextStyle.SHORT, locale).trimEnd('.').replaceFirstChar { it.uppercase() }
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

    /** Name of a catalog exercise in the user's language (the catalog has Spanish and English names). */
    fun localName(es: String, en: String?): String = if (english() && !en.isNullOrBlank()) en else es

    /** The name in the other language, shown as a subtitle. */
    fun otherName(es: String, en: String?): String? = if (english() && !en.isNullOrBlank()) es else en

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
