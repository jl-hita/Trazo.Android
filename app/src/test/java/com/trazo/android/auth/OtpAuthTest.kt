package com.trazo.android.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Only a real TOTP setup link may be handed to another app. */
class OtpAuthTest {
    @Test fun `a totp setup link is accepted`() {
        assertTrue(isOtpAuthUri("otpauth://totp/TRAZO:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=TRAZO"))
        assertTrue(isOtpAuthUri("OTPAUTH://totp/TRAZO:user?secret=ABC"))
    }

    @Test fun `anything else is refused`() {
        assertFalse(isOtpAuthUri(null))
        assertFalse(isOtpAuthUri(""))
        assertFalse(isOtpAuthUri("https://evil.example/otpauth://totp/x"))
        assertFalse(isOtpAuthUri("otpauth://hotp/TRAZO:user?secret=ABC")) // only time-based codes
        assertFalse(isOtpAuthUri("intent://scan#Intent;scheme=zxing;end"))
        assertFalse(isOtpAuthUri("otpauth://totp/TRAZO user?secret=ABC")) // whitespace
        assertFalse(isOtpAuthUri("otpauth://totp/" + "a".repeat(600)))
    }
}
