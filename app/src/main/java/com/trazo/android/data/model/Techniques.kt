package com.trazo.android.data.model

/**
 * A set technique of an exercise: what follows a main set (drops, mini-sets or resumptions). [type] is "DropSet", "MyoReps",
 * "MyoRepMatch" or "RestPause"; [appliesTo] is "LastSet" or "AllSets". The server sends it only to apps that declare the
 * capability, and accepts the same shape in a manual plan.
 */
data class TechniqueView(
    val type: String, val appliesTo: String = Techniques.LAST_SET, val maxSegments: Int = 1, val restSeconds: Int = 0,
    val loadDropPercent: Int? = null, val segmentMinRepetitions: Int? = null, val segmentMaxRepetitions: Int? = null,
    val targetRir: Double? = null
)

/** How the exercises that share a superset letter are alternated. */
data class SupersetView(val group: String, val rounds: Int, val restBetweenSeconds: Int, val restAfterRoundSeconds: Int)

/** The rules of the techniques the app needs: defaults (the server's), which sets carry one and when it is over. */
object Techniques {
    const val DROP_SET = "DropSet"
    const val MYO_REPS = "MyoReps"
    const val MATCH = "MyoRepMatch"
    const val REST_PAUSE = "RestPause"
    const val LAST_SET = "LastSet"
    const val ALL_SETS = "AllSets"
    const val MAX_DROPS = 3

    fun defaults(type: String): TechniqueView = when (type) {
        MYO_REPS -> TechniqueView(type, LAST_SET, 5, 15, null, 3, 5, 1.0)
        MATCH -> TechniqueView(type, ALL_SETS, 5, 15, targetRir = 1.0)
        REST_PAUSE -> TechniqueView(type, LAST_SET, 2, 20, null, 2, null, 1.0)
        else -> TechniqueView(DROP_SET, LAST_SET, 1, 10, 20, targetRir = 1.0)
    }

    /** True when the set at [index] (from 0) of [sets] carries the technique. In a match the first set is the reference. */
    fun carries(technique: TechniqueView, index: Int, sets: Int): Boolean = when {
        technique.type == MATCH && technique.appliesTo == ALL_SETS -> index >= 1
        technique.type == MATCH -> sets > 1 && index == sets - 1
        technique.appliesTo == ALL_SETS -> true
        else -> index == sets - 1
    }

    /**
     * Whether another segment is due after the ones done. [firstSetReps] is the reference of a match, [mainReps] the main set
     * and [segmentReps] what each done segment reached, in order.
     */
    fun wantsAnother(technique: TechniqueView, firstSetReps: Int?, mainReps: Int?, segmentReps: List<Int>): Boolean {
        if (segmentReps.size >= technique.maxSegments) return false
        if (technique.type == MATCH) return firstSetReps == null || (mainReps ?: 0) + segmentReps.sum() < firstSetReps
        val minimum = technique.segmentMinRepetitions ?: return true
        // Myo-reps and rest-pause stop at the first segment that falls short.
        return segmentReps.lastOrNull()?.let { it >= minimum } ?: true
    }

    /** The load a segment starts with: lower by the drop of a drop set (to half a kilo), the same load otherwise. */
    fun segmentLoad(technique: TechniqueView, previousKg: Double?): Double? {
        val drop = technique.loadDropPercent
        if (previousKg == null || technique.type != DROP_SET || drop == null) return previousKg
        return Math.round(previousKg * (100 - drop) / 100.0 * 2) / 2.0
    }
}
