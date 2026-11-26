package com.trazo.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The real app on a device, signed out: it starts, the sign-in form behaves and passes the accessibility checks. No server needed. */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule val app = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = app.activity.getString(id)
    private fun button(id: Int) = app.onNode(hasText(text(id)) and hasClickAction())
    private fun field(id: Int) = app.onNode(hasText(text(id)) and hasSetTextAction())

    @Before fun waitForTheForm() {
        app.waitUntil(10_000) { app.onAllNodes(hasText(text(R.string.login_email))).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun signingInNeedsEmailAndPassword() {
        button(R.string.login_sign_in).performScrollTo().assertIsNotEnabled()
        field(R.string.login_email).performScrollTo().performTextInput("someone@example.com")
        field(R.string.login_password).performScrollTo().performTextInput("not-a-real-password")
        button(R.string.login_sign_in).performScrollTo().assertIsEnabled()
    }

    @Test fun theRegistrationFormCanBeOpened() {
        button(R.string.login_create_account).performScrollTo().performClick()
        field(R.string.login_name).performScrollTo().assertIsDisplayed()
        button(R.string.login_have_account).performScrollTo().assertIsDisplayed()
    }

    /** Missing labels, small touch targets and low contrast fail the test. */
    @SdkSuppress(minSdkVersion = 34)
    @Test fun theFormsPassTheAccessibilityChecks() {
        app.enableAccessibilityChecks()
        app.onRoot().tryPerformAccessibilityChecks()
        button(R.string.login_create_account).performScrollTo().performClick()
        app.onRoot().tryPerformAccessibilityChecks()
    }
}
