package com.trazo.android.data.model

/** A comment for the team. [clientRequestId] is the same on every retry, so the server stores it once. */
data class FeedbackRequest(
    val category: String,
    val message: String,
    val screen: String?,
    val device: String?,
    val clientRequestId: String
)
