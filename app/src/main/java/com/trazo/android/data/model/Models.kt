package com.trazo.android.data.model

// Contracts of the TRAZO API (docs/planning-engine.md, docs/subscriptions.md, docs/exercise-catalog-api.md).
// Dates travel as ISO strings (yyyy-MM-dd) and enums by name, exactly as the server sends them.

data class PhaseView(val sequence: Int, val name: String, val type: String, val startDate: String, val endDate: String, val objective: String)
data class MacrocycleView(val name: String, val notes: String, val phases: List<PhaseView>)
data class MesocycleView(val sequence: Int, val phaseType: String, val isDeload: Boolean, val startWeek: Int, val weeks: Int, val weekInBlock: Int)

data class PlanView(
    val id: String, val status: String, val timeline: String, val currentWeek: Int, val totalWeeks: Int,
    val startDate: String, val endDate: String, val goal: String, val experience: String, val split: String,
    val trainingDays: List<String>, val maxSessionMinutes: Int, val availableEquipment: List<String>,
    val excludedExerciseIds: List<String>, val limitations: List<String>, val currentPhase: Int?,
    val macrocycle: MacrocycleView, val disclaimer: String
)

data class ExerciseView(
    val exerciseId: String, val name: String, val order: Int, val sets: Int, val minRepetitions: Int, val maxRepetitions: Int,
    val targetRir: Double?, val restSeconds: Int, val progressionRule: String, val targetLoadKg: Double?, val loadBasis: String?,
    val warmupSets: Int?, val warmupNote: String?, val primaryMuscles: List<String>, val secondaryMuscles: List<String>,
    val swappedFromExerciseId: String?
)

data class LoggedSetView(
    val exerciseId: String, val setNumber: Int, val weightKg: Double?, val repetitions: Int?, val rir: Double?,
    val completed: Boolean, val skippedReason: String?, val pain: Boolean
)

data class LogView(
    val id: String, val loggedAt: String, val outcome: String, val nonCompletionReason: String, val recovery: String,
    val sessionRpe: Double?, val painReported: Boolean, val notes: String?, val sets: List<LoggedSetView>
)

data class SessionView(
    val id: String, val weekNumber: Int, val date: String, val day: String, val title: String,
    val estimatedDurationMinutes: Int, val status: String, val exercises: List<ExerciseView>, val log: LogView?
)

data class WeekView(
    val planId: String, val weekNumber: Int, val weekStart: String, val weekEnd: String, val timeline: String,
    val mesocycle: MesocycleView?, val sessions: List<SessionView>
)

data class AppliedAdaptationView(val kind: String, val exerciseIds: List<String>, val sessionsChanged: Int, val explanation: String)
data class ExerciseAdviceView(val exerciseId: String, val action: String, val explanation: String, val nextLoadKg: Double?, val suggestedRepetitions: List<Int>?)
data class AdaptationReport(
    val exercises: List<ExerciseAdviceView>, val sessionAction: String, val sessionExplanation: String,
    val applied: List<AppliedAdaptationView>, val notes: List<String>
)
data class LogResult(val session: SessionView, val adaptation: AdaptationReport)

data class AlternativeView(
    val exerciseId: String, val name: String, val movementPattern: String, val primaryMuscles: List<String>,
    val requiredEquipment: List<String>, val matchLevel: String
)
data class AlternativesResult(val exerciseId: String, val alternatives: List<AlternativeView>, val message: String?)

data class HistoryItem(
    val sessionId: String, val weekNumber: Int, val date: String, val title: String, val outcome: String,
    val loggedAt: String, val completedSets: Int, val plannedSets: Int
)

data class PlanOptions(
    val goals: List<String>, val experience: List<String>, val splits: List<String>, val days: List<String>,
    val limitations: List<String>, val equipment: List<String>, val minimumTrainingDays: Int, val maximumTrainingDays: Int,
    val minimumSessionMinutes: Int, val maximumSessionMinutes: Int, val minimumTotalWeeks: Int, val maximumTotalWeeks: Int,
    val defaultTotalWeeks: Int
)

// Requests.

data class PlanProfileRequest(
    val goal: String, val experience: String, val startDate: String? = null, val totalWeeks: Int? = null,
    val trainingDays: List<String>, val maxSessionMinutes: Int? = null, val availableEquipment: List<String> = emptyList(),
    val excludedExerciseIds: List<String> = emptyList(), val limitations: List<String> = emptyList(),
    val split: String = "Automatic", val includeDeloadPhases: Boolean = true, val deloadEveryTrainingWeeks: Int? = null
)

data class LogSetRequest(
    val exerciseId: String, val setNumber: Int, val weightKg: Double?, val repetitions: Int?, val rir: Double?,
    val completed: Boolean, val skippedReason: String? = null, val painReported: Boolean = false
)

data class LogSessionRequest(
    val outcome: String, val nonCompletionReason: String = "None", val recovery: String = "Unknown", val sessionRpe: Double? = null,
    val painReported: Boolean = false, val notes: String? = null, val sets: List<LogSetRequest>
)

data class SwapRequest(val replacementExerciseId: String, val applyToRemainingBlock: Boolean = false)

// Exercise catalog.

data class ExerciseListItem(
    val id: String, val nameEs: String, val nameEn: String, val descriptionEs: String, val difficulty: String,
    val equipment: String?, val requiredEquipment: List<String>, val movementPattern: String,
    val primaryMuscles: List<String>, val secondaryMuscles: List<String>, val minimumExperience: String,
    val isUnilateral: Boolean, val isBodyweight: Boolean, val selectionPriority: Int
)
data class ExercisePage(val items: List<ExerciseListItem>, val page: Int, val pageSize: Int, val totalCount: Int, val totalPages: Int)

/** Only the fields the app shows; the server may send more. */
data class ExerciseDetail(
    val id: String, val nameEs: String, val nameEn: String, val descriptionEs: String, val instructionsEs: List<String>,
    val tipsEs: List<String>, val difficulty: String, val equipment: String?, val requiredEquipment: List<String>,
    val bodyPart: String?, val movementPattern: String, val primaryMuscles: List<String>, val secondaryMuscles: List<String>, val minimumExperience: String?
)

// Subscription.

data class SubscriptionStatus(
    val hasAccess: Boolean, val enforced: Boolean = true, val status: String = "none", val productId: String? = null,
    val startedAt: String? = null, val expiresAt: String? = null, val autoRenewing: Boolean = false, val source: String? = null
) {
    /** The plans screen is shown only when the backend demands payment and the user has no access. */
    val paywallRequired: Boolean get() = enforced && !hasAccess
}
data class VerifySubscriptionRequest(val productId: String, val purchaseToken: String)
