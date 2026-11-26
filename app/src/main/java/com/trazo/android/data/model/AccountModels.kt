package com.trazo.android.data.model

/** What `GET /api/v1/account` says about deleting the account: whether it can be done and what the person must prove. */
data class AccountInfo(val canDelete: Boolean, val cannotDeleteReason: String? = null, val deletion: DeletionRequirements)

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
