package com.trazo.android.ui.common

/**
 * Spanish names for the vocabulary the API sends in English (enum names, movement patterns, muscles, equipment).
 * Unknown values fall back to a readable form of the raw value, so a new backend value never breaks a screen.
 */
object Labels {
    private val goal = mapOf("GeneralFitness" to "Forma general", "Hypertrophy" to "Hipertrofia", "Strength" to "Fuerza")
    private val experience = mapOf("Beginner" to "Principiante", "Intermediate" to "Intermedio", "Advanced" to "Avanzado")
    private val day = mapOf(
        "Monday" to "Lunes", "Tuesday" to "Martes", "Wednesday" to "Miércoles", "Thursday" to "Jueves",
        "Friday" to "Viernes", "Saturday" to "Sábado", "Sunday" to "Domingo"
    )
    private val limitation = mapOf(
        "Knee" to "Rodilla", "Shoulder" to "Hombro", "LowerBack" to "Zona lumbar", "Wrist" to "Muñeca", "Elbow" to "Codo"
    )
    private val recovery = mapOf("Unknown" to "Prefiero no decirlo", "Good" to "Bien recuperado", "Partial" to "A medias", "Insufficient" to "Poco recuperado")
    private val reason = mapOf(
        "Time" to "Falta de tiempo", "Fatigue" to "Fatiga", "Equipment" to "Material", "Motivation" to "Motivación",
        "Illness" to "Enfermedad", "Other" to "Otro"
    )
    private val phase = mapOf("accumulation" to "Acumulación", "intensification" to "Intensificación", "deload" to "Descarga")
    private val pattern = mapOf(
        "Squat" to "Sentadilla", "Hinge" to "Bisagra de cadera", "HorizontalPush" to "Empuje horizontal",
        "VerticalPush" to "Empuje vertical", "HorizontalPull" to "Tirón horizontal", "VerticalPull" to "Tirón vertical",
        "SingleLeg" to "Unilateral de pierna", "Core" to "Core", "Accessory" to "Accesorio"
    )
    private val muscle = mapOf(
        "Chest" to "Pecho", "Lats" to "Dorsales", "UpperBack" to "Espalda alta", "Deltoids" to "Hombros", "Biceps" to "Bíceps",
        "Triceps" to "Tríceps", "Forearms" to "Antebrazos", "Abdominals" to "Abdominales", "Obliques" to "Oblicuos",
        "ErectorSpinae" to "Zona lumbar", "Glutes" to "Glúteos", "Quadriceps" to "Cuádriceps", "Hamstrings" to "Isquiotibiales",
        "Calves" to "Gemelos", "Adductors" to "Aductores", "Abductors" to "Abductores", "HipFlexors" to "Flexores de cadera",
        "Serratus" to "Serrato", "TibialisAnterior" to "Tibial anterior", "RotatorCuff" to "Manguito rotador"
    )
    val movementPatterns = listOf("Squat", "Hinge", "HorizontalPush", "VerticalPush", "HorizontalPull", "VerticalPull", "SingleLeg", "Core")
    val muscleGroups = muscle.keys.toList()
    val experienceLevels = listOf("Beginner", "Intermediate", "Advanced")
    val commonEquipment = listOf("barbell", "dumbbell", "machine", "cable", "bench", "kettlebell", "bodyweight", "band")
    private val equipment = mapOf(
        "barbell" to "Barra", "dumbbell" to "Mancuernas", "bench" to "Banco", "cable" to "Polea", "machine" to "Máquina",
        "kettlebell" to "Pesa rusa", "bodyweight" to "Peso corporal", "band" to "Banda elástica", "pull-up-bar" to "Barra de dominadas",
        "rack" to "Rack", "smith-machine" to "Máquina Smith", "ez-bar" to "Barra Z"
    )
    private val difficulty = mapOf("Beginner" to "Principiante", "Intermediate" to "Intermedio", "Advanced" to "Avanzado", "Easy" to "Fácil", "Hard" to "Difícil")
    private val timeline = mapOf("NotStarted" to "No ha empezado", "InProgress" to "En curso", "Finished" to "Terminado")

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
    fun timeline(v: String) = lookup(timeline, v)

    fun muscles(values: List<String>) = values.joinToString(", ") { muscle(it) }

    private fun lookup(map: Map<String, String>, value: String): String = map[value] ?: humanize(value)

    /** `HorizontalPush` / `pull-up-bar` / `lower_back` → `Horizontal push`, `Pull up bar`, `Lower back`. */
    fun humanize(value: String): String {
        val spaced = value.replace(Regex("([a-z])([A-Z])"), "$1 $2").replace('-', ' ').replace('_', ' ').trim().lowercase()
        return spaced.replaceFirstChar { it.uppercase() }
    }

    /** 125 → "2:05". */
    fun clock(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)
}
