package com.trazo.android.auth

/**
 * The setup link the API returns is handed to another app, so only a well-formed `otpauth://totp/...` link is ever opened:
 * anything else (another scheme, spaces, an absurd length) is treated as unusable and the user falls back to copying the key.
 */
fun isOtpAuthUri(uri: String?): Boolean =
    uri != null && uri.length <= MAX_OTPAUTH_LENGTH && uri.startsWith("otpauth://totp/", ignoreCase = true) && uri.none { it.isWhitespace() }

private const val MAX_OTPAUTH_LENGTH = 512
