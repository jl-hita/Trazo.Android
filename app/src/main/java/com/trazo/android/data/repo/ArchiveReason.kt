package com.trazo.android.data.repo

/** Why a plan is archived. [code] is what the server stores and exports: never change one. The order is the order they are offered in. */
enum class ArchiveReason(val code: String) {
    PlanCompleted("PlanCompleted"),
    WantNewPlan("WantNewPlan"),
    NoTime("NoTime"),
    TooDemanding("TooDemanding"),
    TooEasy("TooEasy"),
    /** Health data: only offered with the health data consent. */
    Discomfort("Discomfort"),
    GoalsChanged("GoalsChanged"),
    NotConvinced("NotConvinced"),
    Other("Other")
}

/** The question to ask before archiving, with the answers this person can be offered. */
data class ArchiveQuestion(val reasons: List<ArchiveReason>)
