package com.trazo.android.data.model

/** What `GET /api/v1/account` says about deleting the account: whether it can be done and what the person must prove. */
data class AccountInfo(
    val canDelete: Boolean, val cannotDeleteReason: String? = null, val deletion: DeletionRequirements,
    /** Whether the explicit consent for health data is in force. */
    val healthConsent: Boolean = false,
    /** Whether the person lets their training data be used to study and improve the plans. */
    val researchConsent: Boolean = false
)

data class HealthConsentRequest(val consent: Boolean)

data class ResearchConsentRequest(val consent: Boolean)

/**
 * What goes with archiving a plan: [planId] is the plan the person was looking at and [reason] a code of
 * [com.trazo.android.data.repo.ArchiveReason]. Both are left out when unknown or not answered.
 */
data class ArchivePlanRequest(val reason: String? = null, val planId: String? = null)

/** The password and the Google token are alternatives; the two-factor code goes on top when the account has it. */
data class DeletionRequirements(
    val requiresPassword: Boolean = false,
    val requiresGoogleIdToken: Boolean = false,
    val requiresTwoFactorCode: Boolean = false,
    /** What the person must know first, such as a Google Play subscription that goes on charging. */
    val warning: String? = null,
    /** Where to cancel that subscription; null when there is none. */
    val manageSubscriptionUrl: String? = null
)

/** The proof of identity an irreversible deletion asks for. */
data class DeleteAccountRequest(val password: String? = null, val idToken: String? = null, val code: String? = null)

data class DeleteAccountResult(val deleted: Boolean, val playSubscriptionNotCancelled: Boolean = false, val manageSubscriptionUrl: String? = null)
