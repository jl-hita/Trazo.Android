package com.trazo.android.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

sealed interface GoogleResult {
    data class Token(val idToken: String) : GoogleResult
    data object Cancelled : GoogleResult
    data object NoAccount : GoogleResult
    data object UnexpectedCredential : GoogleResult
    /** [detail] is only filled in debug builds. */
    data class Unavailable(val detail: String?) : GoogleResult
}

/** Asks the user to pick a Google account and returns its ID token (used to sign in and to link the account). */
suspend fun requestGoogleIdToken(context: Context, clientId: String, debug: Boolean): GoogleResult = try {
    val request = GetCredentialRequest.Builder().addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build()).build()
    val credential = CredentialManager.create(context).getCredential(context, request).credential
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
        GoogleResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
    else GoogleResult.UnexpectedCredential
} catch (_: GetCredentialCancellationException) {
    GoogleResult.Cancelled
} catch (_: NoCredentialException) {
    GoogleResult.NoAccount
} catch (e: GetCredentialException) {
    GoogleResult.Unavailable(if (debug) "${e.type}: ${e.message}" else null)
}
