package com.trazo.android.data.model

// Contracts of the TRAZO API (docs/planning-engine.md, docs/subscriptions.md, docs/exercise-catalog-api.md).
// Dates travel as ISO strings (yyyy-MM-dd) and enums by name, exactly as the server sends them.

/** The dates exist only in calendar plans; [startPeriod]/[endPeriod] (weeks, or cycles in a sequence plan) always do. */
data class PhaseView(
    val sequence: Int, val name: String, val type: String, val startDate: String?, val endDate: String?, val objective: String,
    val startPeriod: Int = 0, val endPeriod: Int = 0
)
data class MacrocycleView(val name: String, val notes: String?, val phases: List<PhaseView>)
data class MesocycleView(val sequence: Int, val phaseType: String, val isDeload: Boolean, val startWeek: Int, val weeks: Int, val weekInBlock: Int)

data class PlanView(
    val id: String, val status: String, val timeline: String, val currentWeek: Int, val totalWeeks: Int,
    val startDate: String, val endDate: String?, val goal: String, val experience: String, val split: String,
    val trainingDays: List<String>, val maxSessionMinutes: Int, val availableEquipment: List<String>,
    val excludedExerciseIds: List<String>, val limitations: List<String>, val currentPhase: Int?,
    val macrocycle: MacrocycleView, val disclaimer: String, val origin: String? = null,
    /** "Calendar" (weeks and days) or "Sequence" (the program follows the order of its sessions; no days or dates). */
    val schedulingMode: String = "Calendar", val sessionsPerPeriod: Int = 0,
    /** Muscle to "Emphasize" or "Maintain"; a muscle that is absent is "Grow". Null when an older server does not send it (read it with [priorities]). */
    val musclePriorities: Map<String, String>? = null
) {
    val priorities: Map<String, String> get() = musclePriorities.orEmpty()
}

data class ExerciseView(
    val exerciseId: String, val name: String, val order: Int, val sets: Int, val minRepetitions: Int, val maxRepetitions: Int,
    val targetRir: Double?, val restSeconds: Int, val progressionRule: String, val targetLoadKg: Double?, val loadBasis: String?,
    val warmupSets: Int?, val warmupNote: String?, val primaryMuscles: List<String>, val secondaryMuscles: List<String>,
    val swappedFromExerciseId: String?,
    /** What the user did the last time (any plan), with a proposed load; null when never logged. */
    val lastPerformance: LastPerformanceView? = null,
    /** Letter of the superset this exercise belongs to (consecutive exercises with the same letter), or null. */
    val supersetGroup: String? = null,
    /** True when the user added it to this session on top of the program (it is not repeated in other sessions). */
    val isExtra: Boolean = false
)

data class LastSetView(val setNumber: Int, val weightKg: Double?, val repetitions: Int?, val rir: Double?)

/**
 * [suggestionBasis] is "repeat" (same load), "increase" (every set reached the top of the range) or "estimate" (Premium: estimated from
 * the best of the last [basedOnSessions] sessions of the exercise).
 */
data class LastPerformanceView(
    val date: String, val sets: List<LastSetView>, val suggestedLoadKg: Double?, val suggestionBasis: String?, val basedOnSessions: Int? = null
)

data class ExerciseRefView(val exerciseId: String, val name: String)

/**
 * One thing the engine did (or decided not to do) after a session. [kind] is reduction_sets, raise_reserve, restore_sets, deload, hold_progression,
 * muscle_add_set, muscle_reduce_set, load_update, reentry, reschedule, skip or review; [changed] is false for a review that left the plan as it was.
 */
data class AdaptationEntryView(
    val id: String, val at: String, val kind: String, val summary: String, val explanation: String, val exercises: List<ExerciseRefView> = emptyList(),
    val sessionsChanged: Int = 0, val changed: Boolean = false, val sourceSession: String? = null, val sourceDate: String? = null
)

/** What the engine adjusted in the plan, newest first. [automatic] is false when the account has no automatic adaptation. */
data class AdaptationHistoryView(
    val reviews: Int = 0, val changes: Int = 0, val sessionsAdjusted: Int = 0, val automatic: Boolean = false,
    val entries: List<AdaptationEntryView> = emptyList(),
    /** "block" (the block in progress, the default) or "plan" (every block of the active plan). */
    val scope: String = "block", val planChanges: Int = 0, val hasEarlier: Boolean = false
)

data class LoggedSetView(
    val exerciseId: String, val setNumber: Int, val weightKg: Double?, val repetitions: Int?, val rir: Double?,
    val completed: Boolean, val skippedReason: String?, val pain: Boolean
)

data class LogView(
    val id: String, val loggedAt: String, val outcome: String, val nonCompletionReason: String, val recovery: String,
    val sessionRpe: Double?, val painReported: Boolean, val notes: String?, val sets: List<LoggedSetView>,
    /** When the workout really happened (the time it was registered is [loggedAt]). */
    val performedAt: String? = null, val performedOn: String? = null
)

data class SessionView(
    val id: String, val weekNumber: Int, val date: String?, val day: String?, val title: String,
    val estimatedDurationMinutes: Int, val status: String, val exercises: List<ExerciseView>, val log: LogView?,
    /** Place of the session in the program: its identity and order, whatever day it is done. */
    val sequenceIndex: Int = 0
)

data class WeekView(
    val planId: String, val weekNumber: Int, val weekStart: String?, val weekEnd: String?, val timeline: String,
    val mesocycle: MesocycleView?, val sessions: List<SessionView>,
    /** A generated plan without Premium: it can be read, not used. */
    val locked: Boolean = false
)

data class AppliedAdaptationView(val kind: String, val exerciseIds: List<String>, val sessionsChanged: Int, val explanation: String)
data class ExerciseAdviceView(val exerciseId: String, val action: String, val explanation: String, val nextLoadKg: Double?, val suggestedRepetitions: List<Int>?)
data class AdaptationReport(
    val exercises: List<ExerciseAdviceView>, val sessionAction: String, val sessionExplanation: String?,
    val applied: List<AppliedAdaptationView>, val notes: List<String>,
    /** The session was saved but not adapted: automatic adaptation is a Premium feature. */
    val locked: Boolean = false
)
/** [replayed] is true when the session had already been logged with this request: the saved result is returned again. */
data class LogResult(val session: SessionView, val adaptation: AdaptationReport, val replayed: Boolean = false)

data class AlternativeView(
    val exerciseId: String, val name: String, val movementPattern: String, val primaryMuscles: List<String>,
    val requiredEquipment: List<String>, val matchLevel: String
)
data class AlternativesResult(val exerciseId: String, val alternatives: List<AlternativeView>, val message: String?)

data class HistoryItem(
    val sessionId: String, val weekNumber: Int, val date: String?, val title: String, val outcome: String,
    val loggedAt: String, val completedSets: Int, val plannedSets: Int, val performedAt: String? = null
)

/** Volume (sets) of one plan week. [plannedSets] is null while its block is not generated; [completedSets] is null for weeks not reached. */
data class WeekLoadView(
    val week: Int, val weekStart: String?, val phaseType: String?, val isDeload: Boolean, val plannedSets: Int?,
    val completedSets: Int?, val plannedSessions: Int, val loggedSessions: Int
)
data class PlanLoadView(val planId: String, val totalWeeks: Int, val currentWeek: Int, val unit: String, val weeks: List<WeekLoadView>,
    /** "Week" for a calendar plan, "Rotation" for a sequence plan, whose periods are cycles of sessions and not seven days. */
    val periodKind: String? = null
)

data class PlanOptions(
    val goals: List<String>, val experience: List<String>, val splits: List<String>, val days: List<String>,
    val limitations: List<String>, val equipment: List<String>, val minimumTrainingDays: Int, val maximumTrainingDays: Int,
    val minimumSessionMinutes: Int, val maximumSessionMinutes: Int, val minimumTotalWeeks: Int, val maximumTotalWeeks: Int,
    val defaultTotalWeeks: Int,
    val schedulingModes: List<String> = listOf("Calendar"), val minimumSessionsPerPeriod: Int = 2, val maximumSessionsPerPeriod: Int = 6,
    /** Null on a server that does not offer the preparation: the wizard then does not show the choice. */
    val preparation: PreparationOptions? = null
)

/** The starting points a plan can declare and the weeks of preparation each one suggests; the user may change them within the limits. */
data class PreparationOptions(
    val readiness: List<String>, val minimumWeeks: Int, val maximumWeeks: Int, val newWeeks: Int, val returningShortBreakWeeks: Int,
    val returningLongBreakWeeks: Int, val longBreakMonths: Int, val minimumWeeksAfter: Int
)

// Requests.

/** A plan the user writes: their weekly routines, repeated every week. Free; Premium adapts it as they train. */
data class ManualPlanRequest(
    val name: String, val totalWeeks: Int, val days: List<ManualDayRequest>, val notes: String? = null,
    val startDate: String? = null, val goal: String? = null, val experience: String? = null,
    /** "Sequence": the routines are a rotation in the order given and have no weekday. */
    val schedulingMode: String? = null
)
/** [day] is null in a sequence plan (the routines rotate; there are no weekdays). */
data class ManualDayRequest(val day: String?, val title: String, val exercises: List<ManualExerciseRequest>)
data class ManualExerciseRequest(
    val exerciseId: String, val sets: Int, val minRepetitions: Int, val maxRepetitions: Int, val targetRir: Double? = null,
    val restSeconds: Int? = null, val supersetGroup: String? = null
)

data class PlanProfileRequest(
    val goal: String, val experience: String, val startDate: String? = null, val totalWeeks: Int? = null,
    val trainingDays: List<String>? = null, val maxSessionMinutes: Int? = null, val availableEquipment: List<String> = emptyList(),
    val excludedExerciseIds: List<String> = emptyList(), val limitations: List<String> = emptyList(),
    val split: String = "Automatic", val includeDeloadPhases: Boolean = true, val deloadEveryTrainingWeeks: Int? = null,
    /** "Sequence" plans have no fixed days: send [sessionsPerPeriod] instead of [trainingDays]. */
    val schedulingMode: String? = null, val sessionsPerPeriod: Int? = null,
    /** Hypertrophy only. Muscle to "Emphasize" / "Maintain"; leave a muscle out for the default dose. Null when nothing is chosen. */
    val musclePriorities: Map<String, String>? = null,
    /** "New" or "Returning" start the plan with a few weeks of preparation, counted inside [totalWeeks]; null for a regular plan. */
    val readiness: String? = null, val preparationWeeks: Int? = null
)

/** Replaces every priority of the current plan: an empty map clears them. */
data class UpdateMusclePrioritiesRequest(val musclePriorities: Map<String, String>)

data class LogSetRequest(
    val exerciseId: String, val setNumber: Int, val weightKg: Double?, val repetitions: Int?, val rir: Double?,
    val completed: Boolean, val skippedReason: String? = null, val painReported: Boolean = false
)

data class LogSessionRequest(
    val outcome: String, val nonCompletionReason: String = "None", val recovery: String = "Unknown", val sessionRpe: Double? = null,
    val painReported: Boolean = false, val notes: String? = null, val sets: List<LogSetRequest>,
    /** The post-exercise questions by exercise id; only answered ones are sent. */
    val exerciseFeedback: Map<String, ExerciseFeedbackRequest>? = null,
    /** When the workout happened (ISO instant). Fixed once, when the workout ends, so a retry or a queued send never changes it. */
    val performedAt: String? = null,
    /** Identifies this request: sending it again (lost answer, queue, second device) returns the saved result. */
    val clientRequestId: String? = null
)

/** stimulus: low|adequate|high, discomfort: none|mild|strong, preFatigue: recovered|loaded. Each may be null (not answered). */
data class ExerciseFeedbackRequest(val stimulus: String? = null, val discomfort: String? = null, val preFatigue: String? = null) {
    val isEmpty: Boolean get() = stimulus == null && discomfort == null && preFatigue == null
}

/** One message of the manual-review conversation. [author] is "user" or "team" (the user never sees who on the team). */
data class ReviewMessage(val id: String, val author: String, val body: String, val createdAt: String)

/**
 * A request for a manual review of the plan and its conversation. [status] is "pending", "in_progress" or "reviewed";
 * [unread] counts the team messages not read yet; [messages] is only present when the conversation was asked for.
 */
data class ReviewRequestView(
    val id: String, val status: String, val createdAt: String, val updatedAt: String? = null, val reviewedAt: String? = null,
    val unread: Int = 0, val messages: List<ReviewMessage>? = null
)

data class ReviewRequestBody(val note: String?)

data class ReviewMessageBody(val body: String)

/** Adds an exercise to one session only; the omitted fields take the server's moderate defaults. */
data class AddExerciseRequest(
    val exerciseId: String, val sets: Int? = null, val minRepetitions: Int? = null, val maxRepetitions: Int? = null,
    val targetRir: Double? = null, val restSeconds: Int? = null
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
    val id: String, val nameEs: String, val nameEn: String?, val descriptionEs: String?, val instructionsEs: List<String>,
    val tipsEs: List<String>, val difficulty: String, val equipment: String?, val requiredEquipment: List<String>,
    val bodyPart: String?, val movementPattern: String, val primaryMuscles: List<String>, val secondaryMuscles: List<String>, val minimumExperience: String?,
    val localizedName: String? = null, val localizedNameLanguage: String? = null, val localizedDescription: String? = null,
    val localizedInstructions: List<String>? = null, val localizedTips: List<String>? = null
)

/** Text resolved by GET /api/v1/exercises/{id}/localized for the request language. */
data class LocalizedExerciseText(
    val requestedLanguage: String, val name: String, val description: String, val instructions: List<String>, val tips: List<String>,
    val nameLanguage: String, val descriptionLanguage: String, val instructionsLanguage: String, val tipsLanguage: String,
    val isCompleteForRequestedLanguage: Boolean, val missingFields: List<String>
)

data class ExerciseImageSlots(val slots: List<Int>, val attribution: String?)

// Subscription.

data class SubscriptionStatus(
    val hasAccess: Boolean, val enforced: Boolean = true, val status: String = "none", val productId: String? = null,
    val startedAt: String? = null, val expiresAt: String? = null, val autoRenewing: Boolean = false, val source: String? = null,
    /** End of the promotional Premium time (welcome period or referral rewards) that is active now; the server decides it. */
    val promoExpiresAt: String? = null
) {
    /** Premium can be offered: the backend charges for it and the account has no access yet. */
    val canUpgrade: Boolean get() = enforced && !hasAccess
}
data class VerifySubscriptionRequest(val productId: String, val purchaseToken: String)

/** Where in the app the Premium offer was opened from: account, plan, session or onboarding. */
data class PaywallViewRequest(val source: String)

// Current block (GET api/v1/plans/current/block): every week of the current mesocycle.
data class BlockWeekView(val weekNumber: Int, val weekStart: String?, val weekEnd: String?, val isCurrent: Boolean, val sessions: List<SessionView>)
data class BlockView(val planId: String, val mesocycle: MesocycleView?, val weeks: List<BlockWeekView>, val locked: Boolean = false)

// Progress (GET api/v1/plans/progress): everything the user logged, across plans.

data class ProgressSummary(
    val totalSessions: Int, val sessionsLast30Days: Int, val sessionsThisWeek: Int, val weeklyGoal: Int, val streakWeeks: Int,
    val totalSets: Int, val totalVolumeKg: Double
)
data class ExercisePoint(val date: String, val topWeightKg: Double, val repsAtTop: Int, val estimatedOneRepMaxKg: Double?, val volumeKg: Double, val sets: Int)
data class ExerciseProgress(
    val exerciseId: String, val name: String, val sessions: Int, val lastDate: String, val bestWeightKg: Double?,
    val bestEstimatedOneRepMaxKg: Double?, val recordDate: String?, val newRecord: Boolean, val points: List<ExercisePoint>
)
/** Effective sets per week of one muscle (primary 1 per set, secondary 0.5), aligned with [ProgressView.weekStarts]. */
data class MuscleVolume(val muscle: String, val setsByWeek: List<Double>)
data class ProgressView(val summary: ProgressSummary, val weekStarts: List<String>, val muscles: List<MuscleVolume>, val exercises: List<ExerciseProgress>)

// Next session (GET api/v1/plans/current/next): the backend decides which session comes next and whether the muscles are recovered.

/** A pending session whose planned date has passed (calendar plans only). It can still be registered or skipped. */
data class OverdueSessionView(val id: String, val sequenceIndex: Int, val title: String, val date: String)

/** A later pending session that is recovered now. */
data class RecoveryAlternativeView(val sessionId: String, val sequenceIndex: Int, val title: String)

/**
 * Recovery advice (Premium): [state] is "Ready", "Caution" or "Wait"; [availableAt] is the estimate (null when ready), [reasons] say
 * which muscles are still recovering and [alternatives] are later sessions that can be done now. Advice only: the user can always train.
 */
data class AvailabilityView(val state: String, val availableAt: String?, val reasons: List<String>, val alternatives: List<RecoveryAlternativeView>)

/** Where the session is in the program: its period (a week, or a cycle in a sequence plan), its place in it and its block. */
data class NextPositionView(val period: Int, val sessionInPeriod: Int, val sessionsInPeriod: Int, val mesocycle: MesocycleView?)

/** Shown after a long time without training: advice to ease back in. The plan does not move meanwhile. */
data class ReentryView(val daysSinceLastWorkout: Int, val message: String, val applied: Boolean = false)

/** Calendar plans: the pending program is [weeksBehind] whole weeks behind the calendar and can be moved to today. */
data class RescheduleOfferView(val weeksBehind: Int)

/** What moving the plan did: whole weeks moved, sessions whose date changed and the plan's new dates. */
data class RescheduleResult(val weeksShifted: Int, val sessionsMoved: Int, val startDate: String, val endDate: String)

data class NextSessionView(
    val planId: String, val schedulingMode: String, val timeline: String, val session: SessionView?, val position: NextPositionView?,
    val availability: AvailabilityView?, val availabilityLocked: Boolean = false, val overdue: List<OverdueSessionView> = emptyList(),
    val locked: Boolean = false, val reentry: ReentryView? = null, val reschedule: RescheduleOfferView? = null,
    val adjustment: AdjustmentView? = null
)

/**
 * What to do about the sessions not done, decided by the backend. [strategy] is Continue, Shift, EaseBack or Rebuild; [message] explains it in the
 * app language. [suggestSkip]: omitting the oldest late session is better than recovering it. [canSkip]: any pending session can be omitted.
 */
data class AdjustmentView(
    val strategy: String, val reason: String, val message: String, val easeBackSessions: Int = 0,
    val canReschedule: Boolean = false, val suggestSkip: Boolean = false, val canSkip: Boolean = true
)

/** Omits a pending session on purpose: [reason] is Time, Fatigue, Equipment, Motivation, Illness or Other. */
data class SkipSessionRequest(val reason: String, val notes: String? = null)

data class SkipResult(val session: SessionView, val adapted: Boolean = false, val message: String = "")
