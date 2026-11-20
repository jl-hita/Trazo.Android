package com.trazo.android.ui.onboarding

import com.trazo.android.data.model.PreparationOptions

/**
 * The starting point the wizard asks for (trains regularly / new to the gym / coming back after a break) and the weeks of preparation it
 * suggests. The figures come from the server; none of them is a guideline, they are product decisions the user can change.
 */
object PreparationChoice {
    const val REGULAR = "Regular"
    const val NEW = "New"
    const val RETURNING = "Returning"

    /** Weeks suggested for a starting point; zero for a regular plan. */
    fun suggestedWeeks(readiness: String, longBreak: Boolean, options: PreparationOptions): Int = when (readiness) {
        NEW -> options.newWeeks
        RETURNING -> if (longBreak) options.returningLongBreakWeeks else options.returningShortBreakWeeks
        else -> 0
    }.coerceIn(0, options.maximumWeeks)

    /** The plan must keep its own weeks after the preparation, so a longer preparation asks for a longer plan. */
    fun minimumTotalWeeks(planMinimum: Int, preparationWeeks: Int, options: PreparationOptions): Int =
        if (preparationWeeks <= 0) planMinimum else maxOf(planMinimum, preparationWeeks + options.minimumWeeksAfter)

    /** Weeks left for the plan itself once the preparation is done. */
    fun weeksAfter(totalWeeks: Int, preparationWeeks: Int): Int = totalWeeks - preparationWeeks
}
